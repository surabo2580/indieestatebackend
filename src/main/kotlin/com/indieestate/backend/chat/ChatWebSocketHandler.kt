package com.indieestate.backend.chat

import com.indieestate.backend.dto.ChatRealtimeEvent
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.UUID

@Component
class ChatWebSocketHandler(
    private val registry: ChatSessionRegistry,
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.attributes[USER_ID] as? UUID
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"))
            return
        }
        registry.add(userId, session)
        session.sendMessage(TextMessage("""{"type":"${ChatRealtimeEvent.READY}"}"""))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val body = message.payload
        if (body.equals("ping", ignoreCase = true) || body.contains("\"ping\"")) {
            session.sendMessage(TextMessage("""{"type":"${ChatRealtimeEvent.PONG}"}"""))
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        registry.remove(session)
    }

    companion object {
        const val USER_ID = "chatUserId"
    }
}
