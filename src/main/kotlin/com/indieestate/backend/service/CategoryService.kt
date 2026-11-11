package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.CategoryResponse
import com.indieestate.backend.dto.CreateCategoryRequest
import com.indieestate.backend.entity.CategoryEntity
import com.indieestate.backend.repository.CategoryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun listActive(): List<CategoryResponse> {
        val all = categoryRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
        val children = all.filter { it.parentId != null }.groupBy { it.parentId }
        return all.filter { it.parentId == null }.map { parent ->
            parent.toResponse(children[parent.id].orEmpty().map { it.toResponse() })
        }
    }

    @Transactional(readOnly = true)
    fun listSubcategories(parentId: UUID): List<CategoryResponse> {
        requireActive(parentId)
        return categoryRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(parentId)
            .map { it.toResponse() }
    }

    /** Category id plus direct child category ids (for listing ads on parent and leaf pages). */
    @Transactional(readOnly = true)
    fun categoryScopeIds(categoryId: UUID): List<UUID> {
        requireActive(categoryId)
        val children = categoryRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(categoryId)
        return listOf(categoryId) + children.map { it.id }
    }

    @Transactional
    fun create(request: CreateCategoryRequest): CategoryResponse {
        val slug = normalizeSlug(request.slug ?: request.name)
        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw ApiException(HttpStatus.CONFLICT, "Category slug '$slug' already exists")
        }
        val saved = categoryRepository.save(
            CategoryEntity(
                slug = slug,
                name = request.name.trim(),
                icon = request.icon?.trim()?.ifBlank { null },
                parentId = resolveParent(request.parentId),
                sortOrder = request.sortOrder ?: 0,
            ),
        )
        return saved.toResponse()
    }

    @Transactional
    fun update(id: UUID, request: CreateCategoryRequest): CategoryResponse {
        val entity = requireActive(id)
        val slug = request.slug?.takeIf { it.isNotBlank() }?.let { normalizeSlug(it) } ?: entity.slug
        if (!slug.equals(entity.slug, ignoreCase = true) && categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw ApiException(HttpStatus.CONFLICT, "Category slug '$slug' already exists")
        }
        val parentId = resolveParent(request.parentId, selfId = id)
        if (parentId != null && categoryRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(id).isNotEmpty()) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Move or remove subcategories before nesting this category",
            )
        }
        entity.slug = slug
        entity.name = request.name.trim()
        entity.icon = request.icon?.trim()?.ifBlank { null }
        entity.sortOrder = request.sortOrder ?: entity.sortOrder
        entity.parentId = parentId
        return categoryRepository.save(entity).toResponse()
    }

    @Transactional
    fun deactivate(id: UUID) {
        val entity = requireActive(id)
        val children = categoryRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(id)
        children.forEach { it.isActive = false }
        entity.isActive = false
        categoryRepository.saveAll(children + entity)
    }

    private fun resolveParent(parentId: UUID?, selfId: UUID? = null): UUID? {
        if (parentId == null) return null
        if (parentId == selfId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "A category cannot be its own parent")
        }
        val parent = requireActive(parentId)
        if (parent.parentId != null) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Subcategories can only be added under a top-level category",
            )
        }
        return parent.id
    }

    private fun normalizeSlug(raw: String): String {
        val slug = raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        if (slug.isBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Category slug cannot be empty")
        }
        return slug
    }

    @Transactional(readOnly = true)
    fun requireActive(id: UUID): CategoryEntity =
        categoryRepository.findById(id)
            .filter { it.isActive }
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Category not found") }
}

fun CategoryEntity.toResponse(
    subcategories: List<CategoryResponse> = emptyList(),
) = CategoryResponse(
    id = id,
    slug = slug,
    name = name,
    icon = icon,
    sortOrder = sortOrder,
    parentId = parentId,
    subcategories = subcategories,
)
