package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.CommunityCommentResponse
import com.indieestate.backend.dto.CommunityPostDetailResponse
import com.indieestate.backend.dto.CommunityPostResponse
import com.indieestate.backend.dto.CreateCommunityCommentRequest
import com.indieestate.backend.dto.CreateCommunityPostRequest
import com.indieestate.backend.entity.CommunityCategory
import com.indieestate.backend.entity.CommunityCommentEntity
import com.indieestate.backend.entity.CommunityPostEntity
import com.indieestate.backend.repository.CommunityCommentRepository
import com.indieestate.backend.repository.CommunityPostRepository
import com.indieestate.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CommunityService(
    private val posts: CommunityPostRepository,
    private val comments: CommunityCommentRepository,
    private val users: UserRepository,
) {

    @Transactional(readOnly = true)
    fun feed(category: String?, limit: Int): List<CommunityPostResponse> {
        val page = PageRequest.of(0, limit.coerceIn(1, 50))
        val normalized = category?.trim()?.uppercase()?.takeIf { it in CommunityCategory.all }
        val rows = if (normalized == null) {
            posts.findAllByOrderByCreatedAtDesc(page)
        } else {
            posts.findAllByCategoryOrderByCreatedAtDesc(normalized, page)
        }
        return rows.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun detail(postId: UUID): CommunityPostDetailResponse {
        val post = posts.findById(postId)
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Post not found") }
        return CommunityPostDetailResponse(
            post = post.toResponse(),
            comments = comments.findAllByPostIdOrderByCreatedAtAsc(postId).map { it.toResponse() },
        )
    }

    @Transactional
    fun create(userId: UUID, request: CreateCommunityPostRequest): CommunityPostResponse {
        val body = request.body.trim()
        if (body.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Write something to post")
        }
        val saved = posts.save(
            CommunityPostEntity(
                userId = userId,
                authorName = displayName(userId),
                category = CommunityCategory.normalize(request.category),
                body = body.take(500),
            ),
        )
        return saved.toResponse()
    }

    @Transactional
    fun comment(userId: UUID, postId: UUID, request: CreateCommunityCommentRequest): CommunityCommentResponse {
        val post = posts.findById(postId)
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Post not found") }
        val body = request.body.trim()
        if (body.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Write a reply")
        }
        val saved = comments.save(
            CommunityCommentEntity(
                postId = post.id,
                userId = userId,
                authorName = displayName(userId),
                body = body.take(500),
            ),
        )
        post.commentCount += 1
        posts.save(post)
        return saved.toResponse()
    }

    private fun displayName(userId: UUID): String {
        val name = users.findById(userId).orElse(null)?.name?.trim().orEmpty()
        return name.ifBlank { "Neighbour" }
    }

    private fun CommunityPostEntity.toResponse() = CommunityPostResponse(
        id = id,
        authorName = authorName,
        category = category,
        body = body,
        commentCount = commentCount,
        createdAt = createdAt,
    )

    private fun CommunityCommentEntity.toResponse() = CommunityCommentResponse(
        id = id,
        postId = postId,
        authorName = authorName,
        body = body,
        createdAt = createdAt,
    )
}
