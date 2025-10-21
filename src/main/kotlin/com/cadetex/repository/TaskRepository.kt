package com.cadetex.repository

import com.cadetex.database.tables.Tasks
import com.cadetex.model.Task
import com.cadetex.model.TaskStatus
import com.cadetex.model.UpdateTaskRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.datetime.Clock
import java.util.*

class TaskRepository {

    suspend fun allTasks(): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().map(::rowToTask)
    }

    suspend fun tasksByOrganization(organizationId: String): List<Task> = newSuspendedTransaction {
        Tasks
            .selectAll()
            .where { Tasks.organizationId eq UUID.fromString(organizationId) }
            .map(::rowToTask)
    }

    suspend fun tasksByCourier(courierId: String): List<Task> = newSuspendedTransaction {
        Tasks
            .selectAll()
            .where { Tasks.courierId eq UUID.fromString(courierId) }
            .map(::rowToTask)
    }

    suspend fun tasksByStatus(status: TaskStatus): List<Task> = newSuspendedTransaction {
        Tasks
            .selectAll()
            .where { Tasks.status eq status.name }
            .map(::rowToTask)
    }

    suspend fun findById(id: String): Task? = newSuspendedTransaction {
        Tasks
            .selectAll()
            .where { Tasks.id eq UUID.fromString(id) }
            .map(::rowToTask)
            .singleOrNull()
    }

    suspend fun create(task: Task): Task = newSuspendedTransaction {
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
            it[createdAt] = now
            it[updatedAt] = now
        }

        task.copy(
            id = id.toString(),
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
    }

    suspend fun update(id: String, updateRequest: UpdateTaskRequest): Task? = newSuspendedTransaction {
        val now = Clock.System.now()

        val updated = Tasks.update({ Tasks.id eq UUID.fromString(id) }) { row ->
            updateRequest.type?.let { newType -> row[Tasks.type] = newType.name }
            updateRequest.referenceNumber?.let { newRefNumber -> row[Tasks.referenceNumber] = newRefNumber }
            updateRequest.clientId?.let { newClientId -> row[Tasks.clientId] = UUID.fromString(newClientId) }
            updateRequest.providerId?.let { newProviderId -> row[Tasks.providerId] = UUID.fromString(newProviderId) }
            updateRequest.addressOverride?.let { newAddressOverride -> row[Tasks.addressOverride] = newAddressOverride }
            updateRequest.courierId?.let { newCourierId -> row[Tasks.courierId] = UUID.fromString(newCourierId) }
            updateRequest.status?.let { newStatus -> row[Tasks.status] = newStatus.name }
            updateRequest.priority?.let { newPriority -> row[Tasks.priority] = newPriority.name }
            updateRequest.scheduledDate?.let { newScheduledDate -> row[Tasks.scheduledDate] = newScheduledDate }
            updateRequest.notes?.let { newNotes -> row[Tasks.notes] = newNotes }
            updateRequest.mbl?.let { newMbl -> row[Tasks.mbl] = newMbl }
            updateRequest.hbl?.let { newHbl -> row[Tasks.hbl] = newHbl }
            updateRequest.freightCert?.let { newFreightCert -> row[Tasks.freightCert] = newFreightCert }
            updateRequest.foCert?.let { newFoCert -> row[Tasks.foCert] = newFoCert }
            updateRequest.bunkerCert?.let { newBunkerCert -> row[Tasks.bunkerCert] = newBunkerCert }
            updateRequest.linkedTaskId?.let { newLinkedTaskId -> row[Tasks.linkedTaskId] = UUID.fromString(newLinkedTaskId) }
            row[Tasks.updatedAt] = now
        }

        if (updated > 0) findById(id) else null
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        Tasks.deleteWhere { Tasks.id eq UUID.fromString(id) } > 0
    }

    private fun rowToTask(row: ResultRow) = Task(
        id = row[Tasks.id].value.toString(),
        organizationId = row[Tasks.organizationId].value.toString(),
        type = com.cadetex.model.TaskType.valueOf(row[Tasks.type]),
        referenceNumber = row[Tasks.referenceNumber],
        clientId = row[Tasks.clientId]?.value?.toString(),
        providerId = row[Tasks.providerId]?.value?.toString(),
        addressOverride = row[Tasks.addressOverride],
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
        createdAt = row[Tasks.createdAt].toString(),
        updatedAt = row[Tasks.updatedAt].toString()
    )
}
