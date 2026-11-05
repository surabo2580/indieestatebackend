package com.indieestate.backend.media

import java.util.UUID

object VideoObjectKeys {
    private val RAW_KEY = Regex("^raw/([0-9a-fA-F-]{36})/original\\.[a-z0-9]+$")

    fun original(videoId: UUID, ext: String): String = "raw/$videoId/original.$ext"

    /** Parses `raw/{videoId}/original.ext` (for S3 event handlers in Phase 2). */
    fun parseVideoIdFromRawKey(objectKey: String): UUID? {
        val match = RAW_KEY.matchEntire(objectKey.trim()) ?: return null
        return runCatching { UUID.fromString(match.groupValues[1]) }.getOrNull()
    }

    fun hlsPrefix(videoId: UUID): String = "media/$videoId/hls"

    fun masterPlaylist(videoId: UUID): String = "media/$videoId/hls/master.m3u8"

    fun renditionPlaylist(videoId: UUID, resolution: String): String =
        "media/$videoId/hls/$resolution/playlist.m3u8"

    fun thumbnail(videoId: UUID): String = "media/$videoId/thumbnail/poster.jpg"
}
