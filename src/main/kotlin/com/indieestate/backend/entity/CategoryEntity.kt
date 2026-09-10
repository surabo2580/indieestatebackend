package com.indieestate.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "categories")
class CategoryEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true)
    var slug: String = "",
    @Column(nullable = false)
    var name: String = "",
    var icon: String? = null,
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
)
