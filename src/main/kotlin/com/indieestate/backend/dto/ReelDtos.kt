package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateReelRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    @field:NotBlank
    @field:Size(max = 2000)
    val caption: String,
    @field:NotBlank
    @field:Size(max = 64)
    val category: String,
    @field:NotBlank
    @field:Size(max = 64)
    val priceLabel: String,
    @field:NotBlank
    @field:Size(max = 160)
    val location: String,
    @field:NotBlank
    @field:Size(max = 2000)
    val videoUrl: String,
    @field:Size(max = 2000)
    val thumbnailUrl: String? = null,
    @field:Size(max = 16)
    val thumbnailHex: String? = null,
)

data class CreateReelUploadRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val fileName: String,
    @field:Size(max = 128)
    val contentType: String? = null,
)

data class CreateReelUploadResponse(
    val videoId: UUID,
    val objectKey: String,
    val uploadUrl: String,
    val contentType: String,
)

data class CompleteReelRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    @field:NotBlank
    @field:Size(max = 2000)
    val caption: String,
    @field:NotBlank
    @field:Size(max = 64)
    val category: String,
    @field:NotBlank
    @field:Size(max = 64)
    val priceLabel: String,
    @field:NotBlank
    @field:Size(max = 160)
    val location: String,
    @field:NotBlank
    @field:Size(max = 2000)
    val objectKey: String,
    @field:NotNull
    val videoId: UUID,
    @field:Size(max = 16)
    val thumbnailHex: String? = null,
)

data class ReelCommentResponse(
    val id: String,
    val author: String,
    val text: String,
    val timeLabel: String,
)

data class ReelResponse(
    val id: UUID,
    val videoId: UUID? = null,
    val userId: UUID,
    val sellerName: String,
    val sellerInitials: String,
    val category: String,
    val title: String,
    val caption: String,
    val priceLabel: String,
    val location: String,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val thumbnailHex: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val comments: List<ReelCommentResponse>,
    val status: String,
    val createdAt: Instant,
)
