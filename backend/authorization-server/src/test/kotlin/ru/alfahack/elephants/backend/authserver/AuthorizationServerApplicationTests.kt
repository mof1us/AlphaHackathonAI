package ru.alfahack.elephants.backend.authserver

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import ru.alfahack.elephants.backend.data.config.dsl.JooqScope
import kotlin.test.*

@SpringBootTest
class AuthorizationServerApplicationTests {
    @Autowired private lateinit var context: ApplicationContext

    @Test
    fun `authorization server uses the same data module without API or workers`() {
        assertEquals(1, context.getBeansOfType(JooqScope::class.java).size)
        assertFalse(context.containsBean("backendApplication"))
        assertFalse(context.containsBean("temporalWorkerLifecycle"))
    }
}
