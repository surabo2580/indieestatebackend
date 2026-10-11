package com.indieestate.backend.dto

import java.util.UUID

data class ChatRealtimeEvent(
    val type: String,
    val recipientUserId: UUID,
    val unreadCount: Long = 0,
    val message: ChatMessageResponse? = null,
    val conversation: ConversationResponse? = null,
) {
    companion object {
        const val MESSAGE = "message"
        const val UNREAD = "unread"
        const val READY = "ready"
        const val PONG = "pong"
    }
}
