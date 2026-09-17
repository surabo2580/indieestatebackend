package com.indieestate.backend.media

import com.indieestate.backend.entity.ReelStatus
import com.indieestate.backend.entity.VideoEntity
import com.indieestate.backend.entity.VideoRenditionEntity
import com.indieestate.backend.entity.VideoStatus
import com.indieestate.backend.feed.ReelFeedCache
import com.indieestate.backend.repository.ReelRepository
import com.indieestate.backend.repository.VideoRenditionRepository
import com.indieestate.backend.repository.VideoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MediaJobStore(
    private val videoRepository: VideoRepository,
    private val renditionRepository: VideoRenditionRepository,
    private val reelRepository: ReelRepository,
    private val feedCache: ReelFeedCache,
) {
    @Transactional
    fun loadForProcessing(videoId: UUID, reelId: UUID): VideoEntity? {
        val video = videoRepository.findById(videoId).orElse(null) ?: return null
        if (video.status == VideoStatus.READY) return video
        video.status = VideoStatus.PROCESSING
        videoRepository.save(video)
        markReel(reelId, ReelStatus.PROCESSING, videoUrl = "", thumbnailUrl = null)
        return video
    }

    @Transactional
    fun markReady(
        videoId: UUID,
        reelId: UUID,
        width: Int?,
        height: Int?,
        duration: Double?,
        masterKey: String,
        thumbnailKey: String,
        videoUrl: String,
        thumbnailUrl: String,
        renditions: List<VideoRenditionEntity>,
    ): VideoProcessedEvent {
        val video = videoRepository.findById(videoId).orElseThrow()
        video.status = VideoStatus.READY
        video.width = width
        video.height = height
        video.duration = duration
        video.masterPlaylistKey = masterKey
        video.thumbnailKey = thumbnailKey
        videoRepository.save(video)
        renditionRepository.deleteAllByVideoId(videoId)
        renditions.forEach { renditionRepository.save(it) }
        markReel(reelId, ReelStatus.READY, videoUrl = videoUrl, thumbnailUrl = thumbnailUrl)
        feedCache.evict()
        return VideoProcessedEvent(videoId, reelId, VideoStatus.READY, masterKey)
    }

    @Transactional
    fun markFailed(videoId: UUID, reelId: UUID): VideoProcessedEvent {
        videoRepository.findById(videoId).ifPresent { video ->
            video.status = VideoStatus.FAILED
            videoRepository.save(video)
        }
        markReel(reelId, ReelStatus.FAILED, videoUrl = "", thumbnailUrl = null)
        feedCache.evict()
        return VideoProcessedEvent(videoId, reelId, VideoStatus.FAILED)
    }

    private fun markReel(reelId: UUID, status: String, videoUrl: String, thumbnailUrl: String?) {
        val reel = reelRepository.findById(reelId).orElse(null) ?: return
        reel.status = status
        reel.videoUrl = videoUrl
        if (thumbnailUrl != null) reel.thumbnailUrl = thumbnailUrl
        reelRepository.save(reel)
    }
}
