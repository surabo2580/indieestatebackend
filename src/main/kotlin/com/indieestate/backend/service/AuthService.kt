package com.indieestate.backend.service

import com.indieestate.backend.dto.AuthResponse
import com.indieestate.backend.dto.LoginRequest
import com.indieestate.backend.dto.RegisterRequest
import com.indieestate.backend.dto.UserResponse
import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.entity.UserEntity
import com.indieestate.backend.repository.UserRepository
import com.indieestate.backend.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException(HttpStatus.CONFLICT, "An account with this email already exists")
        }
        val user = userRepository.save(
            UserEntity(
                email = email,
                passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
                name = request.name.trim(),
                phone = request.phone?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return toAuthResponse(user)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmailIgnoreCase(request.email.trim())
            ?: throw invalidCredentials()
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw invalidCredentials()
        }
        return toAuthResponse(user)
    }

    @Transactional(readOnly = true)
    fun me(userId: UUID): UserResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "Authentication required")
        }
        return user.toResponse()
    }

    private fun toAuthResponse(user: UserEntity) = AuthResponse(
        accessToken = jwtService.createToken(user.id, user.email),
        expiresInSeconds = jwtService.ttlSeconds(),
        user = user.toResponse(),
    )

    private fun invalidCredentials() =
        ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
}

fun UserEntity.toResponse() = UserResponse(
    id = id,
    name = name,
    email = email,
    phone = phone,
    createdAt = createdAt,
)
