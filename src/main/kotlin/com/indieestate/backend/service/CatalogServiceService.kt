package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.CreateServiceRequest
import com.indieestate.backend.dto.ServiceResponse
import com.indieestate.backend.entity.ServiceEntity
import com.indieestate.backend.repository.ServiceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CatalogServiceService(
    private val serviceRepository: ServiceRepository,
) {

    @Transactional(readOnly = true)
    fun listActive(): List<ServiceResponse> {
        val all = serviceRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
        val children = all.filter { it.parentId != null }.groupBy { it.parentId }
        return all.filter { it.parentId == null }.map { parent ->
            parent.toResponse(children[parent.id].orEmpty().map { it.toResponse() })
        }
    }

    @Transactional(readOnly = true)
    fun listSubcategories(parentId: UUID): List<ServiceResponse> {
        requireActive(parentId)
        return serviceRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(parentId)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun serviceScopeIds(serviceId: UUID): List<UUID> {
        requireActive(serviceId)
        val children = serviceRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(serviceId)
        return listOf(serviceId) + children.map { it.id }
    }

    @Transactional
    fun create(request: CreateServiceRequest): ServiceResponse {
        val slug = normalizeSlug(request.slug ?: request.name)
        if (serviceRepository.existsBySlugIgnoreCase(slug)) {
            throw ApiException(HttpStatus.CONFLICT, "Service slug '$slug' already exists")
        }
        val saved = serviceRepository.save(
            ServiceEntity(
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
    fun update(id: UUID, request: CreateServiceRequest): ServiceResponse {
        val entity = requireActive(id)
        val slug = request.slug?.takeIf { it.isNotBlank() }?.let { normalizeSlug(it) } ?: entity.slug
        if (!slug.equals(entity.slug, ignoreCase = true) && serviceRepository.existsBySlugIgnoreCase(slug)) {
            throw ApiException(HttpStatus.CONFLICT, "Service slug '$slug' already exists")
        }
        val parentId = resolveParent(request.parentId, selfId = id)
        if (parentId != null && serviceRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(id).isNotEmpty()) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Move or remove subcategories before nesting this service",
            )
        }
        entity.slug = slug
        entity.name = request.name.trim()
        entity.icon = request.icon?.trim()?.ifBlank { null }
        entity.sortOrder = request.sortOrder ?: entity.sortOrder
        entity.parentId = parentId
        return serviceRepository.save(entity).toResponse()
    }

    @Transactional
    fun deactivate(id: UUID) {
        val entity = requireActive(id)
        val children = serviceRepository.findAllByIsActiveTrueAndParentIdOrderBySortOrderAsc(id)
        children.forEach { it.isActive = false }
        entity.isActive = false
        serviceRepository.saveAll(children + entity)
    }

    @Transactional(readOnly = true)
    fun requireActive(id: UUID): ServiceEntity =
        serviceRepository.findById(id)
            .filter { it.isActive }
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Service not found") }

    private fun resolveParent(parentId: UUID?, selfId: UUID? = null): UUID? {
        if (parentId == null) return null
        if (parentId == selfId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "A service cannot be its own parent")
        }
        val parent = requireActive(parentId)
        if (parent.parentId != null) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Subcategories can only be added under a top-level service",
            )
        }
        return parent.id
    }

    private fun normalizeSlug(raw: String): String {
        val slug = raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        if (slug.isBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Service slug cannot be empty")
        }
        return slug
    }
}

fun ServiceEntity.toResponse(
    subcategories: List<ServiceResponse> = emptyList(),
) = ServiceResponse(
    id = id,
    slug = slug,
    name = name,
    icon = icon,
    sortOrder = sortOrder,
    parentId = parentId,
    subcategories = subcategories,
)
