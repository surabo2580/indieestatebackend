package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.FormResponse
import com.indieestate.backend.entity.FormEntity
import com.indieestate.backend.repository.FormRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository,
    private val categoryService: CategoryService,
    private val catalogServiceService: CatalogServiceService,
) {

    @Transactional(readOnly = true)
    fun list(categoryId: UUID?, serviceId: UUID?, code: String?): List<FormResponse> {
        val forms = when {
            !code.isNullOrBlank() -> listOfNotNull(formRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim()))
            categoryId != null -> {
                val ids = categoryLookupIds(categoryId)
                formRepository.findAllByCategoryIdInAndIsActiveTrue(ids)
                    .sortedWith(
                        compareByDescending<FormEntity> { it.categoryId == categoryId }
                            .thenBy { it.code },
                    )
            }
            serviceId != null -> {
                val ids = serviceLookupIds(serviceId)
                formRepository.findAllByServiceIdInAndIsActiveTrue(ids)
                    .sortedWith(
                        compareByDescending<FormEntity> { it.serviceId == serviceId }
                            .thenBy { it.code },
                    )
            }
            else -> formRepository.findAllByIsActiveTrueOrderByCodeAsc()
        }
        return forms.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun requireActive(id: UUID): FormEntity =
        formRepository.findById(id)
            .filter { it.isActive }
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Form not found") }

    private fun categoryLookupIds(id: UUID): List<UUID> {
        val category = categoryService.requireActive(id)
        return listOfNotNull(category.id, category.parentId)
    }

    private fun serviceLookupIds(id: UUID): List<UUID> {
        val service = catalogServiceService.requireActive(id)
        return listOfNotNull(service.id, service.parentId)
    }
}

fun FormEntity.toResponse() = FormResponse(
    id = id,
    code = code,
    type = type,
    title = title,
    categoryId = categoryId,
    serviceId = serviceId,
    version = version,
    schema = schema,
)
