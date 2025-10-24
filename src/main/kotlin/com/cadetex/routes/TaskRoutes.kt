package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskRoutes() {
    val taskRepository = TaskRepository()

    route("/tasks") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val tasks = taskRepository.allTasks()
                    call.respond(tasks)
                } else {
                    // Los orgadmin solo ven tareas de su organización
                    val tasks = taskRepository.tasksByOrganization(userData?.organizationId ?: "")
                    call.respond(tasks)
                }
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
                val tasks = taskRepository.tasksByCourier(courierId)
                
                // Filtrar por organización si no es superadmin
                if (userData?.role != "SUPERADMIN") {
                    val filteredTasks = tasks.filter { it.organizationId == userData?.organizationId }
                    call.respond(filteredTasks)
                } else {
                    call.respond(tasks)
                }
            }

            get("/status/{status}") {
                val statusStr = call.parameters["status"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val status = TaskStatus.valueOf(statusStr.uppercase())
                    val userData = call.getUserData()
                    val tasks = taskRepository.tasksByStatus(status)
                    
                    // Filtrar por organización si no es superadmin
                    if (userData?.role != "SUPERADMIN") {
                        val filteredTasks = tasks.filter { it.organizationId == userData?.organizationId }
                        call.respond(filteredTasks)
                    } else {
                        call.respond(tasks)
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
                val existingTask = taskRepository.findById(id)
                
                if (existingTask == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingTask.organizationId == userData.organizationId) ||
                    (userData?.role == "COURIER" && existingTask.courierId == userData.userId)) { // Los couriers pueden actualizar sus tareas
                    try {
                        val request = call.receive<UpdateTaskRequest>()
                        val task = taskRepository.update(id, request)
                        if (task != null) {
                            call.respond(task)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar esta tarea"))
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
                    val deleted = taskRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar esta tarea"))
                }
            }
        }
    }
}
