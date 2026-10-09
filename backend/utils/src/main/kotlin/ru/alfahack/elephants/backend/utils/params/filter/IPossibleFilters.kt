package ru.alfahack.elephants.backend.utils.params.filter

interface IPossibleFilters {
    val filterName: String
    val dataType: FilterDatatype
}
