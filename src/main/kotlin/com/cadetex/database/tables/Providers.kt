package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Providers : UUIDTable("providers") {
    val organizationId = reference("organization_id", Organizations.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 120)
    val addressId = reference("address_id", Addresses.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val contactName = varchar("contact_name", 100).nullable()
    val contactPhone = varchar("contact_phone", 50).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
