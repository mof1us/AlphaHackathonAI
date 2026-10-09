package ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject

import ru.alfahack.elephants.backend.utils.params.filter.FilterOperator

class StringFilter(
    override val field: String,
    override val operator: FilterOperator,
    override val value: String
) : IFilter<String>
