package com.indieestate.backend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value("\${security.jwt.secret:change-me-in-prod-at-least-32-chars}") rawSecret: String,
    @param:Value("\${security.jwt.issuer:indieestate-backend}") private val issuer: String,
    @param:Value("\${security.jwt.access-token-ttl-seconds:7200}") private val accessTokenTtlSeconds: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(
        rawSecret.toByteArray(StandardCharsets.UTF_8).let { bytes ->
            if (bytes.size >= 32) bytes else bytes.copyOf(32)
        },
    )

    fun createToken(userId: UUID, email: String): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(accessTokenTtlSeconds)
        return Jwts.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact()
    }

    fun parsePrincipal(token: String): UserPrincipal? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .payload
            UserPrincipal(
                userId = UUID.fromString(claims.subject),
                email = claims.get("email", String::class.java).orEmpty(),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun ttlSeconds(): Long = accessTokenTtlSeconds
}
