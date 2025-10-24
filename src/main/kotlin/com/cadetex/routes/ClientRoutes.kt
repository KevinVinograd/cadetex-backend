package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.ClientRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ClientRoutes")

fun Route.clientRoutes() {
    val clientRepository = ClientRepository()

    route("/clients") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                val clients = clientRepository.findByOrganization(organizationId)
                call.respond(clients)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val client = clientRepository.findById(id)
                if (client != null) {
                    val userData = call.getUserData()
                    // Verificar que el cliente pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || client.organizationId == userData?.organizationId) {
                        call.respond(client)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este cliente"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val clients = clientRepository.findByOrganization(organizationId)
                    call.respond(clients)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver clientes de esta organización"))
                }
            }

            get("/organization/{organizationId}/search/name/{name}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val clients = clientRepository.searchByName(organizationId, name)
                    call.respond(clients)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            get("/organization/{organizationId}/search/city/{city}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val city = call.parameters["city"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val clients = clientRepository.searchByCity(organizationId, city)
                    call.respond(clients)
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
                        
                        val client = clientRepository.create(request)
                        call.respond(HttpStatusCode.Created, client)
                    } catch (e: Exception) {
                        logger.error("Error creating client: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    logger.warn("Unauthorized user trying to create client: role=${userData?.role}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear clientes"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingClient = clientRepository.findById(id)
                
                if (existingClient == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingClient.organizationId == userData.organizationId)) {
                    try {
                        val request = call.receive<UpdateClientRequest>()
                        val client = clientRepository.update(id, request)
                        if (client != null) {
                            call.respond(client)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este cliente"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingClient = clientRepository.findById(id)
                
                if (existingClient == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar clientes
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingClient.organizationId == userData.organizationId)) {
                    val deleted = clientRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este cliente"))
                }
            }
        }
    }
}
