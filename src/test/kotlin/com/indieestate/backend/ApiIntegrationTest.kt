package com.indieestate.backend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ApiIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun healthIsUp() {
        mockMvc.get("/api/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
                jsonPath("$.service") { value("indieestate-backend") }
            }
    }

    @Test
    fun catalogsAndFormAreSeeded() {
        mockMvc.get("/api/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(4) }
                jsonPath("$[0].slug") { value("jobs") }
                jsonPath("$[0].subcategories.length()") { value(4) }
                jsonPath("$[0].subcategories[0].slug") { value("data-entry") }
                jsonPath("$[1].slug") { value("cars") }
            }

        mockMvc.get("/api/services")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(4) }
                jsonPath("$[0].slug") { value("salon") }
                jsonPath("$[0].subcategories[0].slug") { value("salon-men") }
                jsonPath("$[1].slug") { value("toilet") }
            }

        val categories = objectMapper.readTree(
            mockMvc.get("/api/categories").andReturn().response.contentAsString,
        )
        val carsId = categories.first { it["slug"].asText() == "cars" }["id"].asText()

        mockMvc.get("/api/forms") { param("categoryId", carsId) }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].code") { value("CAR_SELL") }
                jsonPath("$[0].schema.fields[0].key") { value("title") }
            }
    }

    @Test
    fun registerLoginAndPostAd() {
        val registerJson = postJson(
            "/api/auth/register",
            """{"name":"Suraj","email":"suraj@example.com","password":"password1"}""",
            expectedStatus = 201,
        )
        val token = registerJson["accessToken"].asText()

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"suraj@example.com","password":"password1"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.user.email") { value("suraj@example.com") }
        }

        mockMvc.get("/api/auth/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("suraj@example.com") }
        }

        mockMvc.post("/api/ads") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"formId":"0a1c0003-0000-4000-8000-000000000002","title":"Honda City","data":{}}"""
        }.andExpect {
            status { isUnauthorized() }
        }

        val adBody = """
            {
              "formId": "0a1c0003-0000-4000-8000-000000000002",
              "title": "Honda City 2018",
              "data": {
                "title": "Honda City 2018",
                "price": 650000,
                "brand": "Honda",
                "model": "City",
                "year": 2018,
                "kmDriven": 42000,
                "fuel": "Petrol",
                "description": "Single owner"
              }
            }
        """.trimIndent()

        mockMvc.post("/api/ads") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = adBody
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("Honda City 2018") }
            jsonPath("$.status") { value("ACTIVE") }
            jsonPath("$.data.brand") { value("Honda") }
        }

        mockMvc.get("/api/ads/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }
    }

    @Test
    fun duplicateEmailConflicts() {
        val body = """{"name":"Asha","email":"asha@example.com","password":"password1"}"""
        postJson("/api/auth/register", body, expectedStatus = 201)
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isConflict() }
        }
    }

    private fun postJson(path: String, body: String, expectedStatus: Int): JsonNode {
        val result = mockMvc.post(path) {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isEqualTo(expectedStatus) }
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)
    }
}
