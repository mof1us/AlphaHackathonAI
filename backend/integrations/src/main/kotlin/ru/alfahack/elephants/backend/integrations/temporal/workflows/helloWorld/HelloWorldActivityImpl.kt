package ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld

import org.springframework.stereotype.Component
import ru.alfahack.elephants.backend.integrations.temporal.context.withActivityContext

/** Исполнитель проверочного workflow; зависимости домена внедряются в activity. */
@Component
class HelloWorldActivityImpl : HelloWorldActivity {
    override suspend fun greet(name: String?): String = withActivityContext {
        "Hello world! You are $name"
    }
}
