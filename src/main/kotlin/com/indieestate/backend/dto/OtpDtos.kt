package com.indieestate.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class OtpSendRequest(
    @field:NotBlank(message = "Mobile number is required")
    @field:Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter a valid mobile number")
    val phone: String,
)

data class OtpSendResponse(
    val message: String,
    /** Present only in dev mode to simplify local testing. */
    val debugOtp: String? = null,
)

data class OtpVerifyRequest(
    @field:NotBlank(message = "Mobile number is required")
    @field:Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter a valid mobile number")
    val phone: String,
    @field:NotBlank(message = "OTP is required")
    @field:Pattern(regexp = "^[0-9]{4,8}$", message = "Enter the OTP code")
    val otp: String,
)
