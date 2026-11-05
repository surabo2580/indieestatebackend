package com.indieestate.backend.media

import com.indieestate.backend.entity.VideoRenditionEntity
import com.indieestate.backend.entity.VideoStatus
import com.indieestate.backend.media.transcode.VideoTranscodeService
import com.indieestate.backend.storage.FakeReelObjectStorage
import com.indieestate.backend.storage.ReelObjectStorage
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

@Component
class MediaWorker(
    private val jobs: MediaJobStore,
    private val storage: ReelObjectStorage,
    private val transcodeService: VideoTranscodeService,
    meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transcodeTimer: Timer = Timer.builder("reels.transcode.duration")
        .description("ffmpeg transcode duration")
        .register(meterRegistry)

    fun processUploaded(event: VideoUploadedEvent): VideoProcessedEvent {
        val video = jobs.loadForProcessing(event.videoId, event.reelId)
            ?: return VideoProcessedEvent(event.videoId, event.reelId, VideoStatus.FAILED)
        if (video.status == VideoStatus.READY && video.masterPlaylistKey != null) {
            return VideoProcessedEvent(video.id, event.reelId, VideoStatus.READY, video.masterPlaylistKey)
        }
        if (storage is FakeReelObjectStorage) {
            val masterKey = VideoObjectKeys.masterPlaylist(video.id)
            val thumbnailKey = VideoObjectKeys.thumbnail(video.id)
            return jobs.markReady(
                videoId = video.id,
                reelId = event.reelId,
                width = 1280,
                height = 720,
                duration = 1.0,
                masterKey = masterKey,
                thumbnailKey = thumbnailKey,
                videoUrl = storage.publicUrl(masterKey),
                thumbnailUrl = storage.publicUrl(thumbnailKey),
                renditions = listOf(rendition(video.id, "720p", 1280, 720, 5_000_000)),
            )
        }

        val workDir = Files.createTempDirectory("video-${video.id}")
        try {
            val input = workDir.resolve("original")
            storage.download(event.originalKey, input)
            val result = transcodeTimer.recordCallable {
                transcodeService.transcodeLocal(input, workDir, video.id)
            } ?: throw IllegalStateException("Transcode returned null")
            uploadTree(result.hlsRoot, VideoObjectKeys.hlsPrefix(video.id))
            val thumbnailKey = VideoObjectKeys.thumbnail(video.id)
            storage.upload(thumbnailKey, result.poster, "image/jpeg")
            val masterKey = VideoObjectKeys.masterPlaylist(video.id)
            return jobs.markReady(
                videoId = video.id,
                reelId = event.reelId,
                width = result.source.width,
                height = result.source.height,
                duration = result.source.duration,
                masterKey = masterKey,
                thumbnailKey = thumbnailKey,
                videoUrl = storage.publicUrl(masterKey),
                thumbnailUrl = storage.publicUrl(thumbnailKey),
                renditions = result.renditions.map { spec ->
                    rendition(
                        videoId = video.id,
                        resolution = spec.name,
                        width = spec.width,
                        height = spec.height,
                        bitrate = spec.bandwidth,
                    )
                },
            )
        } catch (error: Exception) {
            log.error("Transcode failed for video {} reel {}", video.id, event.reelId, error)
            return jobs.markFailed(video.id, event.reelId)
        } finally {
            workDir.toFile().deleteRecursively()
        }
    }

    private fun rendition(
        videoId: UUID,
        resolution: String,
        width: Int,
        height: Int,
        bitrate: Int,
    ) = VideoRenditionEntity(
        videoId = videoId,
        resolution = resolution,
        width = width,
        height = height,
        bitrate = bitrate,
        codec = "avc1.64001f",
        format = "fmp4",
        playlistKey = VideoObjectKeys.renditionPlaylist(videoId, resolution),
        status = VideoStatus.READY,
    )

    private fun uploadTree(root: java.nio.file.Path, prefix: String) {
        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() }.forEach { file ->
                val relative = root.relativize(file).toString().replace('\\', '/')
                storage.upload("$prefix/$relative", file, contentTypeFor(file))
            }
        }
    }

    private fun contentTypeFor(file: java.nio.file.Path): String = when (file.extension.lowercase()) {
        "m3u8" -> "application/vnd.apple.mpegurl"
        "m4s" -> "video/iso.segment"
        "mp4" -> "video/mp4"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }
}
