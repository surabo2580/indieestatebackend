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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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

    @GetMapping
    @Operation(summary = "List active ads for a category or service")
    fun list(
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) serviceId: UUID?,
    ): List<AdResponse> = adService.listActive(categoryId, serviceId)

    @GetMapping("/me")
    @Operation(summary = "List ads posted by the current user")
    fun listMine(@AuthenticationPrincipal principal: UserPrincipal): List<AdResponse> =
        adService.listMine(principal.userId)

    @PostMapping("/{id}/sold")
    @Operation(summary = "Mark one of the current user's ads as sold")
    fun markSold(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): AdResponse = adService.markSold(principal.userId, id)

    @PostMapping("/{id}/republish")
    @Operation(summary = "Republish one of the current user's ads")
    fun republish(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): AdResponse = adService.republish(principal.userId, id)
}
