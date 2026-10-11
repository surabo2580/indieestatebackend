package com.indieestate.backend.auth.otp

import com.indieestate.backend.controller.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

interface OtpSendRateLimiter {
    fun checkAndRecord(phone: String, clientIp: String?)
}

@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["auth.otp.redis-enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class InMemoryOtpSendRateLimiter(
    private val properties: OtpProperties,
) : OtpSendRateLimiter {
    private val phoneCounts = java.util.concurrent.ConcurrentHashMap<String, WindowCounter>()
    private val ipCounts = java.util.concurrent.ConcurrentHashMap<String, WindowCounter>()

    override fun checkAndRecord(phone: String, clientIp: String?) {
        val windowMs = properties.sendRateWindowSeconds * 1000
        checkLimit(phoneCounts, phone, properties.sendRateLimitPerPhone, windowMs, "phone number")
        if (!clientIp.isNullOrBlank()) {
            checkLimit(ipCounts, clientIp, properties.sendRateLimitPerIp, windowMs, "IP address")
        }
    }

    private fun checkLimit(
        map: java.util.concurrent.ConcurrentHashMap<String, WindowCounter>,
        key: String,
        limit: Int,
        windowMs: Long,
        label: String,
    ) {
        val now = System.currentTimeMillis()
        val counter = map.compute(key) { _, existing ->
            if (existing == null || now - existing.windowStartMs > windowMs) {
                WindowCounter(windowStartMs = now, count = 1)
            } else {
                existing.copy(count = existing.count + 1)
            }
        }!!
        if (counter.count > limit) {
            throw ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many OTP requests for this $label. Try again later.",
            )
        }
    }

    private data class WindowCounter(val windowStartMs: Long, val count: Int)
}

@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["auth.otp.redis-enabled"],
    havingValue = "true",
)
class RedisOtpSendRateLimiter(
    private val redis: org.springframework.data.redis.core.StringRedisTemplate,
    private val properties: OtpProperties,
) : OtpSendRateLimiter {

    override fun checkAndRecord(phone: String, clientIp: String?) {
        val window = properties.sendRateWindowSeconds
        increment("auth:otp:rate:phone:$phone", properties.sendRateLimitPerPhone, window, "phone number")
        if (!clientIp.isNullOrBlank()) {
            increment("auth:otp:rate:ip:$clientIp", properties.sendRateLimitPerIp, window, "IP address")
        }
    }

    private fun increment(key: String, limit: Int, windowSeconds: Long, label: String) {
        val count = redis.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redis.expire(key, windowSeconds, java.util.concurrent.TimeUnit.SECONDS)
        }
        if (count > limit) {
            throw ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many OTP requests for this $label. Try again later.",
            )
        }
    }
}
