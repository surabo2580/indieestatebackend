package com.indieestate.backend.feed

import com.indieestate.backend.dto.ReelPageResponse

interface ReelFeedCache {
    fun getPage(cacheKey: String): ReelPageResponse?
    fun putPage(cacheKey: String, page: ReelPageResponse)
    fun evictAll()
}
