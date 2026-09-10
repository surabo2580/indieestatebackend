package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
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
    fun requireActive(id: UUID): ServiceEntity =
        serviceRepository.findById(id)
            .filter { it.isActive }
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Service not found") }
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
