package com.indieestate.backend.chat

import com.indieestate.backend.dto.ChatRealtimeEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.chat.redis-enabled"], havingValue = "true")
class ChatRedisSubscriber(
    private val registry: ChatSessionRegistry,
) : MessageListener {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val event = chatJson.readValue(message.body, ChatRealtimeEvent::class.java)
            val payload = chatJson.writeValueAsString(event)
            registry.deliver(event.recipientUserId, payload)
        } catch (error: Exception) {
            log.warn("Ignored invalid chat redis event", error)
        }
    }
}

@Configuration
@ConditionalOnProperty(name = ["app.chat.redis-enabled"], havingValue = "true")
class ChatRedisListenerConfig {
    @Bean
    fun chatRedisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        subscriber: ChatRedisSubscriber,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(subscriber, ChannelTopic(ChatRealtimePublisher.CHANNEL))
        return container
    }
}
