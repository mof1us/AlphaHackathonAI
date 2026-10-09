package ru.alfahack.elephants.backend.utils.params.order

class OrderParam(
    override val field: String,
    override val direction: OrderDirection,
) : IOrder
