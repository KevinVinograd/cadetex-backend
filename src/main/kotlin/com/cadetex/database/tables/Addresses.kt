package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Addresses : UUIDTable("addresses") {
    val street = varchar("street", 200).nullable()
    val streetNumber = varchar("street_number", 20).nullable()
    val addressComplement = varchar("address_complement", 100).nullable()
    val city = varchar("city", 80).nullable()
    val province = varchar("province", 80).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

