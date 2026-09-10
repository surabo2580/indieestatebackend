package com.indieestate.backend.controller

import com.indieestate.backend.dto.AdResponse
import com.indieestate.backend.dto.CreateAdRequest
import com.indieestate.backend.security.UserPrincipal
import com.indieestate.backend.service.AdService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ads")
@Tag(name = "Ads")
@SecurityRequirement(name = "bearer-jwt")
class AdController(
    private val adService: AdService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a classified ad from a dynamic form")
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateAdRequest,
    ): AdResponse = adService.create(principal.userId, request)

    @GetMapping("/me")
    @Operation(summary = "List ads posted by the current user")
    fun listMine(@AuthenticationPrincipal principal: UserPrincipal): List<AdResponse> =
        adService.listMine(principal.userId)
}
