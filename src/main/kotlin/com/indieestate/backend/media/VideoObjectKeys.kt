package com.indieestate.backend.media

import java.util.UUID

object VideoObjectKeys {
    fun original(videoId: UUID, ext: String): String = "raw/$videoId/original.$ext"

    fun hlsPrefix(videoId: UUID): String = "media/$videoId/hls"

    fun masterPlaylist(videoId: UUID): String = "media/$videoId/hls/master.m3u8"

    fun renditionPlaylist(videoId: UUID, resolution: String): String =
        "media/$videoId/hls/$resolution/playlist.m3u8"

    fun thumbnail(videoId: UUID): String = "media/$videoId/thumbnail/poster.jpg"
}
