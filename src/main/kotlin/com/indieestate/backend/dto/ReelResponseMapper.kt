package com.indieestate.backend.dto

import com.indieestate.backend.entity.ReelEntity
import com.indieestate.backend.entity.UserEntity

fun ReelEntity.toResponse(user: UserEntity?) = ReelResponse(
    id = id,
    videoId = videoId,
    userId = userId,
    sellerName = user?.name?.takeIf { it.isNotBlank() } ?: "Seller",
    sellerInitials = reelSellerInitials(user?.name),
    category = category,
    title = title,
    caption = caption,
    priceLabel = priceLabel,
    location = location,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    thumbnailHex = thumbnailHex,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = false,
    comments = emptyList<ReelCommentResponse>(),
    status = status,
    createdAt = createdAt,
)

private fun reelSellerInitials(name: String?): String {
    val parts = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "IE"
    return parts.take(2).joinToString("") { it.first().uppercase() }
}
