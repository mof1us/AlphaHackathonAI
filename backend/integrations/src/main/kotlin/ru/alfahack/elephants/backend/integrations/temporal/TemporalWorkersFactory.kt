package ru.alfahack.elephants.backend.integrations.temporal

import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import org.springframework.stereotype.Component
import ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld.HelloWorldActivity
import ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld.HelloWorldWorkflowImpl

/**
 * Собирает workers приложения.
 *
 * [connectQueues] открывает worker на каждую [TemporalTaskQueue].
 * [registerWorkflows] сажает реализации workflow и их activity на эти очереди.
 * Опрос сервера здесь не начинается: его запускает приложение `temporal-runner`
 * после обоих шагов.
 */
@Component
class TemporalWorkersFactory(
    private val workerFactory: WorkerFactory,
    private val helloWorldActivity: HelloWorldActivity,
) {
    /** Открывает по одному worker на каждую очередь, ещё не начиная опрос. */
    fun connectQueues(): Map<TemporalTaskQueue, Worker> =
        TemporalTaskQueue.entries.associateWith { queue ->
            workerFactory.newWorker(queue.name)
        }

    /**
     * Подключает все workflow к своим очередям.
     *
     * Activity приходит готовым Spring-бином: в него уже внедрены зависимости
     * домена. Иначе задача останется без исполнителя.
     */
    fun registerWorkflows(workers: Map<TemporalTaskQueue, Worker>) {
        workers.getValue(TemporalTaskQueue.HELLO_WORLD_QUEUE).apply {
            registerWorkflowImplementationTypes(HelloWorldWorkflowImpl::class.java)
            registerActivitiesImplementations(helloWorldActivity)
        }

    }
}
