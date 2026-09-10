package com.indieestate.backend.service

import com.indieestate.backend.dto.FormField
import com.indieestate.backend.dto.FormSchema
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Component
class FormSchemaValidator {

    fun validate(schema: FormSchema, data: Map<String, Any?>): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        for (field in schema.fields) {
            val value = data[field.key]
            if (isMissing(value)) {
                if (field.required) {
                    errors[field.key] = "${field.label} is required"
                }
                continue
            }
            when (field.type) {
                "text", "textarea" -> validateText(field, value, errors)
                "number" -> validateNumber(field, value, errors)
                "select" -> validateSelect(field, value, errors)
                "multiselect" -> validateMultiSelect(field, value, errors)
                "boolean" -> validateBoolean(field, value, errors)
                "date" -> validateDate(field, value, errors)
                else -> errors[field.key] = "${field.label} has unsupported type '${field.type}'"
            }
        }
        return errors
    }

    private fun isMissing(value: Any?): Boolean =
        value == null || (value is String && value.isBlank())

    private fun validateText(field: FormField, value: Any?, errors: MutableMap<String, String>) {
        if (value !is String) {
            errors[field.key] = "${field.label} must be text"
            return
        }
        val maxLength = field.maxLength
        if (maxLength != null && value.length > maxLength) {
            errors[field.key] = "${field.label} must be at most $maxLength characters"
        }
    }

    private fun validateNumber(field: FormField, value: Any?, errors: MutableMap<String, String>) {
        val number = toDouble(value)
        if (number == null) {
            errors[field.key] = "${field.label} must be a number"
            return
        }
        field.min?.let { min ->
            if (number < min) errors[field.key] = "${field.label} must be at least $min"
        }
        field.max?.let { max ->
            if (number > max) errors[field.key] = "${field.label} must be at most $max"
        }
    }

    private fun validateSelect(field: FormField, value: Any?, errors: MutableMap<String, String>) {
        if (value !is String) {
            errors[field.key] = "${field.label} must be a string"
            return
        }
        val options = field.options.orEmpty()
        if (options.isNotEmpty() && value !in options) {
            errors[field.key] = "${field.label} must be one of: ${options.joinToString()}"
        }
    }

    private fun validateMultiSelect(field: FormField, value: Any?, errors: MutableMap<String, String>) {
        val selected = when (value) {
            is Collection<*> -> value.mapNotNull { it?.toString() }
            else -> {
                errors[field.key] = "${field.label} must be a list"
                return
            }
        }
        if (field.required && selected.isEmpty()) {
            errors[field.key] = "${field.label} is required"
            return
        }
        val options = field.options.orEmpty()
        if (options.isNotEmpty()) {
            val unknown = selected.filter { it !in options }
            if (unknown.isNotEmpty()) {
                errors[field.key] = "${field.label} contains invalid values: ${unknown.joinToString()}"
            }
        }
    }

    private fun validateBoolean(field: FormField, value: Any?, errors: MutableMap<String, String>) {
        if (value !is Boolean) {
            errors[field.key] = "${field.label} must be true or false"
        }
    }

    private fun validateDate(field: FormField, value: Any?, errors: MutableMap<String, String>) {
        if (value !is String) {
            errors[field.key] = "${field.label} must be a date (YYYY-MM-DD)"
            return
        }
        try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            errors[field.key] = "${field.label} must be a date (YYYY-MM-DD)"
        }
    }

    private fun toDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}
