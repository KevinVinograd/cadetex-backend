package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Couriers : UUIDTable("couriers") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE).nullable()
    val organizationId =
            reference("organization_id", Organizations.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val phoneNumber = varchar("phone_number", 50)
    val address = varchar("address", 255).nullable()
    val vehicleType = varchar("vehicle_type", 50).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
