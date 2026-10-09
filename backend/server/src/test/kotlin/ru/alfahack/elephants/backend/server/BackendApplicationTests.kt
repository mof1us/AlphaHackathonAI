package ru.alfahack.elephants.backend.server

import io.temporal.worker.WorkerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import ru.alfahack.elephants.backend.data.config.dsl.JooqScope
import kotlin.test.*

@SpringBootTest
class BackendApplicationTests {
    @Autowired private lateinit var context: ApplicationContext

    @Test
    fun `API starts with one scope and never starts workers or schedules`() {
        assertEquals(1, context.getBeansOfType(JooqScope::class.java).size)
        assertFalse(context.getBean(WorkerFactory::class.java).isStarted)
        assertFalse(context.containsBean("temporalWorkerLifecycle"))
        assertFalse(context.containsBean("temporalScheduleBootstrap"))
        assertFalse(context.containsBean("authorizationServerApplication"))
    }
}
