package com.indieestate.backend.controller

import com.indieestate.backend.dto.ServiceResponse
import com.indieestate.backend.service.CatalogServiceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = ["*"])
@Tag(name = "Services")
class CatalogServiceController(
    private val catalogServiceService: CatalogServiceService,
) {

    @GetMapping
    @Operation(summary = "List services with subcategories")
    fun list(): List<ServiceResponse> = catalogServiceService.listActive()

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "List subcategories for a service")
    fun subcategories(@PathVariable id: UUID): List<ServiceResponse> =
        catalogServiceService.listSubcategories(id)
}
