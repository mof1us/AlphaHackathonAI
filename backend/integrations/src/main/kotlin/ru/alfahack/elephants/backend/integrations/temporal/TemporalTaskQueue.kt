package ru.alfahack.elephants.backend.integrations.temporal

/**
 * Очереди Temporal, которые опрашивает это приложение.
 *
 * Имя очереди — контракт с тем, кто стартует workflow. Новая очередь добавляется
 * сюда, и [TemporalWorkersFactory.connectQueues] поднимает для неё worker.
 */
enum class TemporalTaskQueue {
    /** Очередь проверочного workflow: по ней видно, что worker подключён. */
    HELLO_WORLD_QUEUE,
    ;
}
