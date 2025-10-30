package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.TaskHistoryRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("TaskHistoryService")

/**
 * Service para lógica de negocio de Task History
 * Maneja validaciones y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class TaskHistoryService(
    private val taskHistoryRepository: TaskHistoryRepository = TaskHistoryRepository()
) {

    /**
     * Buscar todas las task history
     */
    suspend fun findAll(): Result<List<TaskHistory>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val history = taskHistoryRepository.findAll()
            success(history)
        } catch (e: Exception) {
            logger.error("Error buscando task history: ${e.message}", e)
            error("Error al buscar task history: ${e.message}")
        }
    }

    /**
     * Buscar task history por ID
     */
    suspend fun findById(id: String): Result<TaskHistory> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val history = taskHistoryRepository.findById(uuid)
            if (history != null) {
                success(history)
            } else {
                error("Task history no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task history inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando task history: ${e.message}", e)
            error("Error al buscar la task history: ${e.message}")
        }
    }

    /**
     * Buscar task history por taskId
     */
    suspend fun findByTaskId(taskId: String): Result<List<TaskHistory>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(taskId)
            val history = taskHistoryRepository.findByTaskId(uuid)
            success(history)
        } catch (e: IllegalArgumentException) {
            error("ID de task inválido: $taskId")
        } catch (e: Exception) {
            logger.error("Error buscando task history por taskId: ${e.message}", e)
            error("Error al buscar task history: ${e.message}")
        }
    }

    /**
     * Crear nueva task history
     * Lógica de negocio: validaciones
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateTaskHistoryRequest): Result<TaskHistory> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val taskId = UUID.fromString(request.taskId)
            val changedBy = request.changedBy?.let { UUID.fromString(it) }
            val now = Clock.System.now()

            // Insertar task history
            val historyId = taskHistoryRepository.insert(
                taskId = taskId,
                previousStatus = request.previousStatus,
                newStatus = request.newStatus,
                changedBy = changedBy,
                changedAt = now,
                updatedAt = now
            )

            // Obtener la task history creada
            val createdHistory = taskHistoryRepository.findById(historyId)
            if (createdHistory != null) {
                success(createdHistory)
            } else {
                error("Error al recuperar la task history creada")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando task history: ${e.message}", e)
            error("Error al crear la task history: ${e.message}")
        }
    }

    /**
     * Actualizar task history existente
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateTaskHistoryRequest): Result<TaskHistory> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val historyId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener task history actual
            val currentHistory = taskHistoryRepository.findById(historyId)
            if (currentHistory == null) {
                return@newSuspendedTransaction error("Task history no encontrada con ID: $id")
            }

            // Parsear changedBy si se actualiza
            val changedBy = updateRequest.changedBy?.let { UUID.fromString(it) }

            // Actualizar task history
            val updated = taskHistoryRepository.update(
                id = historyId,
                previousStatus = updateRequest.previousStatus,
                newStatus = updateRequest.newStatus,
                changedBy = changedBy,
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar la task history")
            }

            // Obtener task history actualizada
            val updatedHistory = taskHistoryRepository.findById(historyId)
            if (updatedHistory != null) {
                success(updatedHistory)
            } else {
                error("Error al recuperar la task history actualizada")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task history inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando task history: ${e.message}", e)
            error("Error al actualizar la task history: ${e.message}")
        }
    }

    /**
     * Eliminar task history
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = taskHistoryRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Task history no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task history inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando task history: ${e.message}", e)
            error("Error al eliminar la task history: ${e.message}")
        }
    }
}

