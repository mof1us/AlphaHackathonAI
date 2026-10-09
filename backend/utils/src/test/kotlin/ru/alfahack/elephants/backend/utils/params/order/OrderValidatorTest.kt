package ru.alfahack.elephants.backend.utils.params.order

import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.utils.exceptions.user.BadOrderArgumentException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderValidatorTest {
    @Test
    fun `keeps allowed order fields and directions`() {
        val orders = listOf(
            OrderParam("id", OrderDirection.ASC),
            OrderParam("title", OrderDirection.DESC),
        )

        assertEquals(orders, orderValidator<TestOrderField>(orders))
    }

    @Test
    fun `rejects fields that are not allowed`() {
        assertFailsWith<BadOrderArgumentException> {
            orderValidator<TestOrderField>(listOf(OrderParam("createdAt", OrderDirection.ASC)))
        }
    }

    private enum class TestOrderField(
        override val orderName: String,
    ) : IPossibleOrders {
        ID("id"),
        TITLE("title"),
    }
}
