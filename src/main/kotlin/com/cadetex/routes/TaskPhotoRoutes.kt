package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.TaskPhotoService
import com.cadetex.service.TaskService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskPhotoRoutes() {
    val taskPhotoService = TaskPhotoService()
    val taskService = TaskService()

    route("/task-photos") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                when (val result = taskPhotoService.findAll()) {
                    is com.cadetex.service.Result.Success -> {
                        val allPhotos = result.value
                        if (userData?.role == "SUPERADMIN") {
                            call.respond(allPhotos)
                        } else {
                            // Filtrar fotos por organización del usuario
                            val filteredPhotos = allPhotos.filter { photo ->
                                when (val taskResult = taskService.findById(photo.taskId)) {
                                    is com.cadetex.service.Result.Success -> {
                                        taskResult.value.organizationId == userData?.organizationId
                                    }
                                    is com.cadetex.service.Result.Error -> false
                                }
                            }
                            call.respond(filteredPhotos)
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
                    }
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                when (val photoResult = taskPhotoService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val taskPhoto = photoResult.value
                        val userData = call.getUserData()
                        when (val taskResult = taskService.findById(taskPhoto.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Verificar que la tarea pertenece a la misma organización
                                if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                                    call.respond(taskPhoto)
                                } else {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver esta foto"))
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to photoResult.message))
                    }
                }
            }

            get("/task/{taskId}") {
                val taskId = call.parameters["taskId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val taskResult = taskService.findById(taskId)) {
                    is com.cadetex.service.Result.Success -> {
                        val task = taskResult.value
                        // Verificar que la tarea pertenece a la misma organización
                        if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                            when (val photosResult = taskPhotoService.findByTaskId(taskId)) {
                                is com.cadetex.service.Result.Success -> {
                                    call.respond(photosResult.value)
                                }
                                is com.cadetex.service.Result.Error -> {
                                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to photosResult.message))
                                }
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver fotos de esta tarea"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                    }
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN" || userData?.role == "COURIER") {
                    try {
                        val request = call.receive<CreateTaskPhotoRequest>()
                        when (val taskResult = taskService.findById(request.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Verificar que la tarea pertenece a la misma organización
                                if (userData.role != "SUPERADMIN" && task.organizationId != userData?.organizationId) {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes agregar fotos a tareas de otras organizaciones"))
                                    return@post
                                }
                                
                                when (val photoResult = taskPhotoService.create(request)) {
                                    is com.cadetex.service.Result.Success -> {
                                        call.respond(HttpStatusCode.Created, photoResult.value)
                                    }
                                    is com.cadetex.service.Result.Error -> {
                                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to photoResult.message))
                                    }
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para crear fotos de tareas"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val photoResult = taskPhotoService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingPhoto = photoResult.value
                        when (val taskResult = taskService.findById(existingPhoto.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Solo superadmin y orgadmin pueden actualizar fotos
                                if (userData?.role == "SUPERADMIN" || 
                                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                                    try {
                                        val request = call.receive<UpdateTaskPhotoRequest>()
                                        when (val updateResult = taskPhotoService.update(id, request)) {
                                            is com.cadetex.service.Result.Success -> {
                                                call.respond(updateResult.value)
                                            }
                                            is com.cadetex.service.Result.Error -> {
                                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                            }
                                        }
                                    } catch (e: Exception) {
                                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                                    }
                                } else {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar esta foto"))
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to photoResult.message))
                    }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val photoResult = taskPhotoService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingPhoto = photoResult.value
                        when (val taskResult = taskService.findById(existingPhoto.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Solo superadmin y orgadmin pueden eliminar fotos
                                if (userData?.role == "SUPERADMIN" || 
                                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                                    when (val deleteResult = taskPhotoService.delete(id)) {
                                        is com.cadetex.service.Result.Success -> {
                                            call.respond(HttpStatusCode.NoContent)
                                        }
                                        is com.cadetex.service.Result.Error -> {
                                            call.respond(HttpStatusCode.NotFound, mapOf("error" to deleteResult.message))
                                        }
                                    }
                                } else {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar esta foto"))
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to photoResult.message))
                    }
                }
            }
        }
    }
}
