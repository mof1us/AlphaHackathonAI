package ru.alfahack.elephants.backend.temporalrunner

import io.temporal.worker.WorkerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import ru.alfahack.elephants.backend.data.config.dsl.JooqScope
import kotlin.test.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["integrations.temporal.workers-enabled=false"])
class TemporalRunnerApplicationTests {
    @Autowired private lateinit var context: ApplicationContext

    @Test
    fun `runner context is isolated and can start without contacting external services`() {
        assertEquals(1, context.getBeansOfType(JooqScope::class.java).size)
        assertFalse(context.getBean(WorkerFactory::class.java).isStarted)
        assertFalse(context.containsBean("backendApplication"))
        assertFalse(context.containsBean("authorizationServerApplication"))
    }
}
