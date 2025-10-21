package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TaskPhotos : UUIDTable("task_photos") {
    val taskId = reference("task_id", Tasks.id, onDelete = ReferenceOption.CASCADE)
    val photoUrl = text("photo_url")
    val description = text("description").nullable()
    val uploadedAt = timestamp("uploaded_at")
    val updatedAt = timestamp("updated_at")
}