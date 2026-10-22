package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateCommunityPostRequest(
    val category: String? = null,
    @field:NotBlank
    @field:Size(max = 500)
    val body: String,
)

data class CreateCommunityCommentRequest(
    @field:NotBlank
    @field:Size(max = 500)
    val body: String,
)

data class CommunityPostResponse(
    val id: UUID,
    val authorName: String,
    val category: String,
    val body: String,
    val commentCount: Int,
    val createdAt: Instant,
)

data class CommunityCommentResponse(
    val id: UUID,
    val postId: UUID,
    val authorName: String,
    val body: String,
    val createdAt: Instant,
)

data class CommunityPostDetailResponse(
    val post: CommunityPostResponse,
    val comments: List<CommunityCommentResponse>,
)
