package com.indieestate.backend.controller

import org.springframework.http.HttpStatus

class ApiException(
    val status: HttpStatus,
    override val message: String,
    val title: String = status.reasonPhrase,
    val errors: Map<String, String>? = null,
) : RuntimeException(message)
