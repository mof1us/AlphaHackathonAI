package ru.alfahack.elephants.backend.data.repository.business

import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.alfahack.elephants.backend.data.config.dsl.JooqScope
import ru.alfahack.elephants.backend.data.jooq.tables.references.BUSINESS
import ru.alfahack.elephants.backend.domain.repository.IBusinessRepository
import ru.alfahack.elephants.backend.domain.service.business.entity.BusinessEntity

@Repository
class BusinessRepository(
    private val jooqScope: JooqScope,
): IBusinessRepository {
    override suspend fun searchBusinessByIds(ids: List<Long>): List<BusinessEntity> = jooqScope.use {
        selectFrom(BUSINESS)
            .where(
                DSL.noCondition()
                    .and(BUSINESS.ID.`in`(ids))
            )
            .awaitList()
            .map { it.toDomain() }
    }

    override suspend fun createBusiness(businessEntities: List<BusinessEntity>){

    }

}