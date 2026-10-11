package com.indieestate.backend.auth.otp

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnProperty(name = ["auth.otp.redis-enabled"], havingValue = "false", matchIfMissing = true)
class InMemoryOtpStore : OtpStore {
    private val pending = ConcurrentHashMap<String, PendingOtp>()
    private val attempts = ConcurrentHashMap<String, Int>()

    override fun savePending(phone: String, pending: PendingOtp, ttlSeconds: Long) {
        this.pending[phone] = pending
    }

    override fun getPending(phone: String): PendingOtp? = pending[phone]

    override fun deletePending(phone: String) {
        pending.remove(phone)
        attempts.remove(phone)
    }

    override fun incrementVerifyAttempts(phone: String, ttlSeconds: Long): Int =
        attempts.compute(phone) { _, current -> (current ?: 0) + 1 } ?: 1

    override fun clearVerifyAttempts(phone: String) {
        attempts.remove(phone)
    }
}
