package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.TaskHistoryService
import com.cadetex.service.TaskService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskHistoryRoutes() {
    val taskHistoryService = TaskHistoryService()
    val taskService = TaskService()

    route("/task-history") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                when (val result = taskHistoryService.findAll()) {
                    is com.cadetex.service.Result.Success -> {
                        val allHistory = result.value
                        if (userData?.role == "SUPERADMIN") {
                            call.respond(allHistory)
                        } else {
                            // Filtrar historial por organización del usuario
                            val filteredHistory = allHistory.filter { history ->
                                when (val taskResult = taskService.findById(history.taskId)) {
                                    is com.cadetex.service.Result.Success -> {
                                        taskResult.value.organizationId == userData?.organizationId
                                    }
                                    is com.cadetex.service.Result.Error -> false
                                }
                            }
                            call.respond(filteredHistory)
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
                    }
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                when (val historyResult = taskHistoryService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val taskHistory = historyResult.value
                        val userData = call.getUserData()
                        when (val taskResult = taskService.findById(taskHistory.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Verificar que la tarea pertenece a la misma organización
                                if (userData?.role == "SUPERADMIN" || task.organizationId == userData?.organizationId) {
                                    call.respond(taskHistory)
                                } else {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este historial"))
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to historyResult.message))
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
                            when (val historyResult = taskHistoryService.findByTaskId(taskId)) {
                                is com.cadetex.service.Result.Success -> {
                                    call.respond(historyResult.value)
                                }
                                is com.cadetex.service.Result.Error -> {
                                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to historyResult.message))
                                }
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver el historial de esta tarea"))
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
                        val request = call.receive<CreateTaskHistoryRequest>()
                        when (val taskResult = taskService.findById(request.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Verificar que la tarea pertenece a la misma organización
                                if (userData.role != "SUPERADMIN" && task.organizationId != userData?.organizationId) {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes agregar historial a tareas de otras organizaciones"))
                                    return@post
                                }
                                
                                when (val historyResult = taskHistoryService.create(request)) {
                                    is com.cadetex.service.Result.Success -> {
                                        call.respond(HttpStatusCode.Created, historyResult.value)
                                    }
                                    is com.cadetex.service.Result.Error -> {
                                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to historyResult.message))
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
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para crear historial de tareas"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val historyResult = taskHistoryService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingHistory = historyResult.value
                        when (val taskResult = taskService.findById(existingHistory.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Verificar permisos
                                if (userData?.role == "SUPERADMIN" || 
                                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                                    try {
                                        val request = call.receive<UpdateTaskHistoryRequest>()
                                        when (val updateResult = taskHistoryService.update(id, request)) {
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
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este historial"))
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to historyResult.message))
                    }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val historyResult = taskHistoryService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingHistory = historyResult.value
                        when (val taskResult = taskService.findById(existingHistory.taskId)) {
                            is com.cadetex.service.Result.Success -> {
                                val task = taskResult.value
                                // Solo superadmin y orgadmin pueden eliminar historial
                                if (userData?.role == "SUPERADMIN" || 
                                    (userData?.role == "ORGADMIN" && task.organizationId == userData.organizationId)) {
                                    when (val deleteResult = taskHistoryService.delete(id)) {
                                        is com.cadetex.service.Result.Success -> {
                                            call.respond(HttpStatusCode.NoContent)
                                        }
                                        is com.cadetex.service.Result.Error -> {
                                            call.respond(HttpStatusCode.NotFound, mapOf("error" to deleteResult.message))
                                        }
                                    }
                                } else {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este historial"))
                                }
                            }
                            is com.cadetex.service.Result.Error -> {
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarea no encontrada"))
                            }
                        }
                    }
                    is com.cadetex.service.Result.Error -> {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to historyResult.message))
                    }
                }
            }
        }
    }
}
