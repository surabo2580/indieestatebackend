package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class StartConversationRequest(
    val peerUserId: UUID,
    val adId: UUID? = null,
)

data class SendChatMessageRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val body: String,
)

data class ChatMessageResponse(
    val id: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val body: String,
    val createdAt: Instant,
    val mine: Boolean,
)

data class ConversationResponse(
    val id: UUID,
    val peerUserId: UUID,
    val peerName: String,
    val adId: UUID?,
    val adTitle: String?,
    val lastMessage: String?,
    val lastMessageAt: Instant,
    val unreadCount: Int,
)

data class UnreadCountResponse(
    val unreadCount: Long,
)
