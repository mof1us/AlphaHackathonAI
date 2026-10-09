package ru.alfahack.elephants.backend.data.config.dsl

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DeleteWhereStep
import org.jooq.InsertSetStep
import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.RowCountQuery
import org.jooq.SelectWhereStep
import org.jooq.Table
import org.jooq.UpdateSetFirstStep
import reactor.core.publisher.Flux

/**
 * Выполняет jOOQ-запросы без блокировки и скрывает реактивные адаптеры от DAO.
 * Обёртка Flux сохраняет контекст транзакций и наблюдений при переходе из корутин.
 * use возвращает результат блока вместе с выполненным внутри маппингом.
 * Запросы выполняются методами await; scope не создаёт отдельную транзакцию.
 */
class JooqScope(private val dsl: DSLContext) {
    /** Выполняет блок в текущем coroutine-контексте и возвращает его результат без преобразований. */
    suspend fun <T> use(block: suspend JooqScope.() -> T): T = block()

    /** Строит SELECT с типом записи заданной таблицы. */
    fun <R : Record> selectFrom(table: Table<R>): SelectWhereStep<R> = dsl.selectFrom(table)

    /** Строит INSERT; выполнение начинается только при вызове await-метода. */
    fun <R : Record> insertInto(table: Table<R>): InsertSetStep<R> = dsl.insertInto(table)

    /** Строит UPDATE; набор полей задаётся расширением соответствующего агрегата. */
    fun <R : Record> update(table: Table<R>): UpdateSetFirstStep<R> = dsl.update(table)

    /** Строит DELETE с возможностью добавить условие. */
    fun <R : Record> deleteFrom(table: Table<R>): DeleteWhereStep<R> = dsl.deleteFrom(table)

    /** Возвращает все записи запроса, включая INSERT/UPDATE с RETURNING. */
    suspend fun <R : Record> ResultQuery<R>.awaitList(): List<R> =
        Flux.from(this).asFlow().toList()

    /** Возвращает запись или null; несколько записей считаются ошибкой. */
    suspend fun <R : Record> ResultQuery<R>.awaitOne(): R? =
        Flux.from(this).singleOrEmpty().awaitSingleOrNull()

    /** Считает строки таблицы с заданным условием без блокирующего DSLContext.fetchCount. */
    suspend fun fetchCount(table: Table<*>, condition: Condition): Int =
        Flux.from(dsl.selectCount().from(table).where(condition)).awaitSingle().value1()

    /** Выполняет запрос без RETURNING и возвращает число изменённых строк, включая ноль. */
    suspend fun RowCountQuery.awaitRowsUpdated(): Int = Flux.from(this).awaitSingle()
}
