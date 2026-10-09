package ru.alfahack.elephants.backend.temporalrunner.schedule

import io.mockk.*
import io.temporal.client.schedules.*
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.integrations.temporal.TemporalTaskQueue
import ru.alfahack.elephants.backend.integrations.temporal.schedule.WorkflowScheduler
import io.temporal.workflow.Functions
import kotlin.test.*

class TemporalScheduleBootstrapTest {
    @Test
    fun `creates declared schedule with workflow type arguments and queue`() {
        val client = mockk<ScheduleClient>()
        val created = slot<Schedule>()
        every { client.createSchedule("probe", capture(created), any()) } returns mockk()
        val bootstrap = TemporalScheduleBootstrap(client, listOf(ScheduledProbe()))
        bootstrap.start()
        bootstrap.start()
        assertTrue(bootstrap.isRunning)
        val action = created.captured.action as ScheduleActionStartWorkflow
        assertEquals("Probe", action.workflowType)
        assertEquals("probe", action.options.workflowId)
        assertEquals(TemporalTaskQueue.HELLO_WORLD_QUEUE.name, action.options.taskQueue)
        assertEquals(listOf("0 * * * *"), created.captured.spec.cronExpressions)
        verify(exactly = 1) { client.createSchedule(any(), any(), any()) }
        verify(exactly = 0) { client.listSchedules() }
    }

    @Test
    fun `updates existing declared schedule while retaining pause state`() {
        val client = mockk<ScheduleClient>()
        val handle = mockk<ScheduleHandle>()
        val declared = slot<Schedule>()
        every { client.createSchedule("probe", capture(declared), any()) } throws ScheduleAlreadyRunningException(null)
        every { client.getHandle("probe") } returns handle
        val updater = slot<Functions.Func1<ScheduleUpdateInput, ScheduleUpdate>>()
        every { handle.update(capture(updater)) } just Runs
        TemporalScheduleBootstrap(client, listOf(ScheduledProbe())).reconcile()
        val description = mockk<ScheduleDescription>()
        val old = Schedule.newBuilder(declared.captured)
            .setState(ScheduleState.newBuilder().setPaused(true).setNote("paused manually").build())
            .build()
        every { description.schedule } returns old
        val updated = updater.captured.apply(ScheduleUpdateInput(description)).schedule
        assertTrue(requireNotNull(updated.state).isPaused)
        assertEquals("paused manually", updated.state?.note)
        assertEquals(listOf("0 * * * *"), updated.spec.cronExpressions)
        verify(exactly = 0) { handle.delete() }
        verify(exactly = 0) { client.listSchedules() }
    }

    @Test
    fun `no schedules does not change namespace`() {
        val client = mockk<ScheduleClient>()
        TemporalScheduleBootstrap(client, emptyList()).reconcile()
        verify { client wasNot Called }
    }

    @Test
    fun `validates duplicate names before contacting Temporal`() {
        val client = mockk<ScheduleClient>()
        assertFailsWith<IllegalArgumentException> {
            TemporalScheduleBootstrap(client, listOf(ScheduledProbe(), ScheduledProbe())).reconcile()
        }
        verify { client wasNot Called }
    }
}

@WorkflowInterface
interface Probe {
    @WorkflowMethod fun run()
}

class ScheduledProbe : WorkflowScheduler(), Probe {
    override fun scheduleName() = "probe"
    override fun scheduleSpec(): ScheduleSpec = ScheduleSpec.newBuilder()
        .setCronExpressions(listOf("0 * * * *")).build()
    override fun taskQueue() = TemporalTaskQueue.HELLO_WORLD_QUEUE
    override fun run() = Unit
}
