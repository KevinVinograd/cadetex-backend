package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.TaskRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Route.taskRoutes() {
    val taskRepository = TaskRepository()
    val logger = LoggerFactory.getLogger("TaskRoutes")

    route("/tasks") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                val tasks = taskRepository.tasksByOrganization(organizationId)
                call.respond(tasks)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val task = taskRepository.findById(id)
                if (task != null) {
                    val userData = call.getUserData()
                    // Verificar que la tarea pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                        call.respond(task)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver esta tarea"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val tasks = taskRepository.tasksByOrganization(organizationId)
                    call.respond(tasks)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver tareas de esta organización"))
                }
            }

            get("/courier/{courierId}") {
                val courierId = call.parameters["courierId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                val tasks = taskRepository.tasksByCourier(courierId)
                val filteredTasks = tasks.filter { it.organizationId == organizationId }
                call.respond(filteredTasks)
            }

            get("/status/{status}") {
                val statusStr = call.parameters["status"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val status = TaskStatus.valueOf(statusStr.uppercase())
                    val userData = call.getUserData()
                    val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                    
                    val tasks = taskRepository.tasksByStatus(status)
                    val filteredTasks = tasks.filter { it.organizationId == organizationId }
                    call.respond(filteredTasks)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN") {
                    try {
                        val request = call.receive<CreateTaskRequest>()
                        // Verificar que el orgadmin solo puede crear tareas en su organización
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear tareas en otras organizaciones"))
                            return@post
                        }
                        val task = Task(
                            organizationId = request.organizationId,
                            type = request.type,
                            referenceNumber = request.referenceNumber,
                            clientId = request.clientId,
                            providerId = request.providerId,
                            addressOverride = request.addressOverride,
                            city = request.city,
                            province = request.province,
                            contact = request.contact,
                            courierId = request.courierId,
                            status = request.status,
                            priority = request.priority,
                            scheduledDate = request.scheduledDate,
                            notes = request.notes,
                            photoRequired = request.photoRequired,
                            mbl = request.mbl,
                            hbl = request.hbl,
                            freightCert = request.freightCert,
                            foCert = request.foCert,
                            bunkerCert = request.bunkerCert,
                            linkedTaskId = request.linkedTaskId,
                            receiptPhotoUrl = request.receiptPhotoUrl
                        )
                        val createdTask = taskRepository.create(task)
                        call.respond(HttpStatusCode.Created, createdTask)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear tareas"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                logger.info("PUT /tasks/$id - User: ${userData?.userId}, Role: ${userData?.role}")
                
                val existingTask = taskRepository.findById(id)
                
                if (existingTask == null) {
                    logger.warn("Task not found: $id")
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId) ||
                    (userData?.role == "COURIER" && existingTask.organizationId == userData.organizationId)) {
                    try {
                        val request = call.receive<UpdateTaskRequest>()
                        
                        // Log específico para cambios de cliente/proveedor
                        if (existingTask.clientId != request.clientId || existingTask.providerId != request.providerId) {
                            logger.info("Contact change detected - Task: $id, Old: client=${existingTask.clientId}, provider=${existingTask.providerId}, New: client=${request.clientId}, provider=${request.providerId}")
                        }
                        
                        val task = taskRepository.update(id, request)
                        if (task != null) {
                            logger.info("Task updated successfully: $id")
                            call.respond(task)
                        } else {
                            logger.error("Failed to update task: $id")
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        logger.error("Error updating task $id", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    logger.warn("Permission denied for task $id - User: ${userData?.userId}, Role: ${userData?.role}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar esta tarea"))
                }
            }

            patch("/{id}/status") {
                val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingTask = taskRepository.findById(id)
                
                if (existingTask == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@patch
                }
                
                // Verificar permisos - couriers pueden actualizar estado de tareas de su organización
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId) ||
                    (userData?.role == "COURIER" && existingTask.organizationId == userData.organizationId)) {
                    try {
                        val request = call.receive<Map<String, String>>()
                        val newStatus = request["status"] ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Status is required"))
                        
                        val updateRequest = UpdateTaskRequest(status = TaskStatus.valueOf(newStatus.uppercase()))
                        val task = taskRepository.update(id, updateRequest)
                        if (task != null) {
                            call.respond(task)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar el estado de esta tarea"))
                }
            }

            post("/{id}/photo") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingTask = taskRepository.findById(id)
                
                if (existingTask == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }
                
                // Verificar permisos - couriers pueden subir fotos a sus tareas
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId) ||
                    (userData?.role == "COURIER" && existingTask.organizationId == userData.organizationId)) {
                    try {
                        val multipartData = call.receiveMultipart()
                        var photoUrl: String? = null
                        
                        multipartData.forEachPart { part ->
                            when (part) {
                                is PartData.FileItem -> {
                                    if (part.name == "photo") {
                                        // Por ahora, guardamos la foto como base64 en la base de datos
                                        // En el futuro se puede cambiar a S3
                                        val bytes = part.streamProvider().readBytes()
                                        val base64 = java.util.Base64.getEncoder().encodeToString(bytes)
                                        photoUrl = "data:image/jpeg;base64,$base64"
                                    }
                                }
                                is PartData.BinaryChannelItem -> {
                                    // Handle binary channel items if needed
                                }
                                is PartData.BinaryItem -> {
                                    // Handle binary items if needed
                                }
                                is PartData.FormItem -> {
                                    // Handle form items if needed
                                }
                            }
                            part.dispose()
                        }
                        
                        if (photoUrl != null) {
                            // Actualizar la tarea con la URL de la foto
                            val updateRequest = UpdateTaskRequest(receiptPhotoUrl = photoUrl)
                            val updatedTask = taskRepository.update(id, updateRequest)
                            
                            if (updatedTask != null) {
                                call.respond(mapOf("photoUrl" to photoUrl))
                            } else {
                                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al guardar la foto"))
                            }
                        } else {
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No se encontró archivo de foto"))
                        }
                    } catch (e: Exception) {
                        logger.error("Error uploading photo for task $id", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para subir fotos a esta tarea"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingTask = taskRepository.findById(id)
                
                if (existingTask == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar tareas
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId)) {
                    try {
                        val deleted = taskRepository.delete(id)
                        if (deleted) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar esta tarea"))
                }
            }
        }
    }
}
