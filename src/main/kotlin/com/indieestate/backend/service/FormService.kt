package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.CreateFormRequest
import com.indieestate.backend.dto.FormField
import com.indieestate.backend.dto.FormFieldWrite
import com.indieestate.backend.dto.FormResponse
import com.indieestate.backend.dto.FormSchema
import com.indieestate.backend.dto.UpdateFormRequest
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

    @Transactional
    fun create(request: CreateFormRequest): FormResponse {
        val (categoryId, serviceId) = resolveOwner(request.categoryId, request.serviceId)
        val type = resolveType(request.type, serviceId != null)
        val saved = formRepository.save(
            FormEntity(
                code = normalizeCode(request.code ?: request.title),
                type = type,
                categoryId = categoryId,
                serviceId = serviceId,
                title = request.title.trim(),
                schema = FormSchema(normalizeFields(request.fields)),
            ),
        )
        return saved.toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateFormRequest): FormResponse {
        val entity = requireActive(id)
        entity.title = request.title.trim()
        entity.type = resolveType(request.type, entity.serviceId != null)
        entity.schema = FormSchema(normalizeFields(request.fields))
        entity.version += 1
        return formRepository.save(entity).toResponse()
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

    private fun resolveOwner(categoryId: UUID?, serviceId: UUID?): Pair<UUID?, UUID?> {
        if ((categoryId == null) == (serviceId == null)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Attach the form to one category or one service")
        }
        if (categoryId != null) categoryService.requireActive(categoryId)
        if (serviceId != null) catalogServiceService.requireActive(serviceId)
        return categoryId to serviceId
    }

    private fun resolveType(requested: String?, forService: Boolean): String {
        val type = (requested?.takeIf { it.isNotBlank() } ?: if (forService) "SERVICE_REQUEST" else "SELL")
            .trim()
            .uppercase()
        if (type !in setOf("JOB", "SELL", "SERVICE_REQUEST")) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Form type must be JOB, SELL, or SERVICE_REQUEST")
        }
        return type
    }

    private fun normalizeCode(raw: String): String {
        val code = raw.trim().uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').take(64)
        if (code.isBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Form code cannot be empty")
        }
        if (formRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException(HttpStatus.CONFLICT, "Form code '$code' already exists")
        }
        return code
    }

    private fun normalizeFields(fields: List<FormFieldWrite>): List<FormField> {
        if (fields.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Add at least one form field")
        }
        val seen = mutableSetOf<String>()
        return fields.map { field ->
            val key = field.key.trim()
            if (!key.matches(Regex("[A-Za-z][A-Za-z0-9]*"))) {
                throw ApiException(HttpStatus.BAD_REQUEST, "Field key '$key' must start with a letter and use only letters and numbers")
            }
            if (!seen.add(key)) {
                throw ApiException(HttpStatus.BAD_REQUEST, "Field key '$key' is duplicated")
            }
            val type = field.type.trim().lowercase()
            if (type !in setOf("text", "textarea", "number", "select", "multiselect", "boolean", "date")) {
                throw ApiException(HttpStatus.BAD_REQUEST, "Field '$key' has unsupported type '$type'")
            }
            val options = field.options?.map { it.trim() }?.filter { it.isNotEmpty() }
            if (type == "select" || type == "multiselect") {
                if (options.isNullOrEmpty()) {
                    throw ApiException(HttpStatus.BAD_REQUEST, "Field '$key' needs at least one option")
                }
            }
            val maxLength = field.maxLength
            if (maxLength != null && maxLength < 1) {
                throw ApiException(HttpStatus.BAD_REQUEST, "Field '$key' max length must be at least 1")
            }
            if (field.min != null && field.max != null && field.min > field.max) {
                throw ApiException(HttpStatus.BAD_REQUEST, "Field '$key' minimum is greater than its maximum")
            }
            FormField(
                key = key,
                label = field.label.trim(),
                type = type,
                required = field.required,
                maxLength = maxLength,
                min = field.min,
                max = field.max,
                options = if (type == "select" || type == "multiselect") options else null,
            )
        }
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
