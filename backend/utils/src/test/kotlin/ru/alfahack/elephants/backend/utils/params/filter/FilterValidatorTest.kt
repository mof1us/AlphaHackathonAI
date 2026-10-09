package ru.alfahack.elephants.backend.utils.params.filter

import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.utils.exceptions.user.BadFilterArgumentException
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.IntFilter
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.BooleanFilter
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.StringFilter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class FilterValidatorTest {
    @Test
    fun `converts supported filter values to their declared types`() {
        val filters = filterValidator<TestFilterField>(
            listOf(
                FilterParam("year", "2024", FilterOperator.EQUAL),
                FilterParam("title", "Final", FilterOperator.NOTEQUAL),
                FilterParam("isDraft", "true", FilterOperator.EQUAL),
            ),
        )

        val year = assertIs<IntFilter>(filters[0])
        val title = assertIs<StringFilter>(filters[1])
        val isDraft = assertIs<BooleanFilter>(filters[2])
        assertEquals(2024, year.value)
        assertEquals("Final", title.value)
        assertEquals(true, isDraft.value)
    }

    @Test
    fun `rejects unknown fields and invalid integer values`() {
        assertFailsWith<BadFilterArgumentException> {
            filterValidator<TestFilterField>(listOf(FilterParam("unknown", "1", FilterOperator.EQUAL)))
        }
        assertFailsWith<BadFilterArgumentException> {
            filterValidator<TestFilterField>(listOf(FilterParam("year", "not-a-number", FilterOperator.EQUAL)))
        }
        assertFailsWith<BadFilterArgumentException> {
            filterValidator<TestFilterField>(listOf(FilterParam("isDraft", "yes", FilterOperator.EQUAL)))
        }
    }

    private enum class TestFilterField(
        override val filterName: String,
        override val dataType: FilterDatatype,
    ) : IPossibleFilters {
        YEAR("year", FilterDatatype.INT),
        TITLE("title", FilterDatatype.STRING),
        IS_DRAFT("isDraft", FilterDatatype.BOOLEAN),
    }
}
