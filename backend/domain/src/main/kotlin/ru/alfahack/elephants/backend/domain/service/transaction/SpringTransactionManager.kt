package ru.alfahack.elephants.backend.domain.service.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

/**
 * Выполняет доменные сценарии в реактивной транзакции Spring.
 *
 * Находится в доменном модуле, поскольку транзакционная граница является частью
 * сценария работы с несколькими агрегатами, а конкретный менеджер транзакций
 * предоставляется инфраструктурной конфигурацией приложения.
 */
@Component
class SpringTransactionManager(
    delegate: ReactiveTransactionManager,
) : ITransactionManager {
    private val operator = TransactionalOperator.create(delegate)

    override suspend fun <T> transaction(block: suspend () -> T): T =
        operator.executeAndAwait { block() }
}
