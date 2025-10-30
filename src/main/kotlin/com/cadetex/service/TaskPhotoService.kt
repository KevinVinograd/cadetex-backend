package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.TaskPhotoRepository
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("TaskPhotoService")

/**
 * Service para lógica de negocio de Task Photos
 * Maneja validaciones y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class TaskPhotoService(
    private val taskPhotoRepository: TaskPhotoRepository = TaskPhotoRepository()
) {

    /**
     * Buscar todas las task photos
     */
    suspend fun findAll(): Result<List<TaskPhoto>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val photos = taskPhotoRepository.findAll()
            success(photos)
        } catch (e: Exception) {
            logger.error("Error buscando task photos: ${e.message}", e)
            error("Error al buscar task photos: ${e.message}")
        }
    }

    /**
     * Buscar task photo por ID
     */
    suspend fun findById(id: String): Result<TaskPhoto> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val photo = taskPhotoRepository.findById(uuid)
            if (photo != null) {
                success(photo)
            } else {
                error("Task photo no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task photo inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando task photo: ${e.message}", e)
            error("Error al buscar la task photo: ${e.message}")
        }
    }

    /**
     * Buscar task photos por taskId
     */
    suspend fun findByTaskId(taskId: String): Result<List<TaskPhoto>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(taskId)
            val photos = taskPhotoRepository.findByTaskId(uuid)
            success(photos)
        } catch (e: IllegalArgumentException) {
            error("ID de task inválido: $taskId")
        } catch (e: Exception) {
            logger.error("Error buscando task photos por taskId: ${e.message}", e)
            error("Error al buscar task photos: ${e.message}")
        }
    }

    /**
     * Crear nueva task photo
     * Lógica de negocio: validaciones
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateTaskPhotoRequest): Result<TaskPhoto> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val taskId = UUID.fromString(request.taskId)

            // Validación: photoUrl no puede estar vacío
            if (request.photoUrl.isBlank()) {
                return@newSuspendedTransaction error("La URL de la foto es obligatoria")
            }

            // Validación: photoType debe ser válido
            val validPhotoTypes = listOf("ADDITIONAL", "RECEIPT", "OTHER")
            if (request.photoType !in validPhotoTypes) {
                return@newSuspendedTransaction error("Tipo de foto inválido. Debe ser uno de: ${validPhotoTypes.joinToString(", ")}")
            }

            // Insertar task photo
            val photoId = taskPhotoRepository.insert(
                taskId = taskId,
                photoUrl = request.photoUrl.trim(),
                photoType = request.photoType,
                createdAt = kotlinx.datetime.Clock.System.now()
            )

            // Obtener la task photo creada
            val createdPhoto = taskPhotoRepository.findById(photoId)
            if (createdPhoto != null) {
                success(createdPhoto)
            } else {
                error("Error al recuperar la task photo creada")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando task photo: ${e.message}", e)
            error("Error al crear la task photo: ${e.message}")
        }
    }

    /**
     * Actualizar task photo existente
     * Lógica de negocio: validaciones
     * Todo en una sola transacción
     */
    suspend fun update(id: String, request: com.cadetex.model.UpdateTaskPhotoRequest): Result<TaskPhoto> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            
            // Verificar que la task photo existe
            val existingPhoto = taskPhotoRepository.findById(uuid)
            if (existingPhoto == null) {
                return@newSuspendedTransaction error("Task photo no encontrada con ID: $id")
            }

            // Validación: photoUrl no puede estar vacío si se proporciona
            if (request.photoUrl != null && request.photoUrl.isBlank()) {
                return@newSuspendedTransaction error("La URL de la foto no puede estar vacía")
            }

            // Validación: photoType debe ser válido si se proporciona
            if (request.photoType != null) {
                val validPhotoTypes = listOf("ADDITIONAL", "RECEIPT", "OTHER")
                if (request.photoType !in validPhotoTypes) {
                    return@newSuspendedTransaction error("Tipo de foto inválido. Debe ser uno de: ${validPhotoTypes.joinToString(", ")}")
                }
            }

            // Actualizar task photo
            val updated = taskPhotoRepository.update(
                id = uuid,
                photoUrl = request.photoUrl?.trim(),
                photoType = request.photoType
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar la task photo")
            }

            // Obtener la task photo actualizada
            val updatedPhoto = taskPhotoRepository.findById(uuid)
            if (updatedPhoto != null) {
                success(updatedPhoto)
            } else {
                error("Error al recuperar la task photo actualizada")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task photo inválido: $id")
        } catch (e: Exception) {
            logger.error("Error actualizando task photo: ${e.message}", e)
            error("Error al actualizar la task photo: ${e.message}")
        }
    }

    /**
     * Eliminar task photo
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = taskPhotoRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Task photo no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task photo inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando task photo: ${e.message}", e)
            error("Error al eliminar la task photo: ${e.message}")
        }
    }
}

