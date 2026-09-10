package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateCategoryRequest(
    @field:NotBlank
    val name: String,
    val slug: String? = null,
    val icon: String? = null,
    val sortOrder: Int? = null,
    val parentId: UUID? = null,
)

data class CategoryResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val icon: String?,
    val sortOrder: Int,
    val parentId: UUID? = null,
    val subcategories: List<CategoryResponse> = emptyList(),
)
