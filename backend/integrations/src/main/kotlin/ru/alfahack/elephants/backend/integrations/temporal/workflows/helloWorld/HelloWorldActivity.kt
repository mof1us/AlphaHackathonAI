package ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

/**
 * Нагрузка проверочного workflow.
 *
 * Сетевые и вычислительные действия живут здесь, а не в [HelloWorldWorkflow]:
 * workflow только планирует вызов и ждёт результат.
 */
@ActivityInterface
interface HelloWorldActivity {
    /** Возвращает приветствие для переданного имени. */
    @ActivityMethod
    suspend fun greet(name: String?): String
}
