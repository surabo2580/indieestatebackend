package com.indieestate.backend.repository

import com.indieestate.backend.entity.CommunityCommentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommunityCommentRepository : JpaRepository<CommunityCommentEntity, UUID> {
    fun findAllByPostIdOrderByCreatedAtAsc(postId: UUID): List<CommunityCommentEntity>
}
