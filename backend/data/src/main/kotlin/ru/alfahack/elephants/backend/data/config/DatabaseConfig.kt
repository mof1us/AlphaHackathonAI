package ru.alfahack.elephants.backend.data.config

import com.zaxxer.hikari.HikariDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager

/** Единые параметры PostgreSQL для JDBC, R2DBC и всех модулей приложения. */
@ConfigurationProperties("app.datasource")
data class DatabaseProperties(
    val host: String = "localhost",
    val port: Int = 5432,
    val database: String = "elephants",
    val username: String = "postgres",
    val password: String = "postgres",
) {
    init {
        require(host.isNotBlank() && database.isNotBlank() && username.isNotBlank())
        require(port in 1..65535)
    }
}

/** Создаёт ленивые пулы одной БД и единственный реактивный менеджер транзакций. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DatabaseProperties::class)
class DatabaseConfig {
    @Bean(destroyMethod = "close")
    fun dataSource(properties: DatabaseProperties): HikariDataSource = HikariDataSource().apply {
        jdbcUrl = "jdbc:postgresql://${properties.host}:${properties.port}/${properties.database}"
        username = properties.username
        password = properties.password
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 0
    }

    @Bean(destroyMethod = "dispose")
    fun connectionFactory(
        properties: DatabaseProperties,
    ): ConnectionPool = ConnectionPool(ConnectionPoolConfiguration.builder(ConnectionFactories.get(
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, properties.host)
            .option(ConnectionFactoryOptions.PORT, properties.port)
            .option(ConnectionFactoryOptions.DATABASE, properties.database)
            .option(ConnectionFactoryOptions.USER, properties.username)
            .option(ConnectionFactoryOptions.PASSWORD, properties.password)
            .build(),
    )).initialSize(0).maxSize(10).build())

    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager =
        R2dbcTransactionManager(connectionFactory)
}
