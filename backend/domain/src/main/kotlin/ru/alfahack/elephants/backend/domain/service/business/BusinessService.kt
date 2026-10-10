package ru.alfahack.elephants.backend.domain.service.business

import org.springframework.stereotype.Service
import ru.alfahack.elephants.backend.domain.repository.IBusinessRepository
import ru.alfahack.elephants.backend.domain.service.business.entity.BusinessEntity

@Service
class BusinessService(
    private val businessRepository: IBusinessRepository,
) {
    suspend fun getByIds(ids: List<Long>): List<BusinessEntity> =
        businessRepository.searchBusinessByIds(ids)
}