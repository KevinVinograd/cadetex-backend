package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val userService = UserService()

    route("/users") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                when (val result = userService.findByOrganization(organizationId)) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = userService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val user = result.value
                        val userData = call.getUserData()
                        if (userData?.role == "SUPERADMIN" || user.organizationId == userData?.organizationId) {
                            call.respond(user)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este usuario"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/email/{email}") {
                val email = call.parameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = userService.findByEmail(email)) {
                    is com.cadetex.service.Result.Success -> {
                        val user = result.value
                        if (user != null) {
                            val userData = call.getUserData()
                            if (userData?.role == "SUPERADMIN" || user.organizationId == userData?.organizationId) {
                                call.respond(user)
                            } else {
                                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este usuario"))
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = userService.findByOrganization(organizationId)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver usuarios de esta organización"))
                }
            }

            get("/role/{role}") {
                val roleStr = call.parameters["role"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val role = UserRole.valueOf(roleStr.uppercase())
                    val userData = call.getUserData()
                    val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                    
                    when (val result = userService.findByRole(role)) {
                        is com.cadetex.service.Result.Success -> {
                            // Filtrar por organización y rol
                            val filteredUsers = result.value.filter { it.organizationId == organizationId && it.role == role }
                            call.respond(filteredUsers)
                        }
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN") {
                    try {
                        val request = call.receive<CreateUserRequest>()
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear usuarios en otras organizaciones"))
                            return@post
                        }
                        when (val result = userService.create(request)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                            is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear usuarios"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = userService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingUser = findResult.value
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingUser.organizationId == userData.organizationId) ||
                            (userData?.userId == id)) {
                            try {
                                val request = call.receive<UpdateUserRequest>()
                                when (val updateResult = userService.update(id, request)) {
                                    is com.cadetex.service.Result.Success -> call.respond(updateResult.value)
                                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                }
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este usuario"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = userService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingUser = findResult.value
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingUser.organizationId == userData.organizationId)) {
                            when (val deleteResult = userService.delete(id)) {
                                is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.NoContent)
                                is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to deleteResult.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este usuario"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }
        }
    }
}
