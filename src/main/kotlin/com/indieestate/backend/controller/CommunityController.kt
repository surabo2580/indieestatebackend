package com.indieestate.backend.controller

import com.indieestate.backend.dto.CommunityCommentResponse
import com.indieestate.backend.dto.CommunityPostDetailResponse
import com.indieestate.backend.dto.CommunityPostResponse
import com.indieestate.backend.dto.CreateCommunityCommentRequest
import com.indieestate.backend.dto.CreateCommunityPostRequest
import com.indieestate.backend.security.UserPrincipal
import com.indieestate.backend.service.CommunityService
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
@RequestMapping("/api/community")
@Tag(name = "Community")
class CommunityController(
    private val communityService: CommunityService,
) {

    @GetMapping("/posts")
    @Operation(summary = "Community feed")
    fun feed(
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "30") limit: Int,
    ): List<CommunityPostResponse> = communityService.feed(category, limit)

    @GetMapping("/posts/{id}")
    @Operation(summary = "Post with discussion")
    fun detail(@PathVariable id: UUID): CommunityPostDetailResponse = communityService.detail(id)

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Create a text community post")
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateCommunityPostRequest,
    ): CommunityPostResponse = communityService.create(principal.userId, request)

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Reply on a community post")
    fun comment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateCommunityCommentRequest,
    ): CommunityCommentResponse = communityService.comment(principal.userId, id, request)
}
