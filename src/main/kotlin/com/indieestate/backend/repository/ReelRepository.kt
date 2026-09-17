package com.indieestate.backend.repository

import com.indieestate.backend.entity.ReelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReelRepository : JpaRepository<ReelEntity, UUID> {
    fun findAllByIsActiveTrueAndStatusOrderByCreatedAtDesc(status: String): List<ReelEntity>
    fun findFirstByVideoId(videoId: UUID): ReelEntity?
}
