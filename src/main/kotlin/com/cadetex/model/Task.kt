package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

// Address is in the same package, no import needed

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
    val addressOverrideId: String? = null,
    val contact: String? = null,
    val courierId: String? = null,
    val courierName: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val courierNotes: String? = null,
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

// DTO para respuestas que incluye address calculado
@Serializable
data class TaskResponse(
    val id: String,
    val organizationId: String,
    val type: TaskType,
    val referenceNumber: String? = null,
    val clientId: String? = null,
    val clientName: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val addressOverrideId: String? = null,
    val address: Address? = null, // Calculado dinámicamente: override, o client address, o provider address
    val contact: String? = null,
    val courierId: String? = null,
    val courierName: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val courierNotes: String? = null,
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
) {
    companion object {
        fun fromTask(task: Task, address: Address?): TaskResponse {
            return TaskResponse(
                id = task.id,
                organizationId = task.organizationId,
                type = task.type,
                referenceNumber = task.referenceNumber,
                clientId = task.clientId,
                clientName = task.clientName,
                providerId = task.providerId,
                providerName = task.providerName,
                addressOverrideId = task.addressOverrideId,
                address = address,
                contact = task.contact,
                courierId = task.courierId,
                courierName = task.courierName,
                status = task.status,
                priority = task.priority,
                scheduledDate = task.scheduledDate,
                notes = task.notes,
                courierNotes = task.courierNotes,
                mbl = task.mbl,
                hbl = task.hbl,
                freightCert = task.freightCert,
                foCert = task.foCert,
                bunkerCert = task.bunkerCert,
                linkedTaskId = task.linkedTaskId,
                receiptPhotoUrl = task.receiptPhotoUrl,
                photoRequired = task.photoRequired,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt
            )
        }
    }
}

@Serializable
data class CreateTaskRequest(
    val organizationId: String,
    val type: TaskType,
    val referenceNumber: String? = null,
    val clientId: String? = null,
    val providerId: String? = null,
    val addressOverride: Address? = null, // Si se proporciona, se crea address y se asigna a addressOverrideId
    val contact: String? = null,
    val courierId: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val courierNotes: String? = null,
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
    val addressOverride: Address? = null, // Si se proporciona, se crea/actualiza address y se asigna a addressOverrideId
    val contact: String? = null,
    val courierId: String? = null,
    val unassignCourier: Boolean? = null, // Explicit flag to unassign courier
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val scheduledDate: String? = null,
    val notes: String? = null,
    val courierNotes: String? = null,
    val photoRequired: Boolean? = null,
    val mbl: String? = null,
    val hbl: String? = null,
    val freightCert: Boolean? = null,
    val foCert: Boolean? = null,
    val bunkerCert: Boolean? = null,
    val linkedTaskId: String? = null,
    val receiptPhotoUrl: String? = null
)
