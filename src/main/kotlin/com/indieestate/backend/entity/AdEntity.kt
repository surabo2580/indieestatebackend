package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ads")
class AdEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),
    @Column(name = "form_id", nullable = false)
    var formId: UUID = UUID.randomUUID(),
    @Column(name = "category_id")
    var categoryId: UUID? = null,
    @Column(name = "service_id")
    var serviceId: UUID? = null,
    @Column(nullable = false)
    var title: String = "",
    @Column(nullable = false)
    var status: String = AdStatus.ACTIVE,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var data: Map<String, Any?> = emptyMap(),
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

object AdStatus {
    const val ACTIVE = "ACTIVE"
}
