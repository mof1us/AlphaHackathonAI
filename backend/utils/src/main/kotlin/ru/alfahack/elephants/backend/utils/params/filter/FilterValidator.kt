package ru.alfahack.elephants.backend.utils.params.filter

import ru.alfahack.elephants.backend.utils.exceptions.user.BadFilterArgumentException
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.IFilter
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.BooleanFilter
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.IntFilter
import ru.alfahack.elephants.backend.utils.params.filter.filterPublicObject.StringFilter
import kotlin.reflect.KClass

inline fun <reified T> filterValidator(
    filters: List<FilterParam>
): List<IFilter<*>> where T : Enum<T>, T : IPossibleFilters {
    val filters: List<IFilter<*>> = filters.map { filter ->
        val allowedFilter = enumValues<T>().find { allowedFilter ->
            allowedFilter.filterName == filter.field
        }   
        if (allowedFilter == null) throw BadFilterArgumentException("Фильтр нельзя использовать")
        val filterValue =
            castFilterValueType(filter.value, allowedFilter.dataType.typeClass)
        return@map when (allowedFilter.dataType) {
            FilterDatatype.INT -> IntFilter(filter.field, filter.operator, filterValue as Int)
            FilterDatatype.STRING -> StringFilter(filter.field, filter.operator, filterValue as String)
            FilterDatatype.BOOLEAN -> BooleanFilter(filter.field, filter.operator, filterValue as Boolean)
        }
    }
    return filters
}


fun castFilterValueType(inputValue: String, kClass: KClass<*>): Any = when (kClass) {
    Int::class -> inputValue.toIntOrNull()
        ?: throw BadFilterArgumentException("Wrong type for value $inputValue. Expected Int")

    String::class -> inputValue
    Boolean::class -> inputValue.toBooleanStrictOrNull()
        ?: throw BadFilterArgumentException("Wrong type for value $inputValue. Expected Boolean")
    else -> throw BadFilterArgumentException("Unsupported type: ${kClass.simpleName}")
}
