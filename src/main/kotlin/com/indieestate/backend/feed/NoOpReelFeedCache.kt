package com.indieestate.backend.feed

import com.indieestate.backend.dto.ReelPageResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.reels.redis-enabled"], havingValue = "false", matchIfMissing = true)
class NoOpReelFeedCache : ReelFeedCache {
    override fun getPage(cacheKey: String): ReelPageResponse? = null
    override fun putPage(cacheKey: String, page: ReelPageResponse) = Unit
    override fun evictAll() = Unit
}
