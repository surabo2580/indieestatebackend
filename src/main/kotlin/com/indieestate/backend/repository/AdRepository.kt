package com.indieestate.backend.repository

import com.indieestate.backend.entity.AdEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdRepository : JpaRepository<AdEntity, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<AdEntity>
}
