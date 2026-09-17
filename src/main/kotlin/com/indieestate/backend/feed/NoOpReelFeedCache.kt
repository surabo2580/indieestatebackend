package com.indieestate.backend.feed

import com.indieestate.backend.dto.ReelResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.reels.redis-enabled"], havingValue = "false", matchIfMissing = true)
class NoOpReelFeedCache : ReelFeedCache {
    override fun get(): List<ReelResponse>? = null
    override fun put(reels: List<ReelResponse>) = Unit
    override fun evict() = Unit
}
