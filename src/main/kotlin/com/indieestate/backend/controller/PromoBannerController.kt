package com.indieestate.backend.controller

import com.indieestate.backend.dto.CreatePromoBannerRequest
import com.indieestate.backend.dto.PromoBannerResponse
import com.indieestate.backend.service.PromoBannerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/banners")
@CrossOrigin(origins = ["*"])
@Tag(name = "Banners")
class PromoBannerController(
    private val promoBannerService: PromoBannerService,
) {

    @GetMapping
    @Operation(summary = "List active advertisement banners")
    fun list(): List<PromoBannerResponse> = promoBannerService.listActive()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an advertisement banner")
    fun create(@Valid @RequestBody request: CreatePromoBannerRequest): PromoBannerResponse =
        promoBannerService.create(request)
}
