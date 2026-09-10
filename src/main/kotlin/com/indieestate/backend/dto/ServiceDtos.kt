package com.indieestate.backend.dto

import java.util.UUID

data class ServiceResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val icon: String?,
    val sortOrder: Int,
    val parentId: UUID? = null,
    val subcategories: List<ServiceResponse> = emptyList(),
)
