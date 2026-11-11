package com.indieestate.backend.repository

import com.indieestate.backend.entity.PromoBannerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PromoBannerRepository : JpaRepository<PromoBannerEntity, UUID> {
    fun findAllByIsActiveTrueOrderBySortOrderAsc(): List<PromoBannerEntity>
}
