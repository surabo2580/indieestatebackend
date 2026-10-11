package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.AuthResponse
import com.indieestate.backend.dto.OtpSendResponse
import com.indieestate.backend.entity.UserEntity
import com.indieestate.backend.repository.UserRepository
import com.indieestate.backend.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class OtpAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @param:Value("\${auth.otp.ttl-seconds:300}") private val otpTtlSeconds: Long,
    @param:Value("\${auth.otp.length:6}") private val otpLength: Int,
    @param:Value("\${auth.otp.dev-mode:true}") private val devMode: Boolean,
    @param:Value("\${auth.otp.dev-fixed-code:}") private val devFixedCode: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val random = SecureRandom()
    private val pendingOtps = ConcurrentHashMap<String, PendingOtp>()

    fun sendOtp(rawPhone: String): OtpSendResponse {
        val phone = normalizePhone(rawPhone)
        val code = when {
            devMode && devFixedCode.isNotBlank() -> devFixedCode.padStart(otpLength, '0').take(otpLength)
            else -> randomNumericCode(otpLength)
        }
        pendingOtps[phone] = PendingOtp(code = code, expiresAt = Instant.now().plusSeconds(otpTtlSeconds))
        log.info("OTP for {} expires in {}s", maskPhone(phone), otpTtlSeconds)
        if (devMode) {
            log.info("DEV OTP for {}: {}", maskPhone(phone), code)
        }
        return OtpSendResponse(
            message = "OTP sent to ${maskPhone(phone)}",
            debugOtp = if (devMode) code else null,
        )
    }

    @Transactional
    fun verifyOtp(rawPhone: String, otp: String): AuthResponse {
        val phone = normalizePhone(rawPhone)
        val pending = pendingOtps[phone]
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Request a new OTP first")
        if (Instant.now().isAfter(pending.expiresAt)) {
            pendingOtps.remove(phone)
            throw ApiException(HttpStatus.BAD_REQUEST, "OTP expired. Request a new one.")
        }
        if (pending.code != otp.trim()) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Invalid OTP")
        }
        pendingOtps.remove(phone)

        val user = userRepository.findByPhone(phone) ?: userRepository.save(newPhoneUser(phone))
        return AuthResponse(
            accessToken = jwtService.createToken(user.id, user.email),
            expiresInSeconds = jwtService.ttlSeconds(),
            user = user.toResponse(),
        )
    }

    private fun newPhoneUser(phone: String): UserEntity {
        val suffix = phone.takeLast(4)
        val email = "$phone@mobile.indieestate.local"
        return UserEntity(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(randomNumericCode(16))),
            name = "User $suffix",
            phone = phone,
        )
    }

    private fun randomNumericCode(length: Int): String =
        (1..length).joinToString("") { random.nextInt(10).toString() }

    private data class PendingOtp(val code: String, val expiresAt: Instant)

    companion object {
        fun normalizePhone(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            return when {
                digits.length == 10 -> digits
                digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
                digits.length in 10..15 -> digits.takeLast(10)
                else -> throw ApiException(HttpStatus.BAD_REQUEST, "Enter a valid 10-digit mobile number")
            }
        }

        fun maskPhone(phone: String): String =
            if (phone.length <= 4) phone else "******${phone.takeLast(4)}"
    }
}
