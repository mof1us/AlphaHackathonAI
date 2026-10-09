package ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * Проверочный workflow очереди [ru.alfahack.elephants.backend.integrations.temporal.TemporalTaskQueue.HELLO_WORLD_QUEUE].
 *
 * Тип workflow для клиента — `HelloWorldWorkflow`. Нагрузка выполняется в
 * [HelloWorldActivity], а не в самом workflow.
 */
@WorkflowInterface
interface HelloWorldWorkflow {
    /** Возвращает приветствие для переданного имени. */
    @WorkflowMethod
    suspend fun greet(input: HelloWorldWorkflowInput?): HelloWorldWorkflowOutput
}
