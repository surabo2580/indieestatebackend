package com.indieestate.backend.repository

import com.indieestate.backend.entity.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryRepository : JpaRepository<CategoryEntity, UUID> {
    fun findAllByIsActiveTrueOrderBySortOrderAsc(): List<CategoryEntity>
    fun findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(parentId: UUID): List<CategoryEntity>
    fun existsBySlugIgnoreCase(slug: String): Boolean
}
