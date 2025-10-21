package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.TaskPhotoRepository
import com.cadetex.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskPhotoRoutes() {
    val taskPhotoRepository = TaskPhotoRepository()
    val taskRepository = TaskRepository()

    route("/task-photos") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val taskPhotos = taskPhotoRepository.allTaskPhotos()
                    call.respond(taskPhotos)
                } else {
                    // Filtrar fotos por organización del usuario
                    val allPhotos = taskPhotoRepository.allTaskPhotos()
                    val filteredPhotos = allPhotos.filter { photo ->
                        val task = taskRepository.findById(photo.taskId)
                        task?.organizationId == userData?.organizationId
                    }
                    call.respond(filteredPhotos)
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val taskPhoto = taskPhotoRepository.findById(id)
                if (taskPhoto != null) {
                    val userData = call.getUserData()
                    val task = taskRepository.findById(taskPhoto.taskId)
                    
                    // Verificar que la tarea pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || task?.organizationId == userData?.organizationId) {
                        call.respond(taskPhoto)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver esta foto"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/task/{taskId}") {
                val taskId = call.parameters["taskId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val task = taskRepository.findById(taskId)
                
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                
                // Verificar que la tarea pertenece a la misma organización
                if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                    val taskPhotos = taskPhotoRepository.findByTaskId(taskId)
                    call.respond(taskPhotos)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver fotos de esta tarea"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN" || userData?.role == "COURIER") {
                    try {
                        val request = call.receive<CreateTaskPhotoRequest>()
                        val task = taskRepository.findById(request.taskId)
                        
                        if (task == null) {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            return@post
                        }
                        
                        // Verificar que la tarea pertenece a la misma organización
                        if (userData.role != "SUPERADMIN" && task.organizationId != userData?.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes agregar fotos a tareas de otras organizaciones"))
                            return@post
                        }
                        
                        val taskPhoto = taskPhotoRepository.create(request)
                        call.respond(HttpStatusCode.Created, taskPhoto)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para crear fotos de tareas"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingPhoto = taskPhotoRepository.findById(id)
                
                if (existingPhoto == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                val task = taskRepository.findById(existingPhoto.taskId)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId) ||
                    (userData?.role == "COURIER" && task.courierId == userData.userId)) {
                    try {
                        val request = call.receive<UpdateTaskPhotoRequest>()
                        val taskPhoto = taskPhotoRepository.update(id, request)
                        if (taskPhoto != null) {
                            call.respond(taskPhoto)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar esta foto"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingPhoto = taskPhotoRepository.findById(id)
                
                if (existingPhoto == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                val task = taskRepository.findById(existingPhoto.taskId)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar fotos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                    val deleted = taskPhotoRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar esta foto"))
                }
            }
        }
    }
}
