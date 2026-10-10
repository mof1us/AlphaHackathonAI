package ru.alfahack.elephants.backend.domain.service.parameter.entity

enum class ParameterType(val orderValue: Int) {
    OKVED(1),
    ;

    companion object{
        val ALL = listOf(
            OKVED,
        )
    }

}