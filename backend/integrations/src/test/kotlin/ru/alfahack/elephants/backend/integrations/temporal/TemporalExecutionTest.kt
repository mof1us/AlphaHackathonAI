package ru.alfahack.elephants.backend.integrations.temporal

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.activity.ActivityOptions
import io.temporal.client.WorkflowClientOptions
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowFailedException
import io.temporal.testing.TestEnvironmentOptions
import io.temporal.testing.TestWorkflowEnvironment
import io.temporal.worker.WorkerFactoryOptions
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.integrations.temporal.configuration.temporalDataConverter
import ru.alfahack.elephants.backend.integrations.temporal.context.*
import ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld.*
import ru.alfahack.elephants.backend.utils.context.UserContext
import java.time.Duration
import kotlin.test.*

class TemporalExecutionTest {
    @Test
    fun `registered worker executes hello world with a typed Kotlin result`() {
        environment().use { env ->
            val workers = TemporalWorkersFactory(env.workerFactory, HelloWorldActivityImpl())
            workers.registerWorkflows(workers.connectQueues())
            env.start()
            val workflow = env.workflowClient.newWorkflowStub(HelloWorldWorkflow::class.java, options())
            assertEquals(HelloWorldWorkflowOutput("Hello world! You are Elephants"),
                runBlocking { workflow.greet(HelloWorldWorkflowInput("Elephants")) })
        }
    }

    @Test
    fun `suspended activity restores context and returns a typed result including generic workflow output`() {
        environment().use { env ->
            val worker = env.newWorker(TemporalTaskQueue.HELLO_WORLD_QUEUE.name)
            worker.registerWorkflowImplementationTypes(ProbeWorkflowImpl::class.java)
            val activity = ProbeActivityImpl()
            worker.registerActivitiesImplementations(activity)
            env.start()
            val workflow = env.workflowClient.newWorkflowStub(ProbeWorkflow::class.java, options())
            val result = runBlocking(UserContext(userId = 7).propagated()) { workflow.run(false) }
            assertEquals(listOf(ProbeResult("ok")), result)
            assertEquals(1, activity.calls)
        }
    }

    @Test
    fun `failure after suspension completes activity as failure without default retry`() {
        environment().use { env ->
            val worker = env.newWorker(TemporalTaskQueue.HELLO_WORLD_QUEUE.name)
            worker.registerWorkflowImplementationTypes(ProbeWorkflowImpl::class.java)
            val activity = ProbeActivityImpl()
            worker.registerActivitiesImplementations(activity)
            env.start()
            val workflow = env.workflowClient.newWorkflowStub(ProbeWorkflow::class.java, options())
            assertFailsWith<WorkflowFailedException> {
                runBlocking(UserContext(userId = 7).propagated()) { workflow.run(true) }
            }
            assertEquals(1, activity.calls)
        }
    }

    private fun environment(): TestWorkflowEnvironment = TestWorkflowEnvironment.newInstance(
        TestEnvironmentOptions.newBuilder()
            .setWorkflowClientOptions(WorkflowClientOptions.newBuilder()
                .setDataConverter(temporalDataConverter())
                .setContextPropagators(listOf(TemporalUserContextPropagator(temporalDataConverter())))
                .setInterceptors(SuspendWorkflowClientInterceptor(
                    TemporalWorkflowTypes.interfaces + ProbeWorkflow::class.java))
                .build())
            .setWorkerFactoryOptions(WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(SuspendWorkerInterceptor()).build())
            .build(),
    )

    private fun options(): WorkflowOptions = WorkflowOptions.newBuilder()
        .setTaskQueue(TemporalTaskQueue.HELLO_WORLD_QUEUE.name)
        .setWorkflowRunTimeout(Duration.ofSeconds(15)).build()
}

data class ProbeResult(val value: String)

@WorkflowInterface
interface ProbeWorkflow {
    @WorkflowMethod(name = "TypedProbe")
    suspend fun run(fail: Boolean): List<ProbeResult>
}

class ProbeWorkflowImpl : ProbeWorkflow {
    override suspend fun run(fail: Boolean): List<ProbeResult> {
        val activity = newSuspendActivityStub(ProbeActivity::class.java,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build())
        return listOf(activity.load(fail))
    }
}

@ActivityInterface
interface ProbeActivity {
    @ActivityMethod suspend fun load(fail: Boolean): ProbeResult
}

class ProbeActivityImpl : ProbeActivity {
    @Volatile var calls = 0
    override suspend fun load(fail: Boolean): ProbeResult = withContext(Dispatchers.Default) {
        calls++
        assertEquals(7, UserContext.current()?.userId)
        withActivityContext {
            assertNull(UserContext.current()?.userId)
            delay(20)
            assertNull(UserContext.current()?.userId)
        }
        assertEquals(7, UserContext.current()?.userId)
        if (fail) error("activity failed")
        ProbeResult("ok")
    }
}
