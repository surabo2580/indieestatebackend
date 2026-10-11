package com.indieestate.backend.chat

import com.indieestate.backend.dto.ChatRealtimeEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class ChatRealtimePublisher(
    private val registry: ChatSessionRegistry,
    redisProvider: ObjectProvider<StringRedisTemplate>,
    @param:Value("\${app.chat.redis-enabled:false}") private val redisEnabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val redis: StringRedisTemplate? = if (redisEnabled) redisProvider.getIfAvailable() else null

    fun publish(event: ChatRealtimeEvent) {
        val payload = chatJson.writeValueAsString(event)
        if (publishToRedis(payload)) return
        registry.deliver(event.recipientUserId, payload)
    }

    private fun publishToRedis(payload: String): Boolean {
        val template = redis ?: return false
        return try {
            template.convertAndSend(CHANNEL, payload)
            true
        } catch (error: Exception) {
            log.warn("Chat redis publish failed; using local sockets", error)
            false
        }
    }

    companion object {
        const val CHANNEL = "indieestate.chat.events"
    }
}
