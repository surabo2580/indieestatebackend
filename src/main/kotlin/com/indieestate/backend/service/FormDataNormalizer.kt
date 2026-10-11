package com.indieestate.backend.service

import com.indieestate.backend.dto.FormField
import com.indieestate.backend.dto.FormSchema
import org.springframework.stereotype.Component

@Component
class FormDataNormalizer {

    fun normalize(schema: FormSchema, raw: Map<String, Any?>): Map<String, Any?> {
        val byKey = raw.mapKeys { it.key.trim() }
        return buildMap {
            schema.fields.forEach { field ->
                val value = byKey[field.key] ?: byKey.entries.firstOrNull { it.key.equals(field.key, ignoreCase = true) }?.value
                put(field.key, coerce(field, value))
            }
        }
    }

    private fun coerce(field: FormField, value: Any?): Any? {
        if (value == null) return null
        return when (field.type) {
            "multiselect" -> coerceMultiSelect(value)
            "boolean" -> coerceBoolean(value)
            "number" -> coerceNumber(value)
            "date", "text", "textarea", "select" -> value.toString().trim().ifBlank { null }
            else -> value
        }
    }

    private fun coerceMultiSelect(value: Any?): List<String>? = when (value) {
        is Collection<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
        is String -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else -> null
    }

    private fun coerceBoolean(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is String -> when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> null
        }
        is Number -> value.toInt() != 0
        else -> null
    }

    private fun coerceNumber(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }
}
