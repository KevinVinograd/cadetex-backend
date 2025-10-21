package com.cadetex.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TaskHistory : UUIDTable("task_history") {
    val taskId = reference("task_id", Tasks.id, onDelete = ReferenceOption.CASCADE)
    val previousStatus = varchar("previous_status", 30).nullable()
    val newStatus = varchar("new_status", 30).nullable()
    val changedBy = reference("changed_by", Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val changedAt = timestamp("changed_at")
    val updatedAt = timestamp("updated_at")
}