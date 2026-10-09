package ru.alfahack.elephants.backend.integrations.temporal.context

import io.temporal.activity.Activity
import ru.alfahack.elephants.backend.utils.context.UserContext
import io.temporal.activity.ActivityOptions
import io.temporal.activity.ManualActivityCompletionClient
import io.temporal.common.RetryOptions
import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowStub
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase
import io.temporal.common.interceptors.WorkerInterceptorBase
import io.temporal.common.interceptors.WorkflowClientInterceptorBase
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase
import io.temporal.common.metadata.POJOActivityInterfaceMetadata
import io.temporal.workflow.Workflow
import ru.alfahack.elephants.backend.integrations.temporal.TemporalWorkflowTypes
import io.temporal.common.metadata.POJOWorkflowInterfaceMetadata
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Даёт вызывать `suspend`-методы workflow и activity через Java SDK Temporal.
 *
 * Kotlin передаёт [Continuation] последним аргументом. SDK пытается записать его
 * в историю, и Jackson падает на его сигнатуре. Здесь этот аргумент снимается
 * на клиенте и на вызове activity из workflow, а на воркере вместо пустого
 * слота подставляется continuation, чтобы тело метода выполнилось.
 * У метода без обычных аргументов этот слот единственный.
 *
 * Если `suspend`-activity реально приостанавливается, JVM-метод возвращает
 * маркер `COROUTINE_SUSPENDED`. Его нельзя записывать в историю. Worker
 * завершает такую activity через [io.temporal.activity.ActivityExecutionContext.useLocalManualCompletion]:
 * это штатный способ Temporal закончить activity после возврата метода.
 * Workflow вызывает activity через [newSuspendActivityStub], чтобы тип
 * результата брался из аргумента [Continuation], а не из JVM-типа `Object`.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class SuspendWorkflowClientInterceptor(
    workflowInterfaces: List<Class<*>> = TemporalWorkflowTypes.interfaces,
) : WorkflowClientInterceptorBase() {
    private val resultTypes: Map<String, Type> = workflowInterfaces.associate { type ->
        val metadata = POJOWorkflowInterfaceMetadata.newInstance(type)
        metadata.workflowType.orElseThrow() to metadata.workflowMethod.orElseThrow().workflowMethod.suspendResultType()
    }
    override fun newUntypedWorkflowStub(
        workflowType: String,
        options: WorkflowOptions,
        next: WorkflowStub,
    ): WorkflowStub = SuspendWorkflowStub(resultTypes[workflowType], next)

    override fun newUntypedWorkflowStub(
        execution: WorkflowExecution,
        workflowType: java.util.Optional<String>,
        next: WorkflowStub,
    ): WorkflowStub = SuspendWorkflowStub(resultTypes[workflowType.orElse(null)], next)
}

/** Подставляет continuation при исполнении suspend-методов на воркере. */
class SuspendWorkerInterceptor : WorkerInterceptorBase() {
    override fun interceptWorkflow(
        next: WorkflowInboundCallsInterceptor,
    ): WorkflowInboundCallsInterceptor = SuspendWorkflowInbound(next)

    override fun interceptActivity(
        next: ActivityInboundCallsInterceptor,
    ): ActivityInboundCallsInterceptor = SuspendActivityInbound(next)
}

private class SuspendWorkflowStub(
    private val suspendResultType: Type?,
    private val next: WorkflowStub,
) : WorkflowStub by next {
    override fun start(vararg args: Any?): WorkflowExecution = next.start(*args.withoutContinuation())

    override fun <R> getResult(resultClass: Class<R>, resultType: Type): R {
        if (resultClass != Any::class.java || suspendResultType == null) {
            return next.getResult(resultClass, resultType)
        }
        @Suppress("UNCHECKED_CAST")
        return next.getResult(suspendResultType.rawClass() as Class<R>, suspendResultType)
    }
}

private class SuspendWorkflowInbound(
    next: WorkflowInboundCallsInterceptor,
) : WorkflowInboundCallsInterceptorBase(next) {
    override fun init(outboundCalls: WorkflowOutboundCallsInterceptor) {
        super.init(SuspendWorkflowOutbound(outboundCalls))
    }

    override fun execute(
        input: WorkflowInboundCallsInterceptor.WorkflowInput,
    ): WorkflowInboundCallsInterceptor.WorkflowOutput {
        input.arguments.injectContinuation()
        return super.execute(input)
    }
}

private class SuspendWorkflowOutbound(
    next: WorkflowOutboundCallsInterceptor,
) : WorkflowOutboundCallsInterceptorBase(next) {
    override fun <R> executeActivity(
        input: WorkflowOutboundCallsInterceptor.ActivityInput<R>,
    ): WorkflowOutboundCallsInterceptor.ActivityOutput<R> {
        val args = input.args.withoutContinuation()
        if (args.size == input.args.size) {
            return super.executeActivity(input)
        }
        return super.executeActivity(
            WorkflowOutboundCallsInterceptor.ActivityInput(
                input.activityName,
                input.resultClass,
                input.resultType,
                args,
                input.options,
                input.header,
            ),
        )
    }
}

private class SuspendActivityInbound(
    next: ActivityInboundCallsInterceptor,
) : ActivityInboundCallsInterceptorBase(next) {
    override fun execute(
        input: ActivityInboundCallsInterceptor.ActivityInput,
    ): ActivityInboundCallsInterceptor.ActivityOutput {
        if (input.arguments.isEmpty() || input.arguments.last() != null) {
            return super.execute(input)
        }
        val continuation = ManualCompletionContinuation()
        input.arguments[input.arguments.lastIndex] = continuation
        val output = super.execute(input)
        continuation.finishInline(output.result)
        return output
    }
}

