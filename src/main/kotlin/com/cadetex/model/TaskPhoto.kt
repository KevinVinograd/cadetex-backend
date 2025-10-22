package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TaskPhoto(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val photoUrl: String,
    val description: String? = null,
    val uploadedAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateTaskPhotoRequest(
    val taskId: String,
    val photoUrl: String,
    val description: String? = null
)

@Serializable
data class UpdateTaskPhotoRequest(
    val photoUrl: String? = null,
    val description: String? = null
)

