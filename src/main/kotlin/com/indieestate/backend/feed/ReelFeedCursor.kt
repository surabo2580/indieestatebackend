package com.indieestate.backend.feed

import java.time.Instant
import java.util.Base64
import java.util.UUID

data class ReelFeedCursor(
    val createdAt: Instant,
    val id: UUID,
) {
    fun encode(): String {
        val raw = "${createdAt.toEpochMilli()}|$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
    }

    companion object {
        fun decode(token: String?): ReelFeedCursor? {
            if (token.isNullOrBlank()) return null
            return try {
                val raw = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
                val parts = raw.split('|', limit = 2)
                if (parts.size != 2) return null
                ReelFeedCursor(
                    createdAt = Instant.ofEpochMilli(parts[0].toLong()),
                    id = UUID.fromString(parts[1]),
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
