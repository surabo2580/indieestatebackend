package com.indieestate.backend.feed

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.indieestate.backend.dto.ReelResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["app.reels.redis-enabled"], havingValue = "true")
class RedisReelFeedCache(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : ReelFeedCache {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun get(): List<ReelResponse>? {
        val json = try {
            redis.opsForValue().get(KEY)
        } catch (error: Exception) {
            log.warn("Redis feed get failed", error)
            return null
        } ?: return null
        return try {
            objectMapper.readValue(json, object : TypeReference<List<ReelResponse>>() {})
        } catch (error: Exception) {
            log.warn("Redis feed decode failed", error)
            null
        }
    }

    override fun put(reels: List<ReelResponse>) {
        try {
            redis.opsForValue().set(KEY, objectMapper.writeValueAsString(reels), TTL)
        } catch (error: Exception) {
            log.warn("Redis feed put failed", error)
        }
    }

    override fun evict() {
        try {
            redis.delete(KEY)
        } catch (error: Exception) {
            log.warn("Redis feed evict failed", error)
        }
    }

    companion object {
        private const val KEY = "feed:reels:v1"
        private val TTL = Duration.ofSeconds(30)
    }
}
