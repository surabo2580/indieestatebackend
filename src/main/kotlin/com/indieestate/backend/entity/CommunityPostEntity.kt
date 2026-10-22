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
    name = "community_posts",
    indexes = [
        Index(name = "idx_community_posts_created", columnList = "created_at"),
        Index(name = "idx_community_posts_category_created", columnList = "category, created_at"),
    ],
)
class CommunityPostEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),
    @Column(name = "author_name", nullable = false)
    var authorName: String = "Neighbour",
    @Column(nullable = false, length = 32)
    var category: String = CommunityCategory.OTHER,
    @Column(nullable = false, length = 500)
    var body: String = "",
    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

object CommunityCategory {
    const val NEWS = "NEWS"
    const val URGENT = "URGENT"
    const val LOST = "LOST"
    const val NEED = "NEED"
    const val OTHER = "OTHER"

    val all = setOf(NEWS, URGENT, LOST, NEED, OTHER)

    fun normalize(raw: String?): String {
        val value = raw?.trim()?.uppercase().orEmpty()
        return if (value in all) value else OTHER
    }
}
