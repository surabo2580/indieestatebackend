package com.indieestate.backend.dto

data class ReelPageResponse(
    val items: List<ReelResponse>,
    val nextCursor: String?,
)
