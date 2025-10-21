package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Organizations : UUIDTable("organizations") {
    val name = varchar("name", 120)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
