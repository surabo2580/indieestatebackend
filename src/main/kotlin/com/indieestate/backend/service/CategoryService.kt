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

    @Transactional
    fun create(request: CreateCategoryRequest): CategoryResponse {
        val slug = (request.slug ?: request.name)
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        if (slug.isBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Category slug cannot be empty")
        }
        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw ApiException(HttpStatus.CONFLICT, "Category slug '$slug' already exists")
        }
        val parentId = request.parentId?.also { requireActive(it) }
        val saved = categoryRepository.save(
            CategoryEntity(
                slug = slug,
                name = request.name.trim(),
                icon = request.icon,
                parentId = parentId,
                sortOrder = request.sortOrder ?: 0,
            ),
        )
        return saved.toResponse()
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
