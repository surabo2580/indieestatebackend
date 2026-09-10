package com.indieestate.backend.repository

import com.indieestate.backend.entity.ServiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceRepository : JpaRepository<ServiceEntity, UUID> {
    fun findAllByIsActiveTrueOrderBySortOrderAsc(): List<ServiceEntity>
    fun findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(parentId: UUID): List<ServiceEntity>
}
