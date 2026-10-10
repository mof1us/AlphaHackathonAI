package ru.alfahack.elephants.backend.domain.repository

import ru.alfahack.elephants.backend.domain.service.business.entity.BusinessEntity

interface IBusinessRepository {
    suspend fun searchBusinessByIds(ids: List<Long>): List<BusinessEntity>
    suspend fun createBusiness(businessEntities: List<BusinessEntity>)
}