package com.indieestate.backend.feed

import com.indieestate.backend.dto.ReelPageResponse
import com.indieestate.backend.dto.toResponse
import com.indieestate.backend.entity.ReelStatus
import com.indieestate.backend.repository.ReelRepository
import com.indieestate.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Feed service and cache layer (step 8 in [docs/ARCHITECTURE.md]).
 * Serves paginated reel candidates from PostgreSQL with optional Redis page cache.
 */
@Service
class FeedService(
    private val reelRepository: ReelRepository,
    private val userRepository: UserRepository,
    private val feedCache: ReelFeedCache,
) {
    @Transactional(readOnly = true)
    fun listActivePage(cursorToken: String?, limit: Int): ReelPageResponse {
        val pageSize = limit.coerceIn(1, MAX_FEED_PAGE_SIZE)
        val cacheKey = "${cursorToken.orEmpty()}|$pageSize"
        feedCache.getPage(cacheKey)?.let { return it }

        val cursor = ReelFeedCursor.decode(cursorToken)
        val fetchSize = pageSize + 1
        val statuses = listOf(ReelStatus.READY, ReelStatus.PROCESSING)
        val reels = reelRepository.findFeedPage(
            statuses = statuses,
            beforeCreatedAt = cursor?.createdAt,
            beforeId = cursor?.id,
            pageable = PageRequest.of(0, fetchSize),
        )
        if (reels.isEmpty()) {
            val empty = ReelPageResponse(items = emptyList(), nextCursor = null)
            feedCache.putPage(cacheKey, empty)
            return empty
        }
        val hasMore = reels.size > pageSize
        val pageItems = if (hasMore) reels.take(pageSize) else reels
        val users = userRepository.findAllById(pageItems.map { it.userId }.toSet())
            .associateBy { it.id }
        val items = pageItems.map { reel -> reel.toResponse(users[reel.userId]) }
        val nextCursor = if (hasMore) {
            val last = pageItems.last()
            ReelFeedCursor(createdAt = last.createdAt, id = last.id).encode()
        } else {
            null
        }
        val page = ReelPageResponse(items = items, nextCursor = nextCursor)
        feedCache.putPage(cacheKey, page)
        return page
    }

    fun evictAllPages() = feedCache.evictAll()
}

private const val MAX_FEED_PAGE_SIZE = 50
