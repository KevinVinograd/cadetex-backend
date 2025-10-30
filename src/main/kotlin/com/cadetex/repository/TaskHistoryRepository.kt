package com.cadetex.repository

import com.cadetex.database.tables.TaskHistory
import com.cadetex.model.TaskHistory as TaskHistoryModel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en TaskHistoryService
 */
class TaskHistoryRepository {

    /**
     * Buscar todas las task history
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findAll(): List<TaskHistoryModel> {
        return TaskHistory.selectAll().map(::rowToTaskHistory)
    }

    /**
     * Buscar task history por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): TaskHistoryModel? {
        return TaskHistory
            .selectAll()
            .where { TaskHistory.id eq id }
            .map(::rowToTaskHistory)
            .singleOrNull()
    }

    /**
     * Buscar task history por taskId
     * Usa índice: task_id tiene índice (si existe)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByTaskId(taskId: UUID): List<TaskHistoryModel> {
        return TaskHistory
            .selectAll()
            .where { TaskHistory.taskId eq taskId }
            .map(::rowToTaskHistory)
    }

    /**
     * Insertar nueva task history
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        taskId: UUID,
        previousStatus: String? = null,
        newStatus: String? = null,
        changedBy: UUID? = null,
        changedAt: kotlinx.datetime.Instant? = null,
        updatedAt: kotlinx.datetime.Instant? = null
    ): UUID {
        val id = UUID.randomUUID()
        val now = changedAt ?: kotlinx.datetime.Clock.System.now()

        TaskHistory.insert {
            it[TaskHistory.id] = id
            it[TaskHistory.taskId] = taskId
            it[TaskHistory.previousStatus] = previousStatus
            it[TaskHistory.newStatus] = newStatus
            it[TaskHistory.changedBy] = changedBy
            it[TaskHistory.changedAt] = now
            it[TaskHistory.updatedAt] = updatedAt ?: now
        }

        return id
    }

    /**
     * Actualizar task history existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        previousStatus: String? = null,
        newStatus: String? = null,
        changedBy: UUID? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = TaskHistory.update({ TaskHistory.id eq id }) { row ->
            previousStatus?.let { row[TaskHistory.previousStatus] = it }
            newStatus?.let { row[TaskHistory.newStatus] = it }
            changedBy?.let { row[TaskHistory.changedBy] = it }
            row[TaskHistory.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar task history
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return TaskHistory.deleteWhere { TaskHistory.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a TaskHistory
     */
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
