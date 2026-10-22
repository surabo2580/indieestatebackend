package com.indieestate.backend.storage

data class PresignedUpload(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
