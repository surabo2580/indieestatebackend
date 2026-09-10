package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateAdRequest(
    @field:NotNull
    val formId: UUID,
    val categoryId: UUID? = null,
    val serviceId: UUID? = null,
    @field:NotBlank
    val title: String,
    @field:NotNull
    val data: Map<String, Any?> = emptyMap(),
)

data class AdResponse(
    val id: UUID,
    val userId: UUID,
    val formId: UUID,
    val categoryId: UUID?,
    val serviceId: UUID?,
    val title: String,
    val status: String,
    val data: Map<String, Any?>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
