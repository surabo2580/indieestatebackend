package com.indieestate.backend.dto

import java.util.UUID

data class FormSchema(
    val fields: List<FormField> = emptyList(),
)

data class FormField(
    val key: String = "",
    val label: String = "",
    val type: String = "text",
    val required: Boolean = false,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val options: List<String>? = null,
)

data class FormResponse(
    val id: UUID,
    val code: String,
    val type: String,
    val title: String,
    val categoryId: UUID?,
    val serviceId: UUID?,
    val version: Int,
    val schema: FormSchema,
)
