package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val userRepository = UserRepository()

    route("/users") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                val users = userRepository.findByOrganization(organizationId)
                call.respond(users)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = userRepository.findById(id)
                if (user != null) {
                    val userData = call.getUserData()
                    // Verificar que el usuario pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || user.organizationId == userData?.organizationId) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este usuario"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/email/{email}") {
                val email = call.parameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = userRepository.findByEmail(email)
                if (user != null) {
                    val userData = call.getUserData()
                    // Verificar que el usuario pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || user.organizationId == userData?.organizationId) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este usuario"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val users = userRepository.findByOrganization(organizationId)
                    call.respond(users)
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
                    
                    // Filtrar por organización y rol
                    val allUsers = userRepository.findByOrganization(organizationId)
                    val filteredUsers = allUsers.filter { it.role == role }
                    call.respond(filteredUsers)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN") {
                    try {
                        val request = call.receive<CreateUserRequest>()
                        // Verificar que el orgadmin solo puede crear usuarios en su organización
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear usuarios en otras organizaciones"))
                            return@post
                        }
                        val user = userRepository.create(request)
                        call.respond(HttpStatusCode.Created, user)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear usuarios"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingUser = userRepository.findById(id)
                
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingUser.organizationId == userData.organizationId) ||
                    (userData?.userId == id)) { // El usuario puede actualizarse a sí mismo
                    try {
                        val request = call.receive<UpdateUserRequest>()
                        val user = userRepository.update(id, request)
                        if (user != null) {
                            call.respond(user)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este usuario"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingUser = userRepository.findById(id)
                
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar usuarios
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingUser.organizationId == userData.organizationId)) {
                    val deleted = userRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este usuario"))
                }
            }
        }
    }
}
