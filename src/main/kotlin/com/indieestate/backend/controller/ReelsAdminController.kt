package com.indieestate.backend.controller

import com.indieestate.backend.dto.ReelsStorageReconcileResponse
import com.indieestate.backend.service.ReelsStorageReconcileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/reels")
@Tag(name = "Reels admin")
class ReelsAdminController(
    private val reconcileService: ReelsStorageReconcileService,
    @param:Value("\${app.reels.admin-reconcile-token:}") private val adminToken: String,
) {
    @GetMapping("/storage-reconcile")
    @Operation(summary = "Compare S3 media/ prefixes with Postgres (requires X-Reels-Admin-Token)")
    fun storageReconcile(
        @RequestHeader("X-Reels-Admin-Token", required = false) token: String?,
    ): ReelsStorageReconcileResponse {
        if (adminToken.isBlank() || token != adminToken) {
            throw ApiException(HttpStatus.FORBIDDEN, "Invalid or missing admin token")
        }
        return reconcileService.reconcile()
    }
}
