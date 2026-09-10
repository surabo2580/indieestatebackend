package com.indieestate.backend.controller

import com.indieestate.backend.dto.CategoryResponse
import com.indieestate.backend.dto.CreateCategoryRequest
import com.indieestate.backend.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = ["*"])
@Tag(name = "Categories")
class CategoryController(
    private val categoryService: CategoryService,
) {

    @GetMapping
    @Operation(summary = "List sell/ad categories with subcategories")
    fun list(): List<CategoryResponse> = categoryService.listActive()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category or subcategory")
    fun create(@Valid @RequestBody request: CreateCategoryRequest): CategoryResponse =
        categoryService.create(request)

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "List subcategories for a category")
    fun subcategories(@PathVariable id: UUID): List<CategoryResponse> =
        categoryService.listSubcategories(id)
}
