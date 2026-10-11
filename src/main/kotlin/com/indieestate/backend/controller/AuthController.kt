package com.indieestate.backend.controller

import com.indieestate.backend.dto.AuthResponse
import com.indieestate.backend.dto.LoginRequest
import com.indieestate.backend.dto.OtpSendRequest
import com.indieestate.backend.dto.OtpSendResponse
import com.indieestate.backend.dto.OtpVerifyRequest
import com.indieestate.backend.dto.RegisterRequest
import com.indieestate.backend.service.OtpAuthService
import com.indieestate.backend.dto.UserResponse
import com.indieestate.backend.security.UserPrincipal
import com.indieestate.backend.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
class AuthController(
    private val authService: AuthService,
    private val otpAuthService: OtpAuthService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @PostMapping("/otp/send")
    @Operation(summary = "Send login OTP to a mobile number")
    fun sendOtp(
        @Valid @RequestBody request: OtpSendRequest,
        servletRequest: HttpServletRequest,
    ): OtpSendResponse =
        otpAuthService.sendOtp(request.phone, clientIp(servletRequest))

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP and receive access token")
    fun verifyOtp(@Valid @RequestBody request: OtpVerifyRequest): AuthResponse =
        otpAuthService.verifyOtp(request.phone, request.otp)

    @GetMapping("/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Current user profile")
    fun me(@AuthenticationPrincipal principal: UserPrincipal): UserResponse =
        authService.me(principal.userId)

    private fun clientIp(request: HttpServletRequest): String? =
        request.getHeader("X-Forwarded-For")
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr
}
