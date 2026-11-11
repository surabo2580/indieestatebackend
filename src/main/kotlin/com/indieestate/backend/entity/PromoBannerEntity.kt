package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "promo_banners")
class PromoBannerEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var title: String = "",
    @Column(nullable = false)
    var description: String = "",
    @Column(name = "target_url", nullable = false)
    var targetUrl: String = "",
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
