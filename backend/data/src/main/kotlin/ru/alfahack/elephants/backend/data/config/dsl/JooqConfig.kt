package ru.alfahack.elephants.backend.data.config.dsl

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.reactor.extensions.CoreSubscriberProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.alfahack.elephants.backend.data.config.JooqConnectionFactory

/** Единый jOOQ-контекст: R2DBC, Spring-транзакции и сохранение Reactor Context. */
@Configuration(proxyBeanMethods = false)
class JooqConfig {
    @Bean
    fun jooqScope(dsl: DSLContext): JooqScope = JooqScope(dsl)

    @Bean
    fun dslContext(connectionFactory: ConnectionFactory): DSLContext =
        DSL.using(
            DefaultConfiguration()
                .set(JooqConnectionFactory(connectionFactory))
                .set(SQLDialect.POSTGRES)
                .set(CoreSubscriberProvider()),
        )
}
