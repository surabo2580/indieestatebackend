package com.indieestate.backend.service

import com.indieestate.backend.dto.FormField
import com.indieestate.backend.dto.FormSchema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FormSchemaValidatorTest {

    private val validator = FormSchemaValidator()

    private val schema = FormSchema(
        fields = listOf(
            FormField(key = "title", label = "Ad title", type = "text", required = true, maxLength = 10),
            FormField(key = "price", label = "Price", type = "number", required = true, min = 0.0),
            FormField(key = "brand", label = "Brand", type = "select", required = true, options = listOf("Honda", "Hyundai")),
            FormField(key = "notes", label = "Notes", type = "textarea", required = false),
        ),
    )

    @Test
    fun `accepts valid payload`() {
        val errors = validator.validate(
            schema,
            mapOf(
                "title" to "Civic",
                "price" to 250000,
                "brand" to "Honda",
            ),
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `reports required and type errors`() {
        val errors = validator.validate(
            schema,
            mapOf(
                "title" to "This title is way too long",
                "price" to "free",
                "brand" to "Tesla",
            ),
        )
        assertEquals("Ad title must be at most 10 characters", errors["title"])
        assertEquals("Price must be a number", errors["price"])
        assertEquals("Brand must be one of: Honda, Hyundai", errors["brand"])
    }
}
