package com.indieestate.backend.service

import com.indieestate.backend.auth.otp.OtpPhone
import com.indieestate.backend.auth.otp.OtpProperties
import com.indieestate.backend.auth.otp.OtpSendRateLimiter
import com.indieestate.backend.auth.otp.OtpSmsSender
import com.indieestate.backend.auth.otp.OtpStore
import com.indieestate.backend.auth.otp.PendingOtp
import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.AuthResponse
import com.indieestate.backend.dto.OtpSendResponse
import com.indieestate.backend.entity.UserEntity
import com.indieestate.backend.repository.UserRepository
import com.indieestate.backend.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

@Service
class OtpAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val otpStore: OtpStore,
    private val rateLimiter: OtpSendRateLimiter,
    private val smsSender: OtpSmsSender,
    private val properties: OtpProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val random = SecureRandom()

    fun sendOtp(rawPhone: String, clientIp: String? = null): OtpSendResponse {
        val phone = OtpPhone.normalize(rawPhone)
        rateLimiter.checkAndRecord(phone, clientIp)

        val code = when {
            properties.devMode && properties.devFixedCode.isNotBlank() ->
                properties.devFixedCode.padStart(properties.length, '0').take(properties.length)
            else -> randomNumericCode(properties.length)
        }
        val pending = PendingOtp(
            code = code,
            expiresAt = Instant.now().plusSeconds(properties.ttlSeconds),
        )
        otpStore.savePending(phone, pending, properties.ttlSeconds)
        otpStore.clearVerifyAttempts(phone)

        if (properties.devMode) {
            log.info("DEV OTP for {}: {} (expires in {}s)", OtpPhone.mask(phone), code, properties.ttlSeconds)
        } else {
            smsSender.sendLoginOtp(phone, code, properties.ttlSeconds)
        }

        return OtpSendResponse(
            message = "OTP sent to ${OtpPhone.mask(phone)}",
            debugOtp = if (properties.devMode) code else null,
        )
    }

    @Transactional
    fun verifyOtp(rawPhone: String, otp: String): AuthResponse {
        val phone = OtpPhone.normalize(rawPhone)
        val pending = otpStore.getPending(phone)
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Request a new OTP first")

        if (Instant.now().isAfter(pending.expiresAt)) {
            otpStore.deletePending(phone)
            throw ApiException(HttpStatus.BAD_REQUEST, "OTP expired. Request a new one.")
        }

        val attempts = otpStore.incrementVerifyAttempts(phone, properties.ttlSeconds)
        if (attempts > properties.maxVerifyAttempts) {
            otpStore.deletePending(phone)
            throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid attempts. Request a new OTP.")
        }

        if (pending.code != otp.trim()) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Invalid OTP")
        }

        otpStore.deletePending(phone)

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

    companion object {
        fun normalizePhone(raw: String): String = OtpPhone.normalize(raw)
        fun maskPhone(phone: String): String = OtpPhone.mask(phone)
    }
}
