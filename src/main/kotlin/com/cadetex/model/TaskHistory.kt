package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TaskHistory(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val changedBy: String? = null,
    val changedAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateTaskHistoryRequest(
    val taskId: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val changedBy: String? = null
)

@Serializable
data class UpdateTaskHistoryRequest(
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val changedBy: String? = null
)
