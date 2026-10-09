package ru.alfahack.elephants.backend.domain.service.transaction

/**
 * Выполняет доменный сценарий в транзакции.
 * Реализация определяет способ открытия, подтверждения и отката транзакции.
 */
interface ITransactionManager {
    suspend fun <T> transaction(block: suspend () -> T): T
}
