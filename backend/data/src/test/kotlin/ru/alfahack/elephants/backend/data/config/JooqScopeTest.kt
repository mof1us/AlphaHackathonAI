package ru.alfahack.elephants.backend.data.config

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import reactor.core.publisher.Sinks
import ru.alfahack.elephants.backend.data.config.dsl.JooqConfig
import ru.alfahack.elephants.backend.data.config.dsl.JooqScope
import ru.alfahack.elephants.backend.domain.service.transaction.SpringTransactionManager
import kotlin.test.*

class JooqScopeTest {
    private val fixture = R2dbcFixture()
    private val scope = JooqScope(JooqConfig().dslContext(fixture.factory))
    private val transactions = SpringTransactionManager(R2dbcTransactionManager(fixture.factory))
    private val id = DSL.field(DSL.name("id"), Long::class.java)
    private val name = DSL.field(DSL.name("name"), String::class.java)
    private val table = DSL.select(id, name).from("probe").asTable("probe")
    private val writableTable = DSL.table(DSL.name("probe"))

    @Test
    fun `use returns arbitrary nullable and Unit values without opening a connection`() = runBlocking {
        val expected = Any()
        assertSame(expected, scope.use { yield(); expected })
        assertNull(scope.use<String?> { null })
        assertEquals(Unit, scope.use { Unit })
        assertEquals(0, fixture.opened)
    }

    @Test
    fun `select maps rows and count and releases each connection outside transactions`() = runBlocking {
        fixture.rows(listOf(7L, "value"))
        assertEquals(listOf("value"), scope.use { selectFrom(table).awaitList().map { it.get(name) } })
        assertEquals(7L, scope.use { selectFrom(table).awaitOne()?.get(id) })
        fixture.rows(listOf(2))
        assertEquals(2, scope.use { fetchCount(table, DSL.trueCondition()) })
        assertEquals(3, fixture.opened)
        assertEquals(3, fixture.closed)
        assertEquals(0, fixture.begun)
    }

    @Test
    fun `awaitOne returns null for no rows and rejects more than one`() = runBlocking {
        fixture.rows()
        assertNull(scope.use { selectFrom(table).awaitOne() })
        fixture.rows(listOf(1L, "one"), listOf(2L, "two"))
        assertFailsWith<IndexOutOfBoundsException> { scope.use { selectFrom(table).awaitOne() } }
        assertEquals(2, fixture.closed)
    }

    @Test
    fun `update suspends until the driver finishes and supports zero affected rows`() = runBlocking {
        val pending = Sinks.one<io.r2dbc.spi.Result>()
        fixture.results = pending.asMono()
        val count = async(start = CoroutineStart.UNDISPATCHED) {
            scope.use { update(writableTable).set(name, "changed").where(id.eq(7)).awaitRowsUpdated() }
        }
        assertFalse(count.isCompleted)
        assertEquals(0, fixture.closed)
        pending.tryEmitValue(fixture.result(emptyList(), updated = 2)).orThrow()
        assertEquals(2, count.await())
        fixture.rows()
        assertEquals(0, scope.use { deleteFrom(writableTable).where(id.eq(7)).awaitRowsUpdated() })
        assertEquals(2, fixture.closed)
    }

    @Test
    fun `sequential queries share one connection and commit only at the transaction boundary`() = runBlocking {
        fixture.rows(listOf(1L, "one"))
        transactions.transaction {
            repeat(2) { scope.use { selectFrom(table).awaitOne() } }
            assertEquals(1, fixture.opened)
            assertEquals(0, fixture.closed)
            assertEquals(0, fixture.committed)
        }
        assertEquals(1, fixture.begun)
        assertEquals(1, fixture.committed)
        assertEquals(1, fixture.closed)
    }

    @Test
    fun `exception rolls back and releases the connection`() = runBlocking {
        fixture.rows()
        assertFailsWith<IllegalStateException> {
            transactions.transaction {
                scope.use { selectFrom(table).awaitList() }
                error("rollback")
            }
        }
        assertEquals(0, fixture.committed)
        assertEquals(1, fixture.rolledBack)
        assertEquals(1, fixture.closed)
    }

    @Test
    fun `cancelling a suspended transaction rolls back and releases the connection`() = runBlocking {
        fixture.rows()
        val job = async(start = CoroutineStart.UNDISPATCHED) {
            transactions.transaction {
                scope.use { selectFrom(table).awaitList() }
                awaitCancellation()
            }
        }
        job.cancelAndJoin()
        assertEquals(0, fixture.committed)
        assertEquals(1, fixture.rolledBack)
        assertEquals(1, fixture.closed)
    }
}
