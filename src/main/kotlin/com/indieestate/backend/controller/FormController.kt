package com.indieestate.backend.controller

import com.indieestate.backend.dto.CreateFormRequest
import com.indieestate.backend.dto.FormResponse
import com.indieestate.backend.dto.UpdateFormRequest
import com.indieestate.backend.service.FormService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/forms")
@Tag(name = "Forms")
class FormController(
    private val formService: FormService,
) {

    @GetMapping
    @Operation(summary = "Fetch dynamic form schema by category, service, or code")
    fun list(
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) serviceId: UUID?,
        @RequestParam(required = false) code: String?,
    ): List<FormResponse> = formService.list(categoryId, serviceId, code)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the sell form for a category or service subcategory")
    fun create(@Valid @RequestBody request: CreateFormRequest): FormResponse =
        formService.create(request)

    @PutMapping("/{id}")
    @Operation(summary = "Update a sell form")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateFormRequest,
    ): FormResponse = formService.update(id, request)
}
