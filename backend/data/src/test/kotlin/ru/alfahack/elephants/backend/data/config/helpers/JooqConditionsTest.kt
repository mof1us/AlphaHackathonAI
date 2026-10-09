package ru.alfahack.elephants.backend.data.config.helpers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.Condition
import org.jooq.Field
import org.junit.jupiter.api.Test
import org.jooq.impl.DSL
import kotlin.test.assertSame

class JooqConditionsTest {
    @Test
    fun `in helper returns no condition for absent values`() {
        val field = mockk<Field<Long>>()

        val condition = field.inIfNotNull(null)

        assertSame(DSL.noCondition(), condition)
    }

    @Test
    fun `helpers delegate populated values to jooq field`() {
        val field = mockk<Field<Long>>()
        val inCondition = mockk<Condition>()
        val notInCondition = mockk<Condition>()
        every { field.`in`(listOf(1L, 2L)) } returns inCondition
        every { field.notIn(listOf(3L)) } returns notInCondition

        assertSame(inCondition, field.inIfNotNull(listOf(1L, 2L)))
        assertSame(notInCondition, field.notInIfNotNull(listOf(3L)))
        verify { field.`in`(listOf(1L, 2L)) }
        verify { field.notIn(listOf(3L)) }
    }
}
