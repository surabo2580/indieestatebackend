package com.indieestate.backend.chat

import com.indieestate.backend.repository.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatUnreadCache(
    private val conversations: ConversationRepository,
    redisProvider: ObjectProvider<StringRedisTemplate>,
    @param:Value("\${app.chat.redis-enabled:false}") private val redisEnabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val redis: StringRedisTemplate? = if (redisEnabled) redisProvider.getIfAvailable() else null
    private val local = ConcurrentHashMap<UUID, Long>()

    fun add(userId: UUID, delta: Long) {
        val cached = peek(userId)
        if (cached != null) {
            set(userId, cached + delta)
        } else {
            set(userId, conversations.unreadTotal(userId))
        }
    }

    fun set(userId: UUID, value: Long) {
        val safe = value.coerceAtLeast(0)
        local[userId] = safe
        val template = redis ?: return
        try {
            template.opsForValue().set(key(userId), safe.toString())
        } catch (error: Exception) {
            log.debug("Chat unread redis set failed", error)
        }
    }

    fun getOrLoad(userId: UUID): Long {
        peek(userId)?.let { return it }
        val loaded = conversations.unreadTotal(userId)
        set(userId, loaded)
        return loaded
    }

    private fun peek(userId: UUID): Long? {
        val template = redis
        if (template != null) {
            try {
                val cached = template.opsForValue().get(key(userId))?.toLongOrNull()
                if (cached != null) {
                    local[userId] = cached
                    return cached
                }
            } catch (error: Exception) {
                log.debug("Chat unread redis get failed", error)
            }
        }
        return local[userId]
    }

    private fun key(userId: UUID) = "chat:unread:$userId"
}
