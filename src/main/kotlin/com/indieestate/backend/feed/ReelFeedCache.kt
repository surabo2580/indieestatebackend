package com.indieestate.backend.feed

import com.indieestate.backend.dto.ReelResponse

interface ReelFeedCache {
    fun get(): List<ReelResponse>?
    fun put(reels: List<ReelResponse>)
    fun evict()
}
