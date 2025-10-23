package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Tasks : UUIDTable("tasks") {
    val organizationId = reference("organization_id", Organizations.id, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 20)
    val referenceNumber = varchar("reference_number", 50).nullable()
    val clientId = reference("client_id", Clients.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val providerId = reference("provider_id", Providers.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val addressOverride = varchar("address_override", 255).nullable()
    val courierId = reference("courier_id", Couriers.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val status = varchar("status", 30)
    val priority = varchar("priority", 10)
    val scheduledDate = varchar("scheduled_date", 10).nullable()
    val notes = text("notes").nullable()
    val mbl = varchar("mbl", 50).nullable()
    val hbl = varchar("hbl", 50).nullable()
    val freightCert = bool("freight_cert").default(false)
    val foCert = bool("fo_cert").default(false)
    val bunkerCert = bool("bunker_cert").default(false)
    val linkedTaskId = reference("linked_task_id", id, onDelete = ReferenceOption.SET_NULL).nullable()
    val receiptPhotoUrl = text("receipt_photo_url").nullable()
    val photoRequired = bool("photo_required").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