/**
 * Стаб `suspend`-activity.
 *
 * Java SDK считает типом результата JVM-возврат `Object`. Реальный тип лежит
 * в последнем параметре, `Continuation<in R>`. Здесь он передаётся в Temporal,
 * поэтому в workflow приходит `R`, а не строка или карта.
 */
fun <T : Any> newSuspendActivityStub(
    activityInterface: Class<T>,
    options: ActivityOptions,
): T {
    val metadata = POJOActivityInterfaceMetadata.newInstance(activityInterface)
    val untyped = Workflow.newUntypedActivityStub(options.withDefaultRetry())
    val proxy = Proxy.newProxyInstance(
        activityInterface.classLoader,
        arrayOf(activityInterface),
    ) { proxy, method, args ->
        if (method.declaringClass == Any::class.java) {
            invokeObjectMethod(proxy, method, args)
        } else {
            val resultType = method.suspendResultType()
            @Suppress("UNCHECKED_CAST")
            val resultClass = resultType.rawClass() as Class<Any>
            untyped.execute(
                metadata.getMethodMetadata(method).activityTypeName,
                resultClass,
                resultType,
                *((args ?: emptyArray()).withoutContinuation()),
            )
        }
    }
    @Suppress("UNCHECKED_CAST")
    return proxy as T
}

/**
 * Одна попытка: первый запуск и ноль повторов.
 *
 * В Temporal `maximumAttempts = 0` означает бесконечные повторы, поэтому
 * отсутствие ретраев задаётся единицей.
 */
internal const val DEFAULT_ACTIVITY_MAXIMUM_ATTEMPTS = 1

internal fun ActivityOptions.withDefaultRetry(): ActivityOptions {
    if (retryOptions != null) {
        return this
    }
    return toBuilder()
        .setRetryOptions(
            RetryOptions.newBuilder()
                .setMaximumAttempts(DEFAULT_ACTIVITY_MAXIMUM_ATTEMPTS)
                .build(),
        )
        .build()
}

private class InlineContinuation : Continuation<Any?> {
    override val context: CoroutineContext by lazy {
        UserContext.current()?.propagated() ?: EmptyCoroutineContext
    }

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

/**
 * Забирает результат приостановленной activity.
 *
 * Kotlin читает [context] в начале `suspend`-метода, когда поток activity ещё
 * внутри Temporal и контекст выполнения установлен. Здесь берётся клиент
 * локального ручного завершения. После возврата метода контекст уже снят.
 */
private class ManualCompletionContinuation : Continuation<Any?> {
    private val lock = Any()
    private var client: ManualActivityCompletionClient? = null
    private var pending: Result<Any?>? = null
    private var finished = false

    override val context: CoroutineContext by lazy {
        // Контекст и manual completion захватываются до выхода из activity.
        val captured = captureActivityCoroutineContext()
        captureClient()
        captured
    }

    override fun resumeWith(result: Result<Any?>) {
        deliver(result)
    }

    fun finishInline(result: Any?) {
        if (!isCoroutineSuspended(result)) {
            val armed = synchronized(lock) { client != null }
            if (armed) {
                deliver(Result.success(result))
            }
            return
        }
        pending?.let(::deliver)
    }

    private fun captureClient() {
        if (synchronized(lock) { client != null }) {
            return
        }
        val manual = Activity.getExecutionContext().useLocalManualCompletion()
        val ready = synchronized(lock) {
            if (client == null) {
                client = manual
            }
            if (finished || pending == null) {
                null
            } else {
                finished = true
                pending.also { pending = null }
            }
        }
        ready?.fold(onSuccess = manual::complete, onFailure = manual::fail)
    }

    private fun deliver(result: Result<Any?>) {
        val manual = synchronized(lock) {
            if (finished) {
                return
            }
            val current = client
            if (current == null) {
                pending = result
                return
            }
            finished = true
            pending = null
            current
        }
        result.fold(onSuccess = manual::complete, onFailure = manual::fail)
    }
}

private fun Array<out Any?>.withoutContinuation(): Array<Any?> =
    if (lastOrNull() is Continuation<*>) dropLast(1).toTypedArray() else Array(size) { this[it] }

private fun Array<Any?>.injectContinuation() {
    if (isEmpty() || last() != null) return
    this[lastIndex] = InlineContinuation()
}

private fun isCoroutineSuspended(value: Any?): Boolean =
    value != null &&
        value.javaClass.name == "kotlin.coroutines.intrinsics.CoroutineSingletons" &&
        value.toString() == "COROUTINE_SUSPENDED"

internal fun Method.suspendResultType(): Type {
    val last = genericParameterTypes.lastOrNull()
    if (last is ParameterizedType && last.rawType.rawClass().let { Continuation::class.java.isAssignableFrom(it) }) {
        return last.actualTypeArguments.first().unwrapWildcard()
    }
    return genericReturnType
}

private fun Type.unwrapWildcard(): Type = when (this) {
    is WildcardType -> lowerBounds.firstOrNull() ?: upperBounds.first()
    else -> this
}

private fun Type.rawClass(): Class<*> = when (this) {
    is Class<*> -> this
    is ParameterizedType -> rawType.rawClass()
    is WildcardType -> unwrapWildcard().rawClass()
    else -> Any::class.java
}

private fun invokeObjectMethod(proxy: Any, method: Method, args: Array<Any?>?): Any? = when (method.name) {
    "toString" -> "SuspendActivityStub"
    "hashCode" -> System.identityHashCode(proxy)
    "equals" -> args?.singleOrNull() === proxy
    else -> throw UnsupportedOperationException(method.name)
}

/** Сохраняет контекст пользователя при возобновлении activity. */
internal fun captureActivityCoroutineContext(): CoroutineContext =
    UserContext.current()?.propagated() ?: EmptyCoroutineContext
