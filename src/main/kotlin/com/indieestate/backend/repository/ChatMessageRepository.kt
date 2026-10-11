package com.indieestate.backend.repository

import com.indieestate.backend.entity.ChatMessageEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ChatMessageRepository : JpaRepository<ChatMessageEntity, UUID> {
    fun findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
        conversationId: UUID,
        createdAt: Instant,
        pageable: Pageable,
    ): List<ChatMessageEntity>

    fun findByConversationIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(
        conversationId: UUID,
        createdAt: Instant,
    ): List<ChatMessageEntity>
}
