package ru.alfahack.elephants.backend.data.config.helpers

import org.jooq.Field
import org.jooq.impl.DSL

fun <T> Field<T>.inIfNotNull(values: Collection<T>?) = if (values == null){
    DSL.noCondition()
} else {
    this.`in`(values)
}

fun <T> Field<T>.notInIfNotNull(values: Collection<T>?) = if (values == null){
    DSL.noCondition()
} else {
    this.notIn(values)
}

