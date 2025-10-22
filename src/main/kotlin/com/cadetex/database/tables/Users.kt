package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable("users") {
    val organizationId = reference("organization_id", Organizations.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val email = varchar("email", 150).uniqueIndex()
    val passwordHash = text("password_hash")
    val role = varchar("role", 20)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}