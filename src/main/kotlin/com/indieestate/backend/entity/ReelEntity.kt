package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reels")
class ReelEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var title: String = "",
    @Column(nullable = false, columnDefinition = "TEXT")
    var caption: String = "",
    @Column(nullable = false)
    var category: String = "",
    @Column(name = "price_label", nullable = false)
    var priceLabel: String = "",
    @Column(nullable = false)
    var location: String = "",
    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    var videoUrl: String = "",
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    var thumbnailUrl: String? = null,
    @Column(name = "thumbnail_hex", nullable = false)
    var thumbnailHex: String = "#1A2744",
    @Column(name = "object_key", columnDefinition = "TEXT")
    var objectKey: String? = null,
    @Column(name = "video_id")
    var videoId: UUID? = null,
    @Column(nullable = false)
    var status: String = ReelStatus.READY,
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,
    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

object ReelStatus {
    const val PROCESSING = "PROCESSING"
    const val READY = "READY"
    const val FAILED = "FAILED"
}
