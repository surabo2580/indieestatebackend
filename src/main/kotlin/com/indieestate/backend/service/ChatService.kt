package com.indieestate.backend.service

import com.indieestate.backend.chat.ChatRealtimePublisher
import com.indieestate.backend.chat.ChatUnreadCache
import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.ChatMessageResponse
import com.indieestate.backend.dto.ChatRealtimeEvent
import com.indieestate.backend.dto.ConversationResponse
import com.indieestate.backend.dto.SendChatMessageRequest
import com.indieestate.backend.dto.StartConversationRequest
import com.indieestate.backend.dto.UnreadCountResponse
import com.indieestate.backend.entity.ChatMessageEntity
import com.indieestate.backend.entity.ConversationEntity
import com.indieestate.backend.entity.UserEntity
import com.indieestate.backend.repository.AdRepository
import com.indieestate.backend.repository.ChatMessageRepository
import com.indieestate.backend.repository.ConversationRepository
import com.indieestate.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Service
class ChatService(
    private val conversations: ConversationRepository,
    private val messages: ChatMessageRepository,
    private val users: UserRepository,
    private val ads: AdRepository,
    private val unreadCache: ChatUnreadCache,
    private val realtime: ChatRealtimePublisher,
) {

    @Transactional
    fun startOrGet(userId: UUID, request: StartConversationRequest): ConversationResponse {
        if (request.peerUserId == userId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "You cannot message yourself")
        }
        users.findById(request.peerUserId)
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "User not found") }
        val ad = request.adId?.let { id ->
            ads.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Ad not found") }
        }
        val (low, high) = orderedPair(userId, request.peerUserId)
        val pairKey = pairKey(low, high, request.adId)
        val conversation = conversations.findByPairKey(pairKey) ?: conversations.save(
            ConversationEntity(
                userLowId = low,
                userHighId = high,
                pairKey = pairKey,
                adId = request.adId,
                adTitle = ad?.title,
                lastMessageAt = Instant.now(),
            ),
        )
        return conversation.toResponse(userId)
    }

    @Transactional(readOnly = true)
    fun inbox(userId: UUID, limit: Int): List<ConversationResponse> {
        val page = PageRequest.of(0, limit.coerceIn(1, 50))
        val rows = conversations.findInbox(userId, page)
        val peerIds = rows.map { if (it.userLowId == userId) it.userHighId else it.userLowId }.toSet()
        val peers = users.findAllById(peerIds).associateBy { it.id }
        return rows.map { it.toResponse(userId, peers) }
    }

    @Transactional(readOnly = true)
    fun unreadCount(userId: UUID): UnreadCountResponse =
        UnreadCountResponse(unreadCache.getOrLoad(userId))

    @Transactional(readOnly = true)
    fun listMessages(
        userId: UUID,
        conversationId: UUID,
        before: Instant?,
        after: Instant?,
        limit: Int,
    ): List<ChatMessageResponse> {
        val conversation = requireParticipant(userId, conversationId)
        if (after != null) {
            return messages
                .findByConversationIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(conversation.id, after)
                .map { it.toResponse(userId) }
        }
        val cursor = before ?: Instant.now().plusSeconds(1)
        return messages
            .findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                conversation.id,
                cursor,
                PageRequest.of(0, limit.coerceIn(1, 80)),
            )
            .asReversed()
            .map { it.toResponse(userId) }
    }

    @Transactional
    fun send(userId: UUID, conversationId: UUID, request: SendChatMessageRequest): ChatMessageResponse {
        val conversation = requireParticipant(userId, conversationId)
        val body = request.body.trim()
        if (body.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Message cannot be empty")
        }
        val now = Instant.now()
        val saved = messages.save(
            ChatMessageEntity(
                conversationId = conversation.id,
                senderId = userId,
                body = body.take(2000),
                createdAt = now,
            ),
        )
        conversation.lastMessage = body.take(180)
        conversation.lastMessageAt = now
        conversation.lastSenderId = userId
        if (conversation.userLowId == userId) {
            conversation.userHighUnread += 1
        } else {
            conversation.userLowUnread += 1
        }
        conversations.save(conversation)
        afterCommit {
            unreadCache.add(peerId(conversation, userId), 1)
            unreadCache.set(userId, conversations.unreadTotal(userId))
            publishMessage(userId, conversation, saved)
        }
        return saved.toResponse(userId)
    }

    @Transactional
    fun markRead(userId: UUID, conversationId: UUID) {
        val conversation = requireParticipant(userId, conversationId)
        if (conversation.userLowId == userId) {
            conversation.userLowUnread = 0
        } else {
            conversation.userHighUnread = 0
        }
        conversations.save(conversation)
        afterCommit {
            val total = conversations.unreadTotal(userId)
            unreadCache.set(userId, total)
            realtime.publish(
                ChatRealtimeEvent(
                    type = ChatRealtimeEvent.UNREAD,
                    recipientUserId = userId,
                    unreadCount = total,
                    conversation = conversation.toResponse(userId),
                ),
            )
        }
    }

    private fun requireParticipant(userId: UUID, conversationId: UUID): ConversationEntity {
        val conversation = conversations.findById(conversationId)
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Conversation not found") }
        if (conversation.userLowId != userId && conversation.userHighId != userId) {
            throw ApiException(HttpStatus.FORBIDDEN, "You are not in this conversation")
        }
        return conversation
    }

    private fun afterCommit(block: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() = block()
                },
            )
        } else {
            block()
        }
    }

    private fun publishMessage(
        senderId: UUID,
        conversation: ConversationEntity,
        saved: ChatMessageEntity,
    ) {
        val peerId = peerId(conversation, senderId)
        realtime.publish(
            ChatRealtimeEvent(
                type = ChatRealtimeEvent.MESSAGE,
                recipientUserId = peerId,
                unreadCount = unreadCache.getOrLoad(peerId),
                message = saved.toResponse(peerId),
                conversation = conversation.toResponse(peerId),
            ),
        )
        realtime.publish(
            ChatRealtimeEvent(
                type = ChatRealtimeEvent.MESSAGE,
                recipientUserId = senderId,
                unreadCount = unreadCache.getOrLoad(senderId),
                message = saved.toResponse(senderId),
                conversation = conversation.toResponse(senderId),
            ),
        )
    }

    private fun peerId(conversation: ConversationEntity, viewerId: UUID): UUID =
        if (conversation.userLowId == viewerId) conversation.userHighId else conversation.userLowId

    private fun ConversationEntity.toResponse(
        viewerId: UUID,
        peers: Map<UUID, UserEntity> = emptyMap(),
    ): ConversationResponse {
        val peerId = peerId(this, viewerId)
        val peer = peers[peerId] ?: users.findById(peerId).orElse(null)
        val unread = if (userLowId == viewerId) userLowUnread else userHighUnread
        return ConversationResponse(
            id = id,
            peerUserId = peerId,
            peerName = peer?.name ?: "User",
            adId = adId,
            adTitle = adTitle,
            lastMessage = lastMessage,
            lastMessageAt = lastMessageAt,
            unreadCount = unread,
        )
    }

    private fun ChatMessageEntity.toResponse(viewerId: UUID) = ChatMessageResponse(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        body = body,
        createdAt = createdAt,
        mine = senderId == viewerId,
    )

    private fun orderedPair(a: UUID, b: UUID): Pair<UUID, UUID> =
        if (a.toString() <= b.toString()) a to b else b to a

    private fun pairKey(low: UUID, high: UUID, adId: UUID?): String =
        "$low:$high:${adId ?: "-"}"
}
