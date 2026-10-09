package ru.alfahack.elephants.backend.utils.params

import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.utils.exceptions.user.BadArgumentException
import ru.alfahack.elephants.backend.utils.exceptions.user.BadFilterArgumentException
import ru.alfahack.elephants.backend.utils.exceptions.user.BadOrderArgumentException
import ru.alfahack.elephants.backend.utils.params.filter.FilterDatatype
import ru.alfahack.elephants.backend.utils.params.filter.castFilterValueType
import ru.alfahack.elephants.backend.utils.params.pagination.PaginationParam
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ParamsAndExceptionsTest {
    @Test
    fun `casts supported filter value types`() {
        assertEquals(12, castFilterValueType("12", Int::class))
        assertEquals("value", castFilterValueType("value", String::class))
        assertEquals(true, castFilterValueType("true", Boolean::class))
    }

    @Test
    fun `rejects unsupported filter value type`() {
        assertFailsWith<BadFilterArgumentException> {
            castFilterValueType("1.5", Double::class)
        }
    }

    @Test
    fun `parameter models and specialized exceptions expose supplied values`() {
        val pagination = PaginationParam(limit = 50, start = 5)

        assertEquals(50, pagination.limit)
        assertEquals(5, pagination.start)
        assertIs<BadArgumentException>(BadFilterArgumentException("bad filter"))
        assertIs<BadArgumentException>(BadOrderArgumentException("bad order"))
        assertEquals(Int::class, FilterDatatype.INT.typeClass)
    }
}
