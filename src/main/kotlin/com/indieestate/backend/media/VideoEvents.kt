package com.indieestate.backend.media

import java.util.UUID

data class VideoUploadedEvent(
    val videoId: UUID,
    val reelId: UUID,
    val originalKey: String,
)

data class VideoProcessedEvent(
    val videoId: UUID,
    val reelId: UUID,
    val status: String,
    val masterPlaylistKey: String? = null,
)

data class VideoAnalyticsEvent(
    val videoId: UUID,
    val reelId: UUID? = null,
    val userId: UUID? = null,
    val type: String,
)
