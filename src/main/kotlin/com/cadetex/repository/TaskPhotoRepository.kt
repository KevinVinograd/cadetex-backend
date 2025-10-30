package com.cadetex.repository

import com.cadetex.database.tables.TaskPhotos
import com.cadetex.model.TaskPhoto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en TaskPhotoService
 */
class TaskPhotoRepository {

    /**
     * Buscar todas las task photos
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findAll(): List<TaskPhoto> {
        return TaskPhotos.selectAll().map(::rowToTaskPhoto)
    }

    /**
     * Buscar task photo por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): TaskPhoto? {
        return TaskPhotos
            .selectAll()
            .where { TaskPhotos.id eq id }
            .map(::rowToTaskPhoto)
            .singleOrNull()
    }

    /**
     * Buscar task photos por taskId
     * Usa índice: idx_task_photos_task_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByTaskId(taskId: UUID): List<TaskPhoto> {
        return TaskPhotos
            .selectAll()
            .where { TaskPhotos.taskId eq taskId }
            .map(::rowToTaskPhoto)
    }

    /**
     * Insertar nueva task photo
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        taskId: UUID,
        photoUrl: String,
        photoType: String = "ADDITIONAL",
        createdAt: kotlinx.datetime.Instant? = null
    ): UUID {
        val id = UUID.randomUUID()
        val now = createdAt ?: kotlinx.datetime.Clock.System.now()

        TaskPhotos.insert {
            it[TaskPhotos.id] = id
            it[TaskPhotos.taskId] = taskId
            it[TaskPhotos.photoUrl] = photoUrl
            it[TaskPhotos.photoType] = photoType
            it[TaskPhotos.createdAt] = now
        }

        return id
    }

    /**
     * Actualizar task photo existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        photoUrl: String? = null,
        photoType: String? = null
    ): Boolean {
        val updated = TaskPhotos.update({ TaskPhotos.id eq id }) { row ->
            photoUrl?.let { row[TaskPhotos.photoUrl] = it }
            photoType?.let { row[TaskPhotos.photoType] = it }
        }
        return updated > 0
    }

    /**
     * Eliminar task photo
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return TaskPhotos.deleteWhere { TaskPhotos.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a TaskPhoto
     */
    private fun rowToTaskPhoto(row: ResultRow) = TaskPhoto(
        id = row[TaskPhotos.id].value.toString(),
        taskId = row[TaskPhotos.taskId].value.toString(),
        photoUrl = row[TaskPhotos.photoUrl],
        photoType = row[TaskPhotos.photoType],
        createdAt = row[TaskPhotos.createdAt].toString()
    )
}
