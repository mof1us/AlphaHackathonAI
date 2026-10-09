package ru.alfahack.elephants.backend.utils.params.order

interface IOrder {
    val field: String
    val direction: OrderDirection
}
