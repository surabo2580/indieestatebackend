package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "video_renditions")
class VideoRenditionEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "video_id", nullable = false)
    var videoId: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var resolution: String = "",
    @Column(nullable = false)
    var width: Int = 0,
    @Column(nullable = false)
    var height: Int = 0,
    @Column(nullable = false)
    var bitrate: Int = 0,
    @Column(nullable = false)
    var codec: String = "avc1.64001f",
    @Column(nullable = false)
    var format: String = "fmp4",
    @Column(name = "playlist_key", nullable = false, columnDefinition = "TEXT")
    var playlistKey: String = "",
    @Column(nullable = false)
    var status: String = VideoStatus.PROCESSING,
)
