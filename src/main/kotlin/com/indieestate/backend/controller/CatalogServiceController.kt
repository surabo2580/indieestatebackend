package com.indieestate.backend.controller

import com.indieestate.backend.dto.CreateServiceRequest
import com.indieestate.backend.dto.ServiceResponse
import com.indieestate.backend.service.CatalogServiceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a service or subcategory")
    fun create(@Valid @RequestBody request: CreateServiceRequest): ServiceResponse =
        catalogServiceService.create(request)

    @PutMapping("/{id}")
    @Operation(summary = "Update a service or subcategory")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateServiceRequest,
    ): ServiceResponse = catalogServiceService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a service or subcategory")
    fun delete(@PathVariable id: UUID) {
        catalogServiceService.deactivate(id)
    }

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "List subcategories for a service")
    fun subcategories(@PathVariable id: UUID): List<ServiceResponse> =
        catalogServiceService.listSubcategories(id)
}
