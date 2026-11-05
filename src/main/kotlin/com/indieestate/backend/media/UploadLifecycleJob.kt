package com.indieestate.backend.media

import com.indieestate.backend.entity.VideoStatus
import com.indieestate.backend.repository.VideoRepository
import com.indieestate.backend.storage.ReelObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
@ConditionalOnProperty(name = ["app.reels.run-media-worker"], havingValue = "false", matchIfMissing = true)
class UploadLifecycleJob(
    private val videoRepository: VideoRepository,
    private val storage: ReelObjectStorage,
    @param:Value("\${app.reels.upload-stale-hours:24}") private val staleHours: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.reels.upload-cleanup-interval-ms:3600000}")
    fun expireStaleUploads() {
        val cutoff = Instant.now().minus(staleHours, ChronoUnit.HOURS)
        val stale = videoRepository.findAllByStatusAndCreatedAtBefore(VideoStatus.UPLOADING, cutoff)
        if (stale.isEmpty()) return
        stale.forEach { video ->
            video.status = VideoStatus.EXPIRED
            videoRepository.save(video)
            runCatching {
                if (video.originalKey.isNotBlank() && storage.exists(video.originalKey)) {
                    storage.delete(video.originalKey)
                }
            }.onFailure { error ->
                log.warn("Could not delete stale raw object {}", video.originalKey, error)
            }
        }
        log.info("Marked {} stale UPLOADING videos as EXPIRED (older than {}h)", stale.size, staleHours)
    }
}
