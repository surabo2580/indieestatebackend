package com.indieestate.backend.repository

import com.indieestate.backend.entity.VideoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface VideoRepository : JpaRepository<VideoEntity, UUID> {
    fun findAllByStatusAndCreatedAtBefore(status: String, createdAt: Instant): List<VideoEntity>
}
