package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.AdResponse
import com.indieestate.backend.dto.CreateAdRequest
import com.indieestate.backend.entity.AdEntity
import com.indieestate.backend.entity.AdStatus
import com.indieestate.backend.repository.AdRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AdService(
    private val adRepository: AdRepository,
    private val formService: FormService,
    private val categoryService: CategoryService,
    private val catalogServiceService: CatalogServiceService,
    private val formSchemaValidator: FormSchemaValidator,
) {

    @Transactional
    fun create(userId: UUID, request: CreateAdRequest): AdResponse {
        val form = formService.requireActive(request.formId)
        val categoryId = resolveCategory(form.categoryId, request.categoryId)
        val serviceId = resolveService(form.serviceId, request.serviceId)

        if (categoryId != null) {
            categoryService.requireActive(categoryId)
        }
        if (serviceId != null) {
            catalogServiceService.requireActive(serviceId)
        }

        val errors = formSchemaValidator.validate(form.schema, request.data)
        if (errors.isNotEmpty()) {
            throw ApiException(
                status = HttpStatus.BAD_REQUEST,
                message = "Form data does not match the selected form schema",
                errors = errors,
            )
        }

        val now = Instant.now()
        val ad = adRepository.save(
            AdEntity(
                userId = userId,
                formId = form.id,
                categoryId = categoryId,
                serviceId = serviceId,
                title = request.title.trim(),
                status = AdStatus.ACTIVE,
                data = request.data,
                updatedAt = now,
            ),
        )
        return ad.toResponse()
    }

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<AdResponse> =
        adRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }

    private fun resolveCategory(formCategoryId: UUID?, requested: UUID?): UUID? {
        if (requested != null && formCategoryId != null && requested != formCategoryId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "categoryId does not match the selected form")
        }
        if (requested != null && formCategoryId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "This form is not linked to a category")
        }
        return requested ?: formCategoryId
    }

    private fun resolveService(formServiceId: UUID?, requested: UUID?): UUID? {
        if (requested != null && formServiceId != null && requested != formServiceId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "serviceId does not match the selected form")
        }
        if (requested != null && formServiceId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "This form is not linked to a service")
        }
        return requested ?: formServiceId
    }
}

fun AdEntity.toResponse() = AdResponse(
    id = id,
    userId = userId,
    formId = formId,
    categoryId = categoryId,
    serviceId = serviceId,
    title = title,
    status = status,
    data = data,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
