package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class TaskType {
    RETIRE, DELIVER
}

@Serializable
enum class TaskStatus {
    PENDING, PENDING_CONFIRMATION, CONFIRMED, COMPLETED, CANCELLED
}

@Serializable
enum class TaskPriority {
    NORMAL, URGENT
}

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val type: TaskType,
    val referenceNumber: String? = null,
    val clientId: String? = null,
    val clientName: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val addressOverride: String? = null,
    val city: String? = null,
    val province: String? = null,
    val contact: String? = null,
    val courierId: String? = null,
    val courierName: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val mbl: String? = null,
    val hbl: String? = null,
    val freightCert: Boolean = false,
    val foCert: Boolean = false,
    val bunkerCert: Boolean = false,
    val linkedTaskId: String? = null,
    val receiptPhotoUrl: String? = null,
    val photoRequired: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateTaskRequest(
    val organizationId: String,
    val type: TaskType,
    val referenceNumber: String? = null,
    val clientId: String? = null,
    val providerId: String? = null,
    val addressOverride: String? = null,
    val city: String? = null,
    val province: String? = null,
    val contact: String? = null,
    val courierId: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val mbl: String? = null,
    val hbl: String? = null,
    val freightCert: Boolean = false,
    val foCert: Boolean = false,
    val bunkerCert: Boolean = false,
    val linkedTaskId: String? = null,
    val receiptPhotoUrl: String? = null,
    val photoRequired: Boolean = false
)

@Serializable
data class UpdateTaskRequest(
    val type: TaskType? = null,
    val referenceNumber: String? = null,
    val clientId: String? = null,
    val providerId: String? = null,
    val addressOverride: String? = null,
    val city: String? = null,
    val province: String? = null,
    val contact: String? = null,
    val courierId: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val photoRequired: Boolean? = null,
    val mbl: String? = null,
    val hbl: String? = null,
    val freightCert: Boolean? = null,
    val foCert: Boolean? = null,
    val bunkerCert: Boolean? = null,
    val linkedTaskId: String? = null,
    val receiptPhotoUrl: String? = null
)
