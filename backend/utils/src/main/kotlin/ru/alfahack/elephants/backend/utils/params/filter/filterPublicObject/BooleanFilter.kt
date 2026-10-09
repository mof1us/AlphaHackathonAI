package ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject

import ru.alfahack.elephants.backend.utils.params.filter.FilterOperator

/** Представляет булево значение фильтра после проверки входных параметров. */
class BooleanFilter(
    override val field: String,
    override val operator: FilterOperator,
    override val value: Boolean,
) : IFilter<Boolean>
