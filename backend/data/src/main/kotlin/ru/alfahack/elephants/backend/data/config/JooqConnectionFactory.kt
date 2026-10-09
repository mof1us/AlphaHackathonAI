package ru.alfahack.elephants.backend.data.config

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import org.springframework.r2dbc.connection.ConnectionFactoryUtils
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono

/**
 * Даёт jOOQ соединение, привязанное к Spring-транзакции, сохраняя владение у Spring.
 *
 * jOOQ закрывает соединение после каждого запроса. Для уже открытой транзакции
 * это закрытие подавляется: commit, rollback и возврат в пул выполняет Spring.
 * Вне транзакции используется обычное получение и освобождение соединения.
 */
class JooqConnectionFactory(target: ConnectionFactory) : TransactionAwareConnectionFactoryProxy(target) {
    override fun create(): Mono<Connection> =
        TransactionSynchronizationManager.forCurrentTransaction()
            .flatMap { manager ->
                if (manager.isActualTransactionActive || manager.isSynchronizationActive) {
                    ConnectionFactoryUtils.doGetConnection(targetConnectionFactory)
                        .map { BorrowedConnection(it) as Connection }
                } else {
                    super.create()
                }
            }
            .onErrorResume(NoTransactionException::class.java) { super.create() }

    /** Закрытие запроса не меняет ресурс, которым владеет менеджер транзакций. */
    private class BorrowedConnection(private val target: Connection) : Connection by target {
        override fun close(): Mono<Void> = Mono.empty()
    }
}
