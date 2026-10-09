package ru.alfahack.elephants.backend.integrations.temporal.schedule

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules.ScheduleSpec
import io.temporal.workflow.WorkflowInterface
import ru.alfahack.elephants.backend.integrations.temporal.TemporalTaskQueue

/**
 * Базовый класс расписания Temporal.
 *
 * Его расширяет класс реализации workflow. Приложение `temporal-runner` стартует
 * тот `@WorkflowInterface`, который реализует этот наследник. Методы расписания
 * читает Spring-бин; сам workflow Temporal создаёт отдельным экземпляром,
 * поэтому конструктор не должен вызывать API workflow.
 */
abstract class WorkflowScheduler {
    /** Идентификатор расписания в namespace. */
    abstract fun scheduleName(): String

    /** Когда запускать workflow: cron, интервал или календарь. */
    abstract fun scheduleSpec(): ScheduleSpec

    /**
     * Политика overflow: что делать с новым запуском, если предыдущий ещё идёт.
     */
    open fun overlapPolicy(): ScheduleOverlapPolicy = ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_SKIP

    /** Очередь, в которой worker обслуживает этот workflow. */
    abstract fun taskQueue(): TemporalTaskQueue

    /** Аргументы запуска. Пустой массив стартует workflow без входных данных. */
    open fun arguments(): Array<out Any?> = emptyArray()

    /** Интерфейс workflow, который реализует класс-наследник. */
    fun workflowInterface(): Class<*> {
        val found = mutableListOf<Class<*>>()
        val pending = ArrayDeque<Class<*>>()
        pending.add(javaClass)
        val seen = mutableSetOf<Class<*>>()
        while (pending.isNotEmpty()) {
            val type = pending.removeFirst()
            if (!seen.add(type)) {
                continue
            }
            if (type.isInterface && type.isAnnotationPresent(WorkflowInterface::class.java)) {
                found.add(type)
            }
            type.interfaces.forEach(pending::add)
            type.superclass?.takeUnless { it == Any::class.java }?.let(pending::add)
        }
        require(found.size == 1) {
            "${javaClass.name} must implement exactly one @WorkflowInterface, found $found"
        }
        return found.single()
    }
}
