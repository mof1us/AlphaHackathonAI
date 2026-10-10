package ru.alfahack.elephants.backend.domain.service.businessVector.model

import ru.alfahack.elephants.backend.domain.service.parameter.entity.ParameterType
import ru.alfahack.elephants.backend.domain.service.parameter.entity.ParameterValue

class BusinessVectorModel{
    private val vectorMap: MutableMap<ParameterType, String> = mutableMapOf()

    constructor(parameters: List<ParameterValue>){
        val types = parameters.map { it.parameterType }
        require(types.toSet().size == types.size) { "Параметры должны быть уникальные" }
        require(types.containsAll(ParameterType.ALL)) { "Вектор должен содержать все доступные параметры" }
        parameters.forEach { parameter ->
            vectorMap[parameter.parameterType] = parameter.parameterValue
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BusinessVectorModel) return false
        return vectorMap == other.vectorMap
    }

    override fun hashCode(): Int {
        return vectorMap.hashCode()
    }
}
