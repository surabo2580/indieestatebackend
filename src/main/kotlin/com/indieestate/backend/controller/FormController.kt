package com.indieestate.backend.controller

import com.indieestate.backend.dto.FormResponse
import com.indieestate.backend.service.FormService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
}
