package ru.alfahack.elephants.backend.data.config

import io.mockk.every
import io.mockk.mockk
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.BiFunction

/** Подменяет только R2DBC-драйвер: jOOQ, Spring-транзакции и маппинг остаются настоящими. */
internal class R2dbcFixture {
    val connection: Connection = mockk()
    val factory: ConnectionFactory = mockk()
    val sql = mutableListOf<String>()
    val bindings = mutableListOf<Any>()
    var opened = 0
    var closed = 0
    var begun = 0
    var committed = 0
    var rolledBack = 0
    var results: Publisher<out Result> = Flux.just(result(emptyList()))

    init {
        every { factory.metadata.name } returns "PostgreSQL"
        every { factory.create() } returns Mono.defer {
            opened++
            Mono.just(connection)
        }
        every { connection.metadata.databaseProductName } returns "PostgreSQL"
        every { connection.metadata.databaseVersion } returns "17"
        every { connection.isAutoCommit } returns true
        every { connection.setAutoCommit(any()) } returns Mono.empty()
        every { connection.validate(any()) } returns Mono.just(true)
        every { connection.close() } returns Mono.fromRunnable { closed++ }
        every { connection.beginTransaction(any()) } returns Mono.fromRunnable { begun++ }
        every { connection.beginTransaction() } returns Mono.fromRunnable { begun++ }
        every { connection.commitTransaction() } returns Mono.fromRunnable { committed++ }
        every { connection.rollbackTransaction() } returns Mono.fromRunnable { rolledBack++ }
        every { connection.createStatement(any()) } answers {
            sql += firstArg<String>()
            val statement = mockk<Statement>()
            every { statement.bind(any<Int>(), any()) } answers {
                bindings += secondArg<Any>()
                statement
            }
            every { statement.bindNull(any<Int>(), any()) } returns statement
            every { statement.fetchSize(any()) } returns statement
            every { statement.returnGeneratedValues(*anyVararg()) } returns statement
            every { statement.execute() } answers { results }
            statement
        }
    }

    fun rows(vararg rows: List<Any?>) {
        results = Flux.just(result(rows.toList()))
    }

    fun result(rows: List<List<Any?>>, updated: Long = rows.size.toLong()): Result {
        val result = mockk<Result>()
        val metadata = mockk<RowMetadata>()
        every { result.rowsUpdated } returns Mono.just(updated)
        every { result.map(any<BiFunction<Row, RowMetadata, Any>>()) } answers {
            val mapper = firstArg<BiFunction<Row, RowMetadata, Any>>()
            Flux.fromIterable(rows).map { values ->
                val row = mockk<Row>()
                every { row.get(any<Int>(), any<Class<*>>()) } answers {
                    val value = values[firstArg<Int>()]
                    if (secondArg<Class<*>>() == String::class.java) value?.toString() else value
                }
                mapper.apply(row, metadata)
            }
        }
        return result
    }
}
