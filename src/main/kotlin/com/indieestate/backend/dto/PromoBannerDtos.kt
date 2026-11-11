package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreatePromoBannerRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val title: String,
    @field:NotBlank
    @field:Size(max = 280)
    val description: String,
    @field:NotBlank
    @field:Size(max = 1000)
    val targetUrl: String,
    val sortOrder: Int? = null,
)

data class PromoBannerResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val targetUrl: String,
    val sortOrder: Int,
)
