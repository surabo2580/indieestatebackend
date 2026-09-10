package com.indieestate.backend.entity

import com.indieestate.backend.dto.FormSchema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "forms")
class FormEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true)
    var code: String = "",
    @Column(nullable = false)
    var type: String = "",
    @Column(name = "category_id")
    var categoryId: UUID? = null,
    @Column(name = "service_id")
    var serviceId: UUID? = null,
    @Column(nullable = false)
    var title: String = "",
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_schema", columnDefinition = "jsonb", nullable = false)
    var schema: FormSchema = FormSchema(),
    @Column(nullable = false)
    var version: Int = 1,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
)
