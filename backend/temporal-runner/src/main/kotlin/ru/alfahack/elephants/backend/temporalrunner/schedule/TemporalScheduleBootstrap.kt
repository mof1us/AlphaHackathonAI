package ru.alfahack.elephants.backend.temporalrunner.schedule

import io.temporal.client.WorkflowOptions
import io.temporal.client.schedules.Schedule
import io.temporal.client.schedules.ScheduleActionStartWorkflow
import io.temporal.client.schedules.ScheduleAlreadyRunningException
import io.temporal.client.schedules.ScheduleClient
import io.temporal.client.schedules.ScheduleOptions
import io.temporal.client.schedules.SchedulePolicy
import io.temporal.client.schedules.ScheduleUpdate
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import ru.alfahack.elephants.backend.integrations.temporal.schedule.WorkflowScheduler

/** Создаёт или обновляет объявленные расписания после старта workers. */
@Component
@ConditionalOnProperty(prefix = "integrations.temporal", name = ["workers-enabled"], havingValue = "true", matchIfMissing = true)
class TemporalScheduleBootstrap(
    private val scheduleClient: ScheduleClient,
    private val schedulers: List<WorkflowScheduler>,
) : SmartLifecycle {
    @Volatile private var running = false

    override fun start() {
        if (running) return
        reconcile()
        running = true
    }

    override fun stop() { running = false }
    override fun isRunning(): Boolean = running
    override fun getPhase(): Int = Integer.MAX_VALUE

    /** Сохраняет чужие расписания и состояние pause у уже существующих. */
    internal fun reconcile() {
        val names = schedulers.map { it.scheduleName() }
        require(names.all { it.isNotBlank() }) { "Temporal schedule name must not be blank" }
        require(names.distinct().size == names.size) { "Temporal schedule names must be unique" }
        // Проверяем все объявления до первой записи в Temporal.
        val schedules = schedulers.associate { scheduler ->
            scheduler.scheduleName() to Schedule.newBuilder()
                .setAction(
                    ScheduleActionStartWorkflow.newBuilder()
                        .setWorkflowType(scheduler.workflowInterface())
                        .setOptions(
                            WorkflowOptions.newBuilder()
                                .setWorkflowId(scheduler.scheduleName())
                                .setTaskQueue(scheduler.taskQueue().name)
                                .build(),
                        )
                        .setArguments(*scheduler.arguments())
                        .build(),
                )
                .setSpec(scheduler.scheduleSpec())
                .setPolicy(SchedulePolicy.newBuilder().setOverlap(scheduler.overlapPolicy()).build())
                .build()
        }
        schedules.forEach { (name, schedule) ->
            try {
                scheduleClient.createSchedule(name, schedule, ScheduleOptions.newBuilder().build())
            } catch (_: ScheduleAlreadyRunningException) {
                scheduleClient.getHandle(name).update { input ->
                    ScheduleUpdate(
                        Schedule.newBuilder(input.description.schedule)
                            .setAction(schedule.action)
                            .setSpec(schedule.spec)
                            .setPolicy(schedule.policy)
                            .build(),
                    )
                }
            }
            logger.info("Reconciled Temporal schedule {}", name)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(TemporalScheduleBootstrap::class.java)
    }
}
