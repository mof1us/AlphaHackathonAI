package ru.alfahack.elephants.backend.domain.service.business.entity

data class BusinessEntity(
    val id: Long,
    val title: String,
    val description: String,
    val isIndexed: Boolean,
)
