package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Clients : UUIDTable("clients") {
    val organizationId = reference("organization_id", Organizations.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 120)
    val addressId = reference("address_id", Addresses.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val phoneNumber = varchar("phone_number", 50).nullable()
    val email = varchar("email", 255).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
