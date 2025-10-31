package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.TaskService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Route.taskRoutes() {
    val taskService = TaskService()
    val logger = LoggerFactory.getLogger("TaskRoutes")

    route("/tasks") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                when (val result = taskService.findByOrganization(organizationId)) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = taskService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val task = result.value
                        val userData = call.getUserData()
                        if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                            call.respond(task)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/{id}/photos") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = taskService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val task = result.value
                        val userData = call.getUserData()
                        if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                            val taskPhotoService = com.cadetex.service.TaskPhotoService()
                            when (val photosResult = taskPhotoService.findByTaskId(id)) {
                                is com.cadetex.service.Result.Success -> {
                                    val allPhotos = mutableListOf<com.cadetex.model.TaskPhoto>()
                                    
                                    // Agregar foto de recibo si existe
                                    if (!task.receiptPhotoUrl.isNullOrBlank()) {
                                        allPhotos.add(
                                            com.cadetex.model.TaskPhoto(
                                                id = "receipt",
                                                taskId = id,
                                                photoUrl = task.receiptPhotoUrl!!,
                                                photoType = "RECEIPT",
                                                createdAt = null
                                            )
                                        )
                                    }
                                    
                                    // Agregar fotos adicionales
                                    allPhotos.addAll(photosResult.value)
                                    
                                    call.respond(allPhotos)
                                }
                                is com.cadetex.service.Result.Error -> {
                                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to photosResult.message))
                                }
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver fotos de esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = taskService.findByOrganization(organizationId)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver tareas de esta organización"))
                }
            }

            get("/courier/{courierId}") {
                val courierId = call.parameters["courierId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                when (val result = taskService.findByCourier(courierId)) {
                    is com.cadetex.service.Result.Success -> {
                        val filteredTasks = result.value.filter { it.organizationId == organizationId }
                        call.respond(filteredTasks)
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/filtered") {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                // Parámetros opcionales de filtro
                val queryParams = call.request.queryParameters
                val courierId = queryParams["courierId"]
                val unassigned = queryParams["unassigned"]?.toBoolean() == true
                val statusStrings = queryParams.getAll("status") ?: emptyList()
                
                // Convertir strings a enums TaskStatus
                val statuses = statusStrings.mapNotNull { 
                    try { TaskStatus.valueOf(it.uppercase()) } 
                    catch (e: IllegalArgumentException) { null }
                }
                
                when (val result = taskService.findFiltered(
                    organizationId = organizationId,
                    courierId = courierId,
                    unassigned = unassigned,
                    statuses = statuses
                )) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/status/{status}") {
                val statusStr = call.parameters["status"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val status = TaskStatus.valueOf(statusStr.uppercase())
                    val userData = call.getUserData()
                    val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                    
                    when (val result = taskService.findByStatus(status)) {
                        is com.cadetex.service.Result.Success -> {
                            val filteredTasks = result.value.filter { it.organizationId == organizationId }
                            call.respond(filteredTasks)
                        }
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN") {
                    try {
                        val request = call.receive<CreateTaskRequest>()
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear tareas en otras organizaciones"))
                            return@post
                        }
                        when (val result = taskService.create(request)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                            is com.cadetex.service.Result.Error -> {
                                val statusCode = if (result.message.contains("número de referencia")) {
                                    HttpStatusCode.Conflict
                                } else {
                                    HttpStatusCode.BadRequest
                                }
                                call.respond(statusCode, mapOf("error" to result.message))
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating task: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear tareas"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                logger.info("PUT /tasks/$id - User: ${userData?.userId}, Role: ${userData?.role}")
                
                when (val findResult = taskService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingTask = findResult.value
                        
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
                                
                                when (val updateResult = taskService.update(id, request)) {
                                    is com.cadetex.service.Result.Success -> {
                                        logger.info("Task updated successfully: $id")
                                        call.respond(updateResult.value)
                                    }
                                    is com.cadetex.service.Result.Error -> {
                                        logger.error("Failed to update task: $id - ${updateResult.message}")
                                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error("Error updating task $id", e)
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                            }
                        } else {
                            logger.warn("Permission denied for task $id - User: ${userData?.userId}, Role: ${userData?.role}")
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        logger.warn("Task not found: $id")
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                    }
                }
            }

            patch("/{id}/status") {
                val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = taskService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingTask = findResult.value
                        
                        // Verificar permisos - couriers pueden actualizar estado de tareas de su organización
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId) ||
                            (userData?.role == "COURIER" && existingTask.organizationId == userData.organizationId)) {
                            try {
                                val request = call.receive<Map<String, String>>()
                                val newStatus = request["status"] ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Status is required"))
                                
                                val updateRequest = UpdateTaskRequest(status = TaskStatus.valueOf(newStatus.uppercase()))
                                when (val updateResult = taskService.update(id, updateRequest)) {
                                    is com.cadetex.service.Result.Success -> call.respond(updateResult.value)
                                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                }
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar el estado de esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }

            post("/{id}/photo") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = taskService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingTask = findResult.value
                        
                        // Verificar permisos - couriers pueden subir fotos a sus tareas
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId) ||
                            (userData?.role == "COURIER" && existingTask.organizationId == userData.organizationId)) {
                            try {
                                val multipartData = call.receiveMultipart()
                                var photoBytes: ByteArray? = null
                                var contentType: String = "image/jpeg" // Por defecto JPEG
                                var isReceipt = false
                                
                                multipartData.forEachPart { part ->
                                    when (part) {
                                        is PartData.FileItem -> {
                                            if (part.name == "photo") {
                                                photoBytes = part.streamProvider().readBytes()
                                                // Usar el contentType del multipart si está disponible
                                                contentType = part.contentType?.toString() ?: "image/jpeg"
                                            }
                                        }
                                        is PartData.FormItem -> {
                                            if (part.name == "isReceipt") {
                                                isReceipt = part.value.toBoolean()
                                            }
                                        }
                                        is PartData.BinaryChannelItem -> {}
                                        is PartData.BinaryItem -> {}
                                    }
                                    part.dispose()
                                }
                                
                                if (photoBytes != null) {
                                    // Determinar extensión del tipo MIME
                                    val extension = when {
                                        contentType.contains("png", ignoreCase = true) -> "png"
                                        contentType.contains("jpeg", ignoreCase = true) -> "jpg"
                                        contentType.contains("jpg", ignoreCase = true) -> "jpg"
                                        contentType.contains("webp", ignoreCase = true) -> "webp"
                                        else -> "jpg"
                                    }
                                    
                                    // Subir a S3
                                    val s3Service = com.cadetex.service.S3Service()
                                    val photoType = if (isReceipt) "RECEIPT" else "ADDITIONAL"
                                    val s3Key = s3Service.generateTaskPhotoKey(id, photoType, extension)
                                    val inputStream = java.io.ByteArrayInputStream(photoBytes)
                                    
                                    when (val uploadResult = s3Service.uploadFile(s3Key, inputStream, contentType)) {
                                        is com.cadetex.service.Result.Success<String> -> {
                                            val photoUrl = uploadResult.value
                                            
                                            // Si es la foto obligatoria (receipt), actualizar en la tarea
                                            if (isReceipt) {
                                                val updateRequest = UpdateTaskRequest(receiptPhotoUrl = photoUrl)
                                                when (val updateResult = taskService.update(id, updateRequest)) {
                                                    is com.cadetex.service.Result.Success<com.cadetex.model.TaskResponse> -> {
                                                        call.respond(mapOf("photoUrl" to photoUrl))
                                                    }
                                                    is com.cadetex.service.Result.Error -> {
                                                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to updateResult.message))
                                                    }
                                                }
                                            } else {
                                                // Foto adicional: crear en task_photos
                                                val taskPhotoService = com.cadetex.service.TaskPhotoService()
                                                val createRequest = com.cadetex.model.CreateTaskPhotoRequest(
                                                    taskId = id,
                                                    photoUrl = photoUrl,
                                                    photoType = "ADDITIONAL"
                                                )
                                                when (val photoResult = taskPhotoService.create(createRequest)) {
                                                    is com.cadetex.service.Result.Success<com.cadetex.model.TaskPhoto> -> {
                                                        call.respond(mapOf("photoUrl" to photoUrl, "photoId" to photoResult.value.id))
                                                    }
                                                    is com.cadetex.service.Result.Error -> {
                                                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to photoResult.message))
                                                    }
                                                }
                                            }
                                        }
                                        is com.cadetex.service.Result.Error -> {
                                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to uploadResult.message))
                                        }
                                    }
                                } else {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No se encontró archivo de foto"))
                                }
                            } catch (e: Exception) {
                                logger.error("Error uploading photo for task $id", e)
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para subir fotos a esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = taskService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingTask = findResult.value
                        
                        // Solo superadmin y orgadmin pueden eliminar tareas
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId)) {
                            when (val deleteResult = taskService.delete(id)) {
                                is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.NoContent)
                                is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to deleteResult.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }
        }
    }
}
