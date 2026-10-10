package ru.alfahack.elephants.backend.domain.service.parameter.entity

data class ParameterValue(
    val id: Long,
    val parameterType: ParameterType,
    val parameterValue: String,
)