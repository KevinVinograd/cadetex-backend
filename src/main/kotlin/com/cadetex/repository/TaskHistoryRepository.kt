package com.cadetex.repository

import com.cadetex.database.tables.TaskHistory
import com.cadetex.model.TaskHistory as TaskHistoryModel
import com.cadetex.model.CreateTaskHistoryRequest
import com.cadetex.model.UpdateTaskHistoryRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.datetime.Clock
import java.util.*

class TaskHistoryRepository {

    suspend fun allTaskHistory(): List<TaskHistoryModel> = newSuspendedTransaction {
        TaskHistory.selectAll().map(::rowToTaskHistory)
    }

    suspend fun findById(id: String): TaskHistoryModel? = newSuspendedTransaction {
        TaskHistory
            .selectAll()
            .where { TaskHistory.id eq UUID.fromString(id) }
            .map(::rowToTaskHistory)
            .singleOrNull()
    }

    suspend fun findByTaskId(taskId: String): List<TaskHistoryModel> = newSuspendedTransaction {
        TaskHistory
            .selectAll()
            .where { TaskHistory.taskId eq UUID.fromString(taskId) }
            .map(::rowToTaskHistory)
    }

    suspend fun create(request: CreateTaskHistoryRequest): TaskHistoryModel = newSuspendedTransaction {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        TaskHistory.insert {
            it[TaskHistory.id] = id
            it[TaskHistory.taskId] = UUID.fromString(request.taskId)
            it[TaskHistory.previousStatus] = request.previousStatus
            it[TaskHistory.newStatus] = request.newStatus
            it[TaskHistory.changedBy] = request.changedBy?.let { UUID.fromString(it) }
            it[TaskHistory.changedAt] = now
            it[TaskHistory.updatedAt] = now
        }

        TaskHistoryModel(
            id = id.toString(),
            taskId = request.taskId,
            previousStatus = request.previousStatus,
            newStatus = request.newStatus,
            changedBy = request.changedBy,
            changedAt = now.toString(),
            updatedAt = now.toString()
        )
    }

    suspend fun update(id: String, updateRequest: UpdateTaskHistoryRequest): TaskHistoryModel? = newSuspendedTransaction {
        val now = Clock.System.now()

        val updated = TaskHistory.update({ TaskHistory.id eq UUID.fromString(id) }) { row ->
            updateRequest.previousStatus?.let { newPreviousStatus -> row[TaskHistory.previousStatus] = newPreviousStatus }
            updateRequest.newStatus?.let { newNewStatus -> row[TaskHistory.newStatus] = newNewStatus }
            updateRequest.changedBy?.let { newChangedBy -> row[TaskHistory.changedBy] = UUID.fromString(newChangedBy) }
            row[TaskHistory.updatedAt] = now
        }

        if (updated > 0) findById(id) else null
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        TaskHistory.deleteWhere { TaskHistory.id eq UUID.fromString(id) } > 0
    }

    private fun rowToTaskHistory(row: ResultRow) = TaskHistoryModel(
        id = row[TaskHistory.id].value.toString(),
        taskId = row[TaskHistory.taskId].value.toString(),
        previousStatus = row[TaskHistory.previousStatus],
        newStatus = row[TaskHistory.newStatus],
        changedBy = row[TaskHistory.changedBy]?.value?.toString(),
        changedAt = row[TaskHistory.changedAt].toString(),
        updatedAt = row[TaskHistory.updatedAt].toString()
    )
}