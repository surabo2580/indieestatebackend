package com.indieestate.backend.controller

import com.indieestate.backend.dto.ChatMessageResponse
import com.indieestate.backend.dto.ConversationResponse
import com.indieestate.backend.dto.SendChatMessageRequest
import com.indieestate.backend.dto.StartConversationRequest
import com.indieestate.backend.dto.UnreadCountResponse
import com.indieestate.backend.security.UserPrincipal
import com.indieestate.backend.service.ChatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chat")
@SecurityRequirement(name = "bearer-jwt")
class ChatController(
    private val chatService: ChatService,
) {

    @PostMapping
    @Operation(summary = "Start or resume a 1:1 conversation, optionally about an ad")
    fun start(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: StartConversationRequest,
    ): ConversationResponse = chatService.startOrGet(principal.userId, request)

    @GetMapping
    @Operation(summary = "Inbox conversations for the current user")
    fun inbox(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "30") limit: Int,
    ): List<ConversationResponse> = chatService.inbox(principal.userId, limit)

    @GetMapping("/unread-count")
    @Operation(summary = "Total unread chat messages")
    fun unreadCount(@AuthenticationPrincipal principal: UserPrincipal): UnreadCountResponse =
        chatService.unreadCount(principal.userId)

    @GetMapping("/{id}/messages")
    @Operation(summary = "Page messages. Use before for history, after for new messages.")
    fun messages(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(required = false) after: Instant?,
        @RequestParam(defaultValue = "40") limit: Int,
    ): List<ChatMessageResponse> = chatService.listMessages(principal.userId, id, before, after, limit)

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a message in a conversation")
    fun send(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: SendChatMessageRequest,
    ): ChatMessageResponse = chatService.send(principal.userId, id, request)

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a conversation as read")
    fun markRead(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): UnreadCountResponse {
        chatService.markRead(principal.userId, id)
        return chatService.unreadCount(principal.userId)
    }
}
