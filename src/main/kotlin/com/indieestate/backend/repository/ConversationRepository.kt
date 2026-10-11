package com.indieestate.backend.repository

import com.indieestate.backend.entity.ConversationEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ConversationRepository : JpaRepository<ConversationEntity, UUID> {
    fun findByPairKey(pairKey: String): ConversationEntity?

    @Query(
        """
        SELECT c FROM ConversationEntity c
        WHERE c.userLowId = :userId OR c.userHighId = :userId
        ORDER BY c.lastMessageAt DESC
        """,
    )
    fun findInbox(userId: UUID, pageable: Pageable): List<ConversationEntity>

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN c.userLowId = :userId THEN c.userLowUnread
                WHEN c.userHighId = :userId THEN c.userHighUnread
                ELSE 0
            END
        ), 0)
        FROM ConversationEntity c
        WHERE c.userLowId = :userId OR c.userHighId = :userId
        """,
    )
    fun unreadTotal(userId: UUID): Long
}
