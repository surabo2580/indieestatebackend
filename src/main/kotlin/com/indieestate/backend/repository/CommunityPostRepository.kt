package com.indieestate.backend.repository

import com.indieestate.backend.entity.CommunityPostEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommunityPostRepository : JpaRepository<CommunityPostEntity, UUID> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): List<CommunityPostEntity>

    fun findAllByCategoryOrderByCreatedAtDesc(category: String, pageable: Pageable): List<CommunityPostEntity>
}
