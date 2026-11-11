package com.indieestate.backend.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateFormRequest(
    @field:NotBlank
    val title: String,
    val code: String? = null,
    val type: String? = null,
    val categoryId: UUID? = null,
    val serviceId: UUID? = null,
    @field:Valid
    val fields: List<FormFieldWrite> = emptyList(),
)

data class UpdateFormRequest(
    @field:NotBlank
    val title: String,
    val type: String? = null,
    @field:Valid
    val fields: List<FormFieldWrite> = emptyList(),
)

data class FormFieldWrite(
    @field:NotBlank
    val key: String,
    @field:NotBlank
    val label: String,
    @field:NotBlank
    val type: String,
    val required: Boolean = false,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val options: List<String>? = null,
)

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
