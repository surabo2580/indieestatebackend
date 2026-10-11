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
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AdService(
    private val adRepository: AdRepository,
    private val formService: FormService,
    private val categoryService: CategoryService,
    private val catalogServiceService: CatalogServiceService,
    private val formSchemaValidator: FormSchemaValidator,
    private val formDataNormalizer: FormDataNormalizer,
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

        val normalizedData = formDataNormalizer.normalize(form.schema, request.data)
        val errors = formSchemaValidator.validate(form.schema, normalizedData)
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
                data = normalizedData,
                updatedAt = now,
            ),
        )
        return ad.toResponse()
    }

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<AdResponse> =
        adRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }

    @Transactional
    fun markSold(userId: UUID, adId: UUID): AdResponse {
        val ad = requireOwnedAd(userId, adId)
        ad.status = AdStatus.SOLD
        ad.updatedAt = Instant.now()
        return adRepository.save(ad).toResponse()
    }

    @Transactional
    fun republish(userId: UUID, adId: UUID): AdResponse {
        val ad = requireOwnedAd(userId, adId)
        val now = Instant.now()
        ad.status = AdStatus.ACTIVE
        ad.updatedAt = now
        return adRepository.save(ad).toResponse()
    }

    private fun requireOwnedAd(userId: UUID, adId: UUID): AdEntity {
        val ad = adRepository.findById(adId)
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Ad not found") }
        if (ad.userId != userId) {
            throw ApiException(HttpStatus.FORBIDDEN, "This ad belongs to another user")
        }
        return ad
    }

    @Transactional(readOnly = true)
    fun listActive(categoryId: UUID?, serviceId: UUID?): List<AdResponse> {
        val ads = when {
            categoryId != null -> {
                val scope = categoryService.categoryScopeIds(categoryId)
                adRepository.findAllByStatusAndCategoryIdInOrderByCreatedAtDesc(AdStatus.ACTIVE, scope)
            }
            serviceId != null -> {
                val scope = catalogServiceService.serviceScopeIds(serviceId)
                adRepository.findAllByStatusAndServiceIdInOrderByCreatedAtDesc(AdStatus.ACTIVE, scope)
            }
            else -> emptyList()
        }
        val now = Instant.now()
        return ads
            .filter { now.isBefore(it.updatedAt.plus(AdStatus.LISTING_DAYS, ChronoUnit.DAYS)) }
            .map { it.toResponse() }
    }

    private fun resolveCategory(formCategoryId: UUID?, requested: UUID?): UUID? {
        if (requested != null && formCategoryId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "This form is not linked to a category")
        }
        if (requested == null) return formCategoryId
        if (formCategoryId == null) return requested
        if (requested == formCategoryId || isCategoryUnderAncestor(requested, formCategoryId)) {
            return requested
        }
        throw ApiException(HttpStatus.BAD_REQUEST, "categoryId does not match the selected form")
    }

    private fun resolveService(formServiceId: UUID?, requested: UUID?): UUID? {
        if (requested != null && formServiceId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "This form is not linked to a service")
        }
        if (requested == null) return formServiceId
        if (formServiceId == null) return requested
        if (requested == formServiceId || isServiceUnderAncestor(requested, formServiceId)) {
            return requested
        }
        throw ApiException(HttpStatus.BAD_REQUEST, "serviceId does not match the selected form")
    }

    private fun isCategoryUnderAncestor(categoryId: UUID, ancestorId: UUID): Boolean {
        var current = categoryService.requireActive(categoryId)
        while (current.parentId != null) {
            if (current.parentId == ancestorId) return true
            current = categoryService.requireActive(current.parentId!!)
        }
        return false
    }

    private fun isServiceUnderAncestor(serviceId: UUID, ancestorId: UUID): Boolean {
        var current = catalogServiceService.requireActive(serviceId)
        while (current.parentId != null) {
            if (current.parentId == ancestorId) return true
            current = catalogServiceService.requireActive(current.parentId!!)
        }
        return false
    }
}

fun AdEntity.toResponse(): AdResponse {
    val listingStart = updatedAt
    val expiresAt = listingStart.plus(AdStatus.LISTING_DAYS, ChronoUnit.DAYS)
    val displayStatus = when {
        status == AdStatus.SOLD -> AdStatus.SOLD
        Instant.now().isAfter(expiresAt) -> "EXPIRED"
        else -> status
    }
    return AdResponse(
        id = id,
        userId = userId,
        formId = formId,
        categoryId = categoryId,
        serviceId = serviceId,
        title = title,
        status = displayStatus,
        data = data,
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAt = expiresAt,
    )
}
