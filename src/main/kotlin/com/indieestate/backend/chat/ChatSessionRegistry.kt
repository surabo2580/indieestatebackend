package com.indieestate.backend.chat

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Component
class ChatSessionRegistry {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sessions = ConcurrentHashMap<UUID, CopyOnWriteArraySet<WebSocketSession>>()

    fun add(userId: UUID, session: WebSocketSession) {
        sessions.computeIfAbsent(userId) { CopyOnWriteArraySet() }.add(session)
    }

    fun remove(session: WebSocketSession) {
        sessions.values.forEach { it.remove(session) }
        sessions.entries.removeIf { it.value.isEmpty() }
    }

    fun deliver(userId: UUID, payload: String) {
        val live = sessions[userId] ?: return
        live.forEach { session ->
            if (!session.isOpen) {
                live.remove(session)
                return@forEach
            }
            try {
                synchronized(session) {
                    session.sendMessage(TextMessage(payload))
                }
            } catch (error: Exception) {
                log.debug("Dropping chat socket for {}", userId, error)
                live.remove(session)
            }
        }
        if (live.isEmpty()) sessions.remove(userId)
    }
}
