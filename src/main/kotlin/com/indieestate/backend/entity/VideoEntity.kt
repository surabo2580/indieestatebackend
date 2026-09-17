package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "videos")
class VideoEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var status: String = VideoStatus.UPLOADING,
    @Column
    var duration: Double? = null,
    @Column
    var width: Int? = null,
    @Column
    var height: Int? = null,
    @Column(name = "original_key", nullable = false, columnDefinition = "TEXT")
    var originalKey: String = "",
    @Column(name = "thumbnail_key", columnDefinition = "TEXT")
    var thumbnailKey: String? = null,
    @Column(name = "master_playlist_key", columnDefinition = "TEXT")
    var masterPlaylistKey: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

object VideoStatus {
    const val UPLOADING = "UPLOADING"
    const val UPLOADED = "UPLOADED"
    const val PROCESSING = "PROCESSING"
    const val FAILED = "FAILED"
    const val READY = "READY"
    const val PUBLISHED = "PUBLISHED"
}
