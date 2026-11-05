package com.indieestate.backend.service

import com.indieestate.backend.dto.ReelsStorageReconcileResponse
import com.indieestate.backend.repository.ReelRepository
import com.indieestate.backend.repository.VideoRepository
import com.indieestate.backend.storage.ReelObjectStorage
import com.indieestate.backend.storage.S3ReelObjectStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReelsStorageReconcileService(
    private val storage: ReelObjectStorage,
    private val reelRepository: ReelRepository,
    private val videoRepository: VideoRepository,
) {
    @Transactional(readOnly = true)
    fun reconcile(): ReelsStorageReconcileResponse {
        val s3MediaIds = when (storage) {
            is S3ReelObjectStorage -> storage.listMediaVideoIds()
            else -> emptyList()
        }
        val reelVideoIdSet = reelRepository.findAllVideoIdsWithReels().toSet()
        val reelVideoIds = reelVideoIdSet.map { it.toString() }.toSet()
        val allVideoIds = videoRepository.findAll().map { it.id.toString() }.toSet()
        val orphanMediaInS3 = s3MediaIds.filter { it !in allVideoIds }.sorted()
        val mediaWithoutReel = s3MediaIds.filter { id ->
            runCatching { UUID.fromString(id) }.getOrNull()?.let { uuid ->
                uuid !in reelVideoIdSet
            } ?: true
        }.sorted()
        val videosWithoutReel = videoRepository.findAll()
            .filter { video -> reelRepository.findFirstByVideoId(video.id) == null }
            .map { it.id.toString() }
            .sorted()
        return ReelsStorageReconcileResponse(
            s3MediaPrefixCount = s3MediaIds.size,
            reelsWithVideoIdCount = reelVideoIds.size,
            orphanMediaPrefixesInS3 = orphanMediaInS3,
            mediaVideoIdsWithoutReelRow = mediaWithoutReel,
            videoRowsWithoutReel = videosWithoutReel,
        )
    }
}
