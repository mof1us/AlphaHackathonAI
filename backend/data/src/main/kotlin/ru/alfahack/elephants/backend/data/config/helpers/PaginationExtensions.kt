package ru.alfahack.elephants.backend.data.config.helpers

import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.SelectLimitStep
import ru.alfahack.elephants.backend.utils.params.pagination.PaginationParam

fun <R : Record> SelectLimitStep<R>.withPagination(pagination: PaginationParam?): ResultQuery<R> =
    pagination?.let {
        limit(it.limit).offset(it.start)
    } ?: this
