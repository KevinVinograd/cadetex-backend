package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TaskPhoto(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val photoUrl: String,
    val photoType: String = "ADDITIONAL",
    val createdAt: String? = null
)

@Serializable
data class CreateTaskPhotoRequest(
    val taskId: String,
    val photoUrl: String,
    val photoType: String = "ADDITIONAL"
)

@Serializable
data class UpdateTaskPhotoRequest(
    val photoUrl: String? = null,
    val photoType: String? = null
)

