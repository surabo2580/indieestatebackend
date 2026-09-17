package com.indieestate.backend.repository

import com.indieestate.backend.entity.VideoRenditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VideoRenditionRepository : JpaRepository<VideoRenditionEntity, UUID> {
    fun deleteAllByVideoId(videoId: UUID)
    fun findAllByVideoId(videoId: UUID): List<VideoRenditionEntity>
}
