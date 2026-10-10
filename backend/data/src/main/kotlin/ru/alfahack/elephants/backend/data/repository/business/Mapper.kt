package ru.alfahack.elephants.backend.data.repository.business

import ru.alfahack.elephants.backend.data.jooq.tables.records.BusinessRecord
import ru.alfahack.elephants.backend.domain.service.business.entity.BusinessEntity

internal fun BusinessRecord.toDomain(): BusinessEntity = BusinessEntity(
    id = id!!,
    title = title!!,
    description = description!!,
    isIndexed = isIndexed!!,
)