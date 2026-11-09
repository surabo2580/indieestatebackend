package com.indieestate.backend.repository

import com.indieestate.backend.entity.ReelEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ReelRepository : JpaRepository<ReelEntity, UUID> {
    fun findAllByIsActiveTrueAndStatusOrderByCreatedAtDesc(status: String): List<ReelEntity>
    fun findAllByIsActiveTrueAndStatusInOrderByCreatedAtDesc(statuses: Collection<String>): List<ReelEntity>
    fun findFirstByVideoId(videoId: UUID): ReelEntity?

    @Query(
        """
        SELECT r FROM ReelEntity r
        WHERE r.isActive = true AND r.status IN :statuses
        ORDER BY r.createdAt DESC, r.id DESC
        """,
    )
    fun findFeedFirstPage(
        @Param("statuses") statuses: Collection<String>,
        pageable: Pageable,
    ): List<ReelEntity>

    @Query(
        """
        SELECT r FROM ReelEntity r
        WHERE r.isActive = true AND r.status IN :statuses
        AND (
            r.createdAt < :beforeCreatedAt
            OR (r.createdAt = :beforeCreatedAt AND r.id < :beforeId)
        )
        ORDER BY r.createdAt DESC, r.id DESC
        """,
    )
    fun findFeedPage(
        @Param("statuses") statuses: Collection<String>,
        @Param("beforeCreatedAt") beforeCreatedAt: Instant,
        @Param("beforeId") beforeId: UUID,
        pageable: Pageable,
    ): List<ReelEntity>

    @Query("SELECT DISTINCT r.videoId FROM ReelEntity r WHERE r.videoId IS NOT NULL")
    fun findAllVideoIdsWithReels(): List<UUID>
}
