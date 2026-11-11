package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.CreatePromoBannerRequest
import com.indieestate.backend.dto.PromoBannerResponse
import com.indieestate.backend.entity.PromoBannerEntity
import com.indieestate.backend.repository.PromoBannerRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PromoBannerService(
    private val promoBannerRepository: PromoBannerRepository,
) {

    @Transactional(readOnly = true)
    fun listActive(): List<PromoBannerResponse> =
        promoBannerRepository.findAllByIsActiveTrueOrderBySortOrderAsc().map { it.toResponse() }

    @Transactional
    fun create(request: CreatePromoBannerRequest): PromoBannerResponse {
        val url = request.targetUrl.trim()
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw ApiException(HttpStatus.BAD_REQUEST, "targetUrl must start with http:// or https://")
        }
        val saved = promoBannerRepository.save(
            PromoBannerEntity(
                title = request.title.trim(),
                description = request.description.trim(),
                targetUrl = url,
                sortOrder = request.sortOrder ?: 0,
            ),
        )
        return saved.toResponse()
    }
}

private fun PromoBannerEntity.toResponse() = PromoBannerResponse(
    id = id,
    title = title,
    description = description,
    targetUrl = targetUrl,
    sortOrder = sortOrder,
)
