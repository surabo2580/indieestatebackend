package com.indieestate.backend.repository

import com.indieestate.backend.entity.FormEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FormRepository : JpaRepository<FormEntity, UUID> {
    fun findAllByIsActiveTrueOrderByCodeAsc(): List<FormEntity>
    fun findByCodeIgnoreCaseAndIsActiveTrue(code: String): FormEntity?
    fun findAllByCategoryIdAndIsActiveTrue(categoryId: UUID): List<FormEntity>
    fun findAllByCategoryIdInAndIsActiveTrue(categoryIds: Collection<UUID>): List<FormEntity>
    fun findAllByServiceIdAndIsActiveTrue(serviceId: UUID): List<FormEntity>
    fun findAllByServiceIdInAndIsActiveTrue(serviceIds: Collection<UUID>): List<FormEntity>
}
