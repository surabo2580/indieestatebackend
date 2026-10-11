package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(name = "idx_chat_messages_thread", columnList = "conversation_id, created_at"),
    ],
)
class ChatMessageEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "conversation_id", nullable = false)
    var conversationId: UUID = UUID.randomUUID(),
    @Column(name = "sender_id", nullable = false)
    var senderId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 2000)
    var body: String = "",
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
