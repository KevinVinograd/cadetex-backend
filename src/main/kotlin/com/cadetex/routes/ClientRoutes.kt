package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.ClientService
import com.cadetex.service.error
import com.cadetex.service.success
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ClientRoutes")

fun Route.clientRoutes() {
    val clientService = ClientService()

    route("/clients") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                when (val result = clientService.findByOrganization(organizationId)) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = clientService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val client = result.value
                        val userData = call.getUserData()
                        // Verificar que el cliente pertenece a la misma organización
                        if (userData?.role == "SUPERADMIN" || client.organizationId == userData?.organizationId) {
                            call.respond(client)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este cliente"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = clientService.findByOrganization(organizationId)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver clientes de esta organización"))
                }
            }

            get("/organization/{organizationId}/search/name/{name}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = clientService.searchByName(organizationId, name)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            get("/organization/{organizationId}/search/city/{city}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val city = call.parameters["city"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = clientService.searchByCity(organizationId, city)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            post {
                val userData = call.getUserData()
                
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN") {
                    try {
                        val request = call.receive<CreateClientRequest>()
                        
                        // Verificar que el orgadmin solo puede crear clientes en su organización
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            logger.warn("ORGADMIN trying to create client in different organization: ${request.organizationId} != ${userData.organizationId}")
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear clientes en otras organizaciones"))
                            return@post
                        }
                        
                        when (val result = clientService.create(request)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                            is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating client: ${e.message}", e)
                        val errorMessage = e.message ?: "Error desconocido"
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to errorMessage))
                    }
                } else {
                    logger.warn("Unauthorized user trying to create client: role=${userData?.role}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear clientes"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = clientService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingClient = findResult.value
                        
                        // Verificar permisos
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingClient.organizationId == userData.organizationId)) {
                            try {
                                val request = call.receive<UpdateClientRequest>()
                                when (val updateResult = clientService.update(id, request)) {
                                    is com.cadetex.service.Result.Success -> call.respond(updateResult.value)
                                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                }
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este cliente"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = clientService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingClient = findResult.value
                        
                        // Solo superadmin y orgadmin pueden eliminar clientes
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingClient.organizationId == userData.organizationId)) {
                            when (val deleteResult = clientService.delete(id)) {
                                is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.NoContent)
                                is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to deleteResult.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este cliente"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }
        }
    }
}
