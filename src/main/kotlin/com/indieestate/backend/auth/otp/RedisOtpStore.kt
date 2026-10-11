package com.indieestate.backend.auth.otp

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(name = ["auth.otp.redis-enabled"], havingValue = "true")
class RedisOtpStore(
    private val redis: StringRedisTemplate,
) : OtpStore {

    override fun savePending(phone: String, pending: PendingOtp, ttlSeconds: Long) {
        val key = pendingKey(phone)
        redis.opsForValue().set(key, "${pending.code}|${pending.expiresAt.epochSecond}", ttlSeconds, TimeUnit.SECONDS)
    }

    override fun getPending(phone: String): PendingOtp? {
        val raw = redis.opsForValue().get(pendingKey(phone)) ?: return null
        val parts = raw.split("|", limit = 2)
        if (parts.size != 2) return null
        return PendingOtp(code = parts[0], expiresAt = Instant.ofEpochSecond(parts[1].toLong()))
    }

    override fun deletePending(phone: String) {
        redis.delete(pendingKey(phone))
        redis.delete(attemptsKey(phone))
    }

    override fun incrementVerifyAttempts(phone: String, ttlSeconds: Long): Int {
        val key = attemptsKey(phone)
        val count = redis.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS)
        }
        return count.toInt()
    }

    override fun clearVerifyAttempts(phone: String) {
        redis.delete(attemptsKey(phone))
    }

    private fun pendingKey(phone: String) = "auth:otp:pending:$phone"
    private fun attemptsKey(phone: String) = "auth:otp:attempts:$phone"
}
