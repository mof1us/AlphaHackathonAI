package ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld

import io.temporal.activity.ActivityOptions
import ru.alfahack.elephants.backend.integrations.temporal.context.newSuspendActivityStub
import java.time.Duration

/**
 * Реализация [HelloWorldWorkflow].
 *
 * Класс открыт и без аргументов конструктора: Temporal создаёт его сам.
 * Доменный код и логи сюда не ставятся: их выполняет activity.
 */
open class HelloWorldWorkflowImpl : HelloWorldWorkflow {
    override suspend fun greet(input: HelloWorldWorkflowInput?): HelloWorldWorkflowOutput {
        val helloWorldActivity = newSuspendActivityStub(
            HelloWorldActivity::class.java,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE)
                .build(),
        )
        return HelloWorldWorkflowOutput(helloWorldActivity.greet(input?.name))
    }

    private companion object {
        private val ACTIVITY_START_TO_CLOSE: Duration = Duration.ofSeconds(10)
    }
}
