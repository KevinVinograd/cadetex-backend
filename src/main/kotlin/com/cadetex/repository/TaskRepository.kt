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

    fun rowToTask(row: ResultRow) =
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

    fun rowToTaskWithNames(row: ResultRow) =
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

    suspend fun allTasks(): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().map(::rowToTask)
    }

    suspend fun tasksByOrganization(organizationId: String): List<Task> = newSuspendedTransaction {
        validateUUID(organizationId, "organization ID")
        val tasks = Tasks.selectAll().where { Tasks.organizationId eq UUID.fromString(organizationId) }.map(::rowToTask)
        
        // Enriquecer con nombres
        tasks.map { task ->
            var clientName: String? = null
            var providerName: String? = null
            
            // Si tiene cliente, obtener nombre del cliente
            if (task.clientId != null) {
                clientName = Clients.selectAll()
                    .where { Clients.id eq UUID.fromString(task.clientId) }
                    .singleOrNull()?.get(Clients.name)
            }
            
            // Si tiene proveedor, obtener nombre del proveedor
            if (task.providerId != null) {
                providerName = Providers.selectAll()
                    .where { Providers.id eq UUID.fromString(task.providerId) }
                    .singleOrNull()?.get(Providers.name)
            }
            
            // Obtener nombre del cadete
            val courierName = task.courierId?.let { courierId ->
                Couriers.selectAll()
                    .where { Couriers.id eq UUID.fromString(courierId) }
                    .singleOrNull()?.get(Couriers.name)
            }
            
            task.copy(
                clientName = clientName,
                providerName = providerName,
                courierName = courierName
            )
        }
    }

    suspend fun tasksByCourier(courierId: String): List<Task> = newSuspendedTransaction {
        validateUUID(courierId, "courier ID")
        val tasks = Tasks.selectAll().where { Tasks.courierId eq UUID.fromString(courierId) }.map(::rowToTask)
        
        // Enriquecer con nombres
        tasks.map { task ->
            val clientName = task.clientId?.let { clientId ->
                Clients.selectAll().where { Clients.id eq UUID.fromString(clientId) }.singleOrNull()?.get(Clients.name)
            }
            val providerName = task.providerId?.let { providerId ->
                Providers.selectAll().where { Providers.id eq UUID.fromString(providerId) }.singleOrNull()?.get(Providers.name)
            }
            val courierName = task.courierId?.let { courierId ->
                Couriers.selectAll().where { Couriers.id eq UUID.fromString(courierId) }.singleOrNull()?.get(Couriers.name)
            }
            
            task.copy(
                clientName = clientName,
                providerName = providerName,
                courierName = courierName
            )
        }
    }

    suspend fun tasksByStatus(status: TaskStatus): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().where { Tasks.status eq status.name }.map(::rowToTask)
    }

    suspend fun findById(id: String): Task? = newSuspendedTransaction {
        validateUUID(id, "task ID")
        val task = Tasks.selectAll().where { Tasks.id eq UUID.fromString(id) }.map(::rowToTask).singleOrNull()
        
        // Enriquecer con nombres si la tarea existe
        task?.let { t ->
            val clientName = t.clientId?.let { clientId ->
                Clients.selectAll().where { Clients.id eq UUID.fromString(clientId) }.singleOrNull()?.get(Clients.name)
            }
            val providerName = t.providerId?.let { providerId ->
                Providers.selectAll().where { Providers.id eq UUID.fromString(providerId) }.singleOrNull()?.get(Providers.name)
            }
            val courierName = t.courierId?.let { courierId ->
                Couriers.selectAll().where { Couriers.id eq UUID.fromString(courierId) }.singleOrNull()?.get(Couriers.name)
            }
            
            t.copy(
                clientName = clientName,
                providerName = providerName,
                courierName = courierName
            )
        }
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
                            // Manejar clientId y providerId - solo uno puede estar presente
                            if (updateRequest.clientId != null) {
                                row[Tasks.clientId] = UUID.fromString(updateRequest.clientId)
                                row[Tasks.providerId] = null // Limpiar providerId cuando se establece clientId
                            } else if (updateRequest.providerId != null) {
                                row[Tasks.providerId] = UUID.fromString(updateRequest.providerId)
                                row[Tasks.clientId] = null // Limpiar clientId cuando se establece providerId
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
}