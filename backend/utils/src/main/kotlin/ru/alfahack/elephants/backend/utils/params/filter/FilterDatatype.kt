package ru.alfahack.elephants.backend.utils.params.filter

import kotlin.reflect.KClass

enum class FilterDatatype(val typeClass: KClass<*>) {
    INT(Int::class),
    STRING(String::class),
    BOOLEAN(Boolean::class),
    ;
}
