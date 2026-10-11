package com.indieestate.backend.auth.otp

import java.time.Instant

data class PendingOtp(
    val code: String,
    val expiresAt: Instant,
)

interface OtpStore {
    fun savePending(phone: String, pending: PendingOtp, ttlSeconds: Long)
    fun getPending(phone: String): PendingOtp?
    fun deletePending(phone: String)
    fun incrementVerifyAttempts(phone: String, ttlSeconds: Long): Int
    fun clearVerifyAttempts(phone: String)
}
