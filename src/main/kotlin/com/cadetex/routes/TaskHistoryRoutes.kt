package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.TaskHistoryRepository
import com.cadetex.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskHistoryRoutes() {
    val taskHistoryRepository = TaskHistoryRepository()
    val taskRepository = TaskRepository()

    route("/task-history") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val taskHistory = taskHistoryRepository.allTaskHistory()
                    call.respond(taskHistory)
                } else {
                    // Filtrar historial por organización del usuario
                    val allHistory = taskHistoryRepository.allTaskHistory()
                    val filteredHistory = allHistory.filter { history ->
                        val task = taskRepository.findById(history.taskId)
                        task?.organizationId == userData?.organizationId
                    }
                    call.respond(filteredHistory)
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val taskHistory = taskHistoryRepository.findById(id)
                if (taskHistory != null) {
                    val userData = call.getUserData()
                    val task = taskRepository.findById(taskHistory.taskId)
                    
                    // Verificar que la tarea pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || task?.organizationId == userData?.organizationId) {
                        call.respond(taskHistory)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este historial"))
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
                    val taskHistory = taskHistoryRepository.findByTaskId(taskId)
                    call.respond(taskHistory)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver el historial de esta tarea"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN" || userData?.role == "COURIER") {
                    try {
                        val request = call.receive<CreateTaskHistoryRequest>()
                        val task = taskRepository.findById(request.taskId)
                        
                        if (task == null) {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            return@post
                        }
                        
                        // Verificar que la tarea pertenece a la misma organización
                        if (userData.role != "SUPERADMIN" && task.organizationId != userData?.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes agregar historial a tareas de otras organizaciones"))
                            return@post
                        }
                        
                        val taskHistory = taskHistoryRepository.create(request)
                        call.respond(HttpStatusCode.Created, taskHistory)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para crear historial de tareas"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingHistory = taskHistoryRepository.findById(id)
                
                if (existingHistory == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                val task = taskRepository.findById(existingHistory.taskId)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                    try {
                        val request = call.receive<UpdateTaskHistoryRequest>()
                        val taskHistory = taskHistoryRepository.update(id, request)
                        if (taskHistory != null) {
                            call.respond(taskHistory)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este historial"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingHistory = taskHistoryRepository.findById(id)
                
                if (existingHistory == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                val task = taskRepository.findById(existingHistory.taskId)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar historial
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                    val deleted = taskHistoryRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este historial"))
                }
            }
        }
    }
}
