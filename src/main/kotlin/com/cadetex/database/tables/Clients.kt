package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Clients : UUIDTable("clients") {
    val organizationId = reference("organization_id", Organizations.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 120)
    val address = varchar("address", 255)
    val city = varchar("city", 80).nullable()
    val province = varchar("province", 80).nullable()
    val contactName = varchar("contact_name", 100).nullable()
    val contactPhone = varchar("contact_phone", 50).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
