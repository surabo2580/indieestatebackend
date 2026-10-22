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
    name = "community_comments",
    indexes = [
        Index(name = "idx_community_comments_post", columnList = "post_id, created_at"),
    ],
)
class CommunityCommentEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "post_id", nullable = false)
    var postId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),
    @Column(name = "author_name", nullable = false)
    var authorName: String = "Neighbour",
    @Column(nullable = false, length = 500)
    var body: String = "",
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
