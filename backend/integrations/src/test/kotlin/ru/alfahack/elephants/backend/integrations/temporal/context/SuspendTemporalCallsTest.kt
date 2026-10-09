package ru.alfahack.elephants.backend.integrations.temporal.context

import io.temporal.activity.ActivityExecutionContext
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase
import io.temporal.common.interceptors.Header
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class SuspendTemporalCallsTest {
    @Test
    fun `runs an activity once unless the workflow sets its own attempts`() {
        val timeout = Duration.ofSeconds(5)
        val defaults = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(timeout)
            .build()
            .withDefaultRetry()
        val custom = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(timeout)
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(4).build())
            .build()
            .withDefaultRetry()

        assertEquals(1, defaults.retryOptions?.maximumAttempts)
        assertEquals(4, custom.retryOptions?.maximumAttempts)
    }

    @Test
    fun `fills the only continuation slot of a parameterless suspend workflow`() {
        val seen = executeWorkflow(arrayOf<Any?>(null))

        val continuation = assertIs<Continuation<*>>(seen.single())
        assertEquals(EmptyCoroutineContext, continuation.context)
    }

    @Test
    fun `fills the trailing continuation slot and keeps business arguments`() {
        val seen = executeWorkflow(arrayOf<Any?>(null, "mail", null))

        assertNull(seen[0])
        assertEquals("mail", seen[1])
        assertIs<Continuation<*>>(seen[2])
    }

    @Test
    fun `leaves a present continuation and a real last argument untouched`() {
        val existing = object : Continuation<Any?> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Any?>) = Unit
        }

        assertSame(existing, executeWorkflow(arrayOf<Any?>(existing)).single())
        assertEquals("name", executeWorkflow(arrayOf<Any?>("name")).single())
        assertEquals(0, executeWorkflow(emptyArray()).size)
    }

    @Test
    fun `fills the only continuation slot of a parameterless suspend activity`() {
        val recording = RecordingActivity()
        val inbound = SuspendWorkerInterceptor().interceptActivity(recording)

        inbound.execute(
            ActivityInboundCallsInterceptor.ActivityInput(Header.empty(), arrayOf<Any?>(null)),
        )

        assertIs<Continuation<*>>(recording.arguments.single())
    }

    private fun executeWorkflow(arguments: Array<Any?>): Array<Any?> {
        val recording = RecordingWorkflow()
        val inbound = SuspendWorkerInterceptor().interceptWorkflow(recording)
        inbound.execute(
            WorkflowInboundCallsInterceptor.WorkflowInput(Header.empty(), arguments),
        )
        return recording.arguments
    }
}

private class RecordingWorkflow : WorkflowInboundCallsInterceptorBase(UnusedWorkflowInbound()) {
    lateinit var arguments: Array<Any?>

    override fun execute(
        input: WorkflowInboundCallsInterceptor.WorkflowInput,
    ): WorkflowInboundCallsInterceptor.WorkflowOutput {
        arguments = input.arguments
        return WorkflowInboundCallsInterceptor.WorkflowOutput(null)
    }
}

private class UnusedWorkflowInbound : WorkflowInboundCallsInterceptor {
    override fun init(outboundCalls: WorkflowOutboundCallsInterceptor) = Unit

    override fun execute(
        input: WorkflowInboundCallsInterceptor.WorkflowInput,
    ): WorkflowInboundCallsInterceptor.WorkflowOutput =
        WorkflowInboundCallsInterceptor.WorkflowOutput(null)

    override fun handleSignal(input: WorkflowInboundCallsInterceptor.SignalInput) = Unit

    override fun handleQuery(
        input: WorkflowInboundCallsInterceptor.QueryInput,
    ): WorkflowInboundCallsInterceptor.QueryOutput =
        WorkflowInboundCallsInterceptor.QueryOutput(null)

    override fun validateUpdate(input: WorkflowInboundCallsInterceptor.UpdateInput) = Unit

    override fun executeUpdate(
        input: WorkflowInboundCallsInterceptor.UpdateInput,
    ): WorkflowInboundCallsInterceptor.UpdateOutput =
        WorkflowInboundCallsInterceptor.UpdateOutput(null)

    override fun newWorkflowMethodThread(runnable: Runnable, name: String?): Any = runnable

    override fun newCallbackThread(runnable: Runnable, name: String?): Any = runnable
}

private class RecordingActivity : ActivityInboundCallsInterceptorBase(UnusedActivityInbound()) {
    lateinit var arguments: Array<Any?>

    override fun execute(
        input: ActivityInboundCallsInterceptor.ActivityInput,
    ): ActivityInboundCallsInterceptor.ActivityOutput {
        arguments = input.arguments
        return ActivityInboundCallsInterceptor.ActivityOutput(null)
    }
}

private class UnusedActivityInbound : ActivityInboundCallsInterceptor {
    override fun init(context: ActivityExecutionContext) = Unit

    override fun execute(
        input: ActivityInboundCallsInterceptor.ActivityInput,
    ): ActivityInboundCallsInterceptor.ActivityOutput =
        ActivityInboundCallsInterceptor.ActivityOutput(null)
}
