package com.indieestate.backend.feed

import com.fasterxml.jackson.databind.ObjectMapper
import com.indieestate.backend.dto.ReelPageResponse
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

    override fun getPage(cacheKey: String): ReelPageResponse? {
        val json = try {
            redis.opsForValue().get(pageKey(cacheKey))
        } catch (error: Exception) {
            log.warn("Redis feed get failed", error)
            return null
        } ?: return null
        return try {
            objectMapper.readValue(json, ReelPageResponse::class.java)
        } catch (error: Exception) {
            log.warn("Redis feed decode failed", error)
            null
        }
    }

    override fun putPage(cacheKey: String, page: ReelPageResponse) {
        try {
            redis.opsForValue().set(
                pageKey(cacheKey),
                objectMapper.writeValueAsString(page),
                TTL,
            )
        } catch (error: Exception) {
            log.warn("Redis feed put failed", error)
        }
    }

    override fun evictAll() {
        try {
            val keys = redis.keys("$KEY_PREFIX*")
            if (!keys.isNullOrEmpty()) {
                redis.delete(keys)
            }
        } catch (error: Exception) {
            log.warn("Redis feed evict failed", error)
        }
    }

    private fun pageKey(cacheKey: String) = "$KEY_PREFIX$cacheKey"

    companion object {
        private const val KEY_PREFIX = "feed:reels:v2:page:"
        private val TTL = Duration.ofSeconds(30)
    }
}
