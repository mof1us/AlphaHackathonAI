package ru.alfahack.elephants.backend.data.config

import com.zaxxer.hikari.HikariDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.transaction.ReactiveTransactionManager
import ru.alfahack.elephants.backend.data.config.dsl.JooqConfig
import ru.alfahack.elephants.backend.data.config.dsl.JooqScope
import ru.alfahack.elephants.backend.domain.service.transaction.ITransactionManager
import ru.alfahack.elephants.backend.domain.service.transaction.SpringTransactionManager
import kotlin.test.*

class DatabaseConfigTest {
    @Test
    fun `one database supplies one scope and one lazy pool without connecting`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(MapPropertySource("test", mapOf(
                "app.datasource.host" to "unreachable.invalid",
                "app.datasource.port" to "5439",
                "app.datasource.database" to "single_database",
                "app.datasource.username" to "test_user",
            )))
            context.register(DatabaseConfig::class.java, JooqConfig::class.java, SpringTransactionManager::class.java)
            context.refresh()
            assertEquals(1, context.getBeansOfType(ConnectionFactory::class.java).size)
            assertEquals(1, context.getBeansOfType(DSLContext::class.java).size)
            assertEquals(1, context.getBeansOfType(JooqScope::class.java).size)
            assertEquals(1, context.getBeansOfType(ReactiveTransactionManager::class.java).size)
            assertNotNull(context.getBean(ITransactionManager::class.java))
            val jdbc = context.getBean(HikariDataSource::class.java)
            assertEquals("jdbc:postgresql://unreachable.invalid:5439/single_database", jdbc.jdbcUrl)
            assertEquals("test_user", jdbc.username)
            assertFalse(jdbc.isRunning)
            val pool = context.getBean(ConnectionPool::class.java)
            assertEquals(0, pool.metrics.orElseThrow().allocatedSize())
        }
    }
}
