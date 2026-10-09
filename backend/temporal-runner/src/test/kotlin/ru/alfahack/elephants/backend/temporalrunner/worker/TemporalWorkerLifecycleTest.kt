package ru.alfahack.elephants.backend.temporalrunner.worker

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.integrations.temporal.TemporalTaskQueue
import ru.alfahack.elephants.backend.integrations.temporal.TemporalWorkersFactory
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemporalWorkerLifecycleTest {
    @Test
    fun `connects queues, registers workflows, and only then starts polling`() {
        val workerFactory = mockk<WorkerFactory>()
        val workersFactory = mockk<TemporalWorkersFactory>()
        val workers = mapOf(TemporalTaskQueue.HELLO_WORLD_QUEUE to mockk<Worker>())
        every { workerFactory.isStarted } returns false
        every { workerFactory.start() } returns Unit
        every { workersFactory.connectQueues() } returns workers
        every { workersFactory.registerWorkflows(workers) } returns Unit
        val lifecycle = TemporalWorkerLifecycle(workerFactory, workersFactory)

        lifecycle.start()

        assertTrue(lifecycle.isRunning)
        verifyOrder {
            workersFactory.connectQueues()
            workersFactory.registerWorkflows(workers)
            workerFactory.start()
        }
    }

    @Test
    fun `does not connect queues when polling has already started`() {
        val workerFactory = mockk<WorkerFactory>()
        val workersFactory = mockk<TemporalWorkersFactory>()
        every { workerFactory.isStarted } returns true
        val lifecycle = TemporalWorkerLifecycle(workerFactory, workersFactory)

        lifecycle.start()

        assertFalse(lifecycle.isRunning)
        verify(exactly = 0) { workersFactory.connectQueues() }
    }
}
