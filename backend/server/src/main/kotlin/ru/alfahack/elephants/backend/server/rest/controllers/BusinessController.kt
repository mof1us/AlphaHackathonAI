package ru.alfahack.elephants.backend.server.rest.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.alfahack.elephants.backend.domain.service.business.BusinessService
import ru.alfahack.elephants.backend.domain.service.business.entity.BusinessEntity

@RestController
class BusinessController(
    private val businessService: BusinessService
) {
    @GetMapping("/business")
    suspend fun getBusinessByIds(
        @RequestParam
        ids: List<Long>
    ): List<BusinessEntity>{
        return businessService.getByIds(ids)
    }
}