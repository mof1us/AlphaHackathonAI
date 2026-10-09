package ru.alfahack.elephants.backend.utils.params.order

import ru.alfahack.elephants.backend.utils.exceptions.user.BadOrderArgumentException

inline fun <reified T> orderValidator(
    orders: List<OrderParam>,
): List<OrderParam> where T : Enum<T>, T : IPossibleOrders {
    return orders.map { order ->
        val allowedOrder = enumValues<T>().find { allowedOrder ->
            allowedOrder.orderName == order.field
        }
        if (allowedOrder == null) throw BadOrderArgumentException("Сортировку нельзя использовать")
        order
    }
}
