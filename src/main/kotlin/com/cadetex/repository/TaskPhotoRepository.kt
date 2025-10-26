package com.cadetex.repository

import com.cadetex.database.tables.TaskPhotos
import com.cadetex.model.TaskPhoto
import com.cadetex.model.CreateTaskPhotoRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class TaskPhotoRepository {

    suspend fun allTaskPhotos(): List<TaskPhoto> = newSuspendedTransaction {
        TaskPhotos.selectAll().map(::rowToTaskPhoto)
    }

    suspend fun findById(id: String): TaskPhoto? = newSuspendedTransaction {
        TaskPhotos
            .selectAll()
            .where { TaskPhotos.id eq UUID.fromString(id) }
            .map(::rowToTaskPhoto)
            .singleOrNull()
    }

    suspend fun findByTaskId(taskId: String): List<TaskPhoto> = newSuspendedTransaction {
        TaskPhotos
            .selectAll()
            .where { TaskPhotos.taskId eq UUID.fromString(taskId) }
            .map(::rowToTaskPhoto)
    }

    suspend fun create(request: CreateTaskPhotoRequest): TaskPhoto = newSuspendedTransaction {
        val id = UUID.randomUUID()

        TaskPhotos.insert {
            it[TaskPhotos.id] = id
            it[TaskPhotos.taskId] = UUID.fromString(request.taskId)
            it[TaskPhotos.photoUrl] = request.photoUrl
            it[TaskPhotos.photoType] = request.photoType
        }

        TaskPhoto(
            id = id.toString(),
            taskId = request.taskId,
            photoUrl = request.photoUrl,
            photoType = request.photoType
        )
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        TaskPhotos.deleteWhere { TaskPhotos.id eq UUID.fromString(id) } > 0
    }

    private fun rowToTaskPhoto(row: ResultRow) = TaskPhoto(
        id = row[TaskPhotos.id].value.toString(),
        taskId = row[TaskPhotos.taskId].value.toString(),
        photoUrl = row[TaskPhotos.photoUrl],
        photoType = row[TaskPhotos.photoType],
        createdAt = row[TaskPhotos.createdAt].toString()
    )
}