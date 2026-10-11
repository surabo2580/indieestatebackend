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
    name = "conversations",
    indexes = [
        Index(name = "idx_conversations_low_updated", columnList = "user_low_id, last_message_at"),
        Index(name = "idx_conversations_high_updated", columnList = "user_high_id, last_message_at"),
        Index(name = "idx_conversations_pair_key", columnList = "pair_key", unique = true),
    ],
)
class ConversationEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_low_id", nullable = false)
    var userLowId: UUID = UUID.randomUUID(),
    @Column(name = "user_high_id", nullable = false)
    var userHighId: UUID = UUID.randomUUID(),
    @Column(name = "pair_key", nullable = false, unique = true)
    var pairKey: String = "",
    @Column(name = "ad_id")
    var adId: UUID? = null,
    @Column(name = "ad_title")
    var adTitle: String? = null,
    @Column(name = "last_message", length = 500)
    var lastMessage: String? = null,
    @Column(name = "last_message_at", nullable = false)
    var lastMessageAt: Instant = Instant.now(),
    @Column(name = "last_sender_id")
    var lastSenderId: UUID? = null,
    @Column(name = "user_low_unread", nullable = false)
    var userLowUnread: Int = 0,
    @Column(name = "user_high_unread", nullable = false)
    var userHighUnread: Int = 0,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
