package ru.alfahack.elephants.backend.temporalrunner.worker

import io.temporal.worker.WorkerFactory
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.alfahack.elephants.backend.integrations.temporal.TemporalTaskQueue
import ru.alfahack.elephants.backend.integrations.temporal.TemporalWorkersFactory

/**
 * Подключает очереди и workflow, затем начинает опрос Temporal.
 *
 * Живёт только в приложении `temporal-runner`: API-процесс этот класс не
 * видит и очереди не опрашивает. Бины клиента создаются без сети. Соединение
 * появляется здесь, после [TemporalWorkersFactory.connectQueues] и
 * [TemporalWorkersFactory.registerWorkflows].
 */
@Component
@ConditionalOnProperty(prefix = "integrations.temporal", name = ["workers-enabled"], havingValue = "true", matchIfMissing = true)
class TemporalWorkerLifecycle(
    private val workerFactory: WorkerFactory,
    private val workersFactory: TemporalWorkersFactory,
) : SmartLifecycle {
    @Volatile
    private var running = false

    override fun start() {
        if (running || workerFactory.isStarted) {
            return
        }
        val workers = workersFactory.connectQueues()
        workersFactory.registerWorkflows(workers)
        workerFactory.start()
        running = true
        logger.info(
            "Temporal workers started for queues: {}",
            TemporalTaskQueue.entries.joinToString { it.name },
        )
    }

    override fun stop() {
        workerFactory.shutdown()
        workerFactory.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)
        if (!workerFactory.isTerminated) {
            workerFactory.shutdownNow()
        }
        running = false
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Integer.MAX_VALUE - 1

    private companion object {
        private val logger = LoggerFactory.getLogger(TemporalWorkerLifecycle::class.java)
    }
}
