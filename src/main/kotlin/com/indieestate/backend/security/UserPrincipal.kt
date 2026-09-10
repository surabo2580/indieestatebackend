package com.indieestate.backend.security

import java.util.UUID

data class UserPrincipal(
    val userId: UUID,
    val email: String,
)
