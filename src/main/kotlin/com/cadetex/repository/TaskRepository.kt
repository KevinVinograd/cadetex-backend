package com.cadetex.repository

import com.cadetex.database.tables.Tasks
import com.cadetex.database.tables.Clients
import com.cadetex.database.tables.Providers
import com.cadetex.database.tables.Couriers
import com.cadetex.model.Task
import com.cadetex.model.TaskStatus
import com.cadetex.model.UpdateTaskRequest
import java.util.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class TaskRepository {

    private fun isValidUUID(uuidString: String): Boolean {
        return try {
            UUID.fromString(uuidString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun validateUUID(uuidString: String, fieldName: String) {
        if (!isValidUUID(uuidString)) {
            throw IllegalArgumentException("Invalid UUID format for $fieldName: $uuidString")
        }
    }

    suspend fun allTasks(): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().map(::rowToTask)
    }

    suspend fun tasksByOrganization(organizationId: String): List<Task> = newSuspendedTransaction {
        validateUUID(organizationId, "organization ID")
        Tasks
                .leftJoin(Clients, { Tasks.clientId }, { Clients.id })
                .leftJoin(Providers, { Tasks.providerId }, { Providers.id })
                .leftJoin(Couriers, { Tasks.courierId }, { Couriers.id })
                .select(
                    Tasks.id,
                    Tasks.organizationId,
                    Tasks.type,
                    Tasks.referenceNumber,
                    Tasks.clientId,
                    Clients.name,
                    Tasks.providerId,
                    Providers.name,
                    Tasks.addressOverride,
                    Tasks.city,
                    Tasks.province,
                    Tasks.contact,
                    Tasks.courierId,
                    Couriers.name,
                    Tasks.status,
                    Tasks.priority,
                    Tasks.scheduledDate,
                    Tasks.notes,
                    Tasks.mbl,
                    Tasks.hbl,
                    Tasks.freightCert,
                    Tasks.foCert,
                    Tasks.bunkerCert,
                    Tasks.linkedTaskId,
                    Tasks.receiptPhotoUrl,
                    Tasks.photoRequired,
                    Tasks.createdAt,
                    Tasks.updatedAt
                )
                .where { Tasks.organizationId eq UUID.fromString(organizationId) }
                .map(::rowToTaskWithNames)
    }

    suspend fun tasksByCourier(courierId: String): List<Task> = newSuspendedTransaction {
        validateUUID(courierId, "courier ID")
        Tasks
                .leftJoin(Clients, { Tasks.clientId }, { Clients.id })
                .leftJoin(Providers, { Tasks.providerId }, { Providers.id })
                .leftJoin(Couriers, { Tasks.courierId }, { Couriers.id })
                .select(
                    Tasks.id,
                    Tasks.organizationId,
                    Tasks.type,
                    Tasks.referenceNumber,
                    Tasks.clientId,
                    Clients.name,
                    Tasks.providerId,
                    Providers.name,
                    Tasks.addressOverride,
                    Tasks.city,
                    Tasks.province,
                    Tasks.contact,
                    Tasks.courierId,
                    Couriers.name,
                    Tasks.status,
                    Tasks.priority,
                    Tasks.scheduledDate,
                    Tasks.notes,
                    Tasks.mbl,
                    Tasks.hbl,
                    Tasks.freightCert,
                    Tasks.foCert,
                    Tasks.bunkerCert,
                    Tasks.linkedTaskId,
                    Tasks.receiptPhotoUrl,
                    Tasks.photoRequired,
                    Tasks.createdAt,
                    Tasks.updatedAt
                )
                .where { Tasks.courierId eq UUID.fromString(courierId) }
                .map(::rowToTaskWithNames)
    }

    suspend fun tasksByStatus(status: TaskStatus): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().where { Tasks.status eq status.name }.map(::rowToTask)
    }

    suspend fun findById(id: String): Task? = newSuspendedTransaction {
        validateUUID(id, "task ID")
        Tasks.selectAll().where { Tasks.id eq UUID.fromString(id) }.map(::rowToTask).singleOrNull()
    }

    suspend fun create(task: Task): Task = newSuspendedTransaction {
        validateUUID(task.organizationId, "organization ID")
        task.clientId?.let { validateUUID(it, "client ID") }
        task.providerId?.let { validateUUID(it, "provider ID") }
        task.courierId?.let { validateUUID(it, "courier ID") }
        task.linkedTaskId?.let { validateUUID(it, "linked task ID") }

        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Tasks.insert {
            it[Tasks.id] = id
            it[organizationId] = UUID.fromString(task.organizationId)
            it[type] = task.type.name
            it[referenceNumber] = task.referenceNumber
            it[clientId] = task.clientId?.let { UUID.fromString(it) }
            it[providerId] = task.providerId?.let { UUID.fromString(it) }
            it[addressOverride] = task.addressOverride
            it[city] = task.city
            it[province] = task.province
            it[contact] = task.contact
            it[courierId] = task.courierId?.let { UUID.fromString(it) }
            it[status] = task.status.name
            it[priority] = task.priority.name
            it[scheduledDate] = task.scheduledDate
            it[notes] = task.notes
            it[mbl] = task.mbl
            it[hbl] = task.hbl
            it[freightCert] = task.freightCert
            it[foCert] = task.foCert
            it[bunkerCert] = task.bunkerCert
            it[linkedTaskId] = task.linkedTaskId?.let { UUID.fromString(it) }
            it[receiptPhotoUrl] = task.receiptPhotoUrl
            it[photoRequired] = task.photoRequired
            it[createdAt] = now
            it[updatedAt] = now
        }

        task.copy(id = id.toString(), createdAt = now.toString(), updatedAt = now.toString())
    }

    suspend fun update(id: String, updateRequest: UpdateTaskRequest): Task? =
            newSuspendedTransaction {
                val now = Clock.System.now()

                val updated =
                        Tasks.update({ Tasks.id eq UUID.fromString(id) }) { row ->
                            updateRequest.type?.let { newType -> row[Tasks.type] = newType.name }
                            updateRequest.referenceNumber?.let { newRefNumber ->
                                row[Tasks.referenceNumber] = newRefNumber
                            }
                            updateRequest.clientId?.let { newClientId ->
                                row[Tasks.clientId] = UUID.fromString(newClientId)
                            }
                            updateRequest.providerId?.let { newProviderId ->
                                row[Tasks.providerId] = UUID.fromString(newProviderId)
                            }
                            updateRequest.addressOverride?.let { newAddressOverride ->
                                row[Tasks.addressOverride] = newAddressOverride
                            }
                            updateRequest.city?.let { newCity ->
                                row[Tasks.city] = newCity
                            }
                            updateRequest.province?.let { newProvince ->
                                row[Tasks.province] = newProvince
                            }
                            updateRequest.contact?.let { newContact ->
                                row[Tasks.contact] = newContact
                            }
                            updateRequest.courierId?.let { newCourierId ->
                                row[Tasks.courierId] = UUID.fromString(newCourierId)
                            }
                            updateRequest.status?.let { newStatus ->
                                row[Tasks.status] = newStatus.name
                            }
                            updateRequest.priority?.let { newPriority ->
                                row[Tasks.priority] = newPriority.name
                            }
                            updateRequest.scheduledDate?.let { newScheduledDate ->
                                row[Tasks.scheduledDate] = newScheduledDate
                            }
                            updateRequest.notes?.let { newNotes -> row[Tasks.notes] = newNotes }
                            updateRequest.photoRequired?.let { newPhotoRequired ->
                                row[Tasks.photoRequired] = newPhotoRequired
                            }
                            updateRequest.mbl?.let { newMbl -> row[Tasks.mbl] = newMbl }
                            updateRequest.hbl?.let { newHbl -> row[Tasks.hbl] = newHbl }
                            updateRequest.freightCert?.let { newFreightCert ->
                                row[Tasks.freightCert] = newFreightCert
                            }
                            updateRequest.foCert?.let { newFoCert -> row[Tasks.foCert] = newFoCert }
                            updateRequest.bunkerCert?.let { newBunkerCert ->
                                row[Tasks.bunkerCert] = newBunkerCert
                            }
                            updateRequest.linkedTaskId?.let { newLinkedTaskId ->
                                row[Tasks.linkedTaskId] = UUID.fromString(newLinkedTaskId)
                            }
                            row[Tasks.updatedAt] = now
                        }

                if (updated > 0) findById(id) else null
            }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        Tasks.deleteWhere { Tasks.id eq UUID.fromString(id) } > 0
    }

    private fun rowToTask(row: ResultRow) =
            Task(
                    id = row[Tasks.id].value.toString(),
                    organizationId = row[Tasks.organizationId].value.toString(),
                    type = com.cadetex.model.TaskType.valueOf(row[Tasks.type]),
                    referenceNumber = row[Tasks.referenceNumber],
                    clientId = row[Tasks.clientId]?.value?.toString(),
                    providerId = row[Tasks.providerId]?.value?.toString(),
                    addressOverride = row[Tasks.addressOverride],
                    city = row[Tasks.city],
                    province = row[Tasks.province],
                    contact = row[Tasks.contact],
                    courierId = row[Tasks.courierId]?.value?.toString(),
                    status = TaskStatus.valueOf(row[Tasks.status]),
                    priority = com.cadetex.model.TaskPriority.valueOf(row[Tasks.priority]),
                    scheduledDate = row[Tasks.scheduledDate],
                    notes = row[Tasks.notes],
                    mbl = row[Tasks.mbl],
                    hbl = row[Tasks.hbl],
                    freightCert = row[Tasks.freightCert],
                    foCert = row[Tasks.foCert],
                    bunkerCert = row[Tasks.bunkerCert],
                    linkedTaskId = row[Tasks.linkedTaskId]?.value?.toString(),
                    receiptPhotoUrl = row[Tasks.receiptPhotoUrl],
                    photoRequired = row[Tasks.photoRequired],
                    createdAt = row[Tasks.createdAt].toString(),
                    updatedAt = row[Tasks.updatedAt].toString()
            )

    private fun rowToTaskWithNames(row: ResultRow) =
            Task(
                    id = row[Tasks.id].value.toString(),
                    organizationId = row[Tasks.organizationId].value.toString(),
                    type = com.cadetex.model.TaskType.valueOf(row[Tasks.type]),
                    referenceNumber = row[Tasks.referenceNumber],
                    clientId = row[Tasks.clientId]?.value?.toString(),
                    clientName = row[Clients.name],
                    providerId = row[Tasks.providerId]?.value?.toString(),
                    providerName = row[Providers.name],
                    addressOverride = row[Tasks.addressOverride],
                    city = row[Tasks.city],
                    province = row[Tasks.province],
                    contact = row[Tasks.contact],
                    courierId = row[Tasks.courierId]?.value?.toString(),
                    courierName = row[Couriers.name],
                    status = TaskStatus.valueOf(row[Tasks.status]),
                    priority = com.cadetex.model.TaskPriority.valueOf(row[Tasks.priority]),
                    scheduledDate = row[Tasks.scheduledDate],
                    notes = row[Tasks.notes],
                    mbl = row[Tasks.mbl],
                    hbl = row[Tasks.hbl],
                    freightCert = row[Tasks.freightCert],
                    foCert = row[Tasks.foCert],
                    bunkerCert = row[Tasks.bunkerCert],
                    linkedTaskId = row[Tasks.linkedTaskId]?.value?.toString(),
                    receiptPhotoUrl = row[Tasks.receiptPhotoUrl],
                    photoRequired = row[Tasks.photoRequired],
                    createdAt = row[Tasks.createdAt].toString(),
                    updatedAt = row[Tasks.updatedAt].toString()
            )
}
