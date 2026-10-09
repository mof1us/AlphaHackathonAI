package ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject

import ru.alfahack.elephants.backend.utils.params.filter.FilterOperator

interface IFilter <T>{
    val field: String
    val operator: FilterOperator
    val value: T
}
