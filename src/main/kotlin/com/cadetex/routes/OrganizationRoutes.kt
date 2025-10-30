package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.OrganizationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("OrganizationRoutes")

fun Route.organizationRoutes() {
    val organizationService = OrganizationService()

    route("/organizations") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    when (val result = organizationService.findAll()) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden ver todas las organizaciones"))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = organizationService.findById(id)) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    try {
                        val request = call.receive<CreateOrganizationRequest>()
                        when (val result = organizationService.create(request)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                            is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating organization: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    logger.warn("Unauthorized user trying to create organization: role=${userData?.role}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden crear organizaciones"))
                }
            }

            put("/{id}") {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                    try {
                        val request = call.receive<UpdateOrganizationRequest>()
                        when (val result = organizationService.update(id, request)) {
                            is com.cadetex.service.Result.Success -> call.respond(result.value)
                            is com.cadetex.service.Result.Error -> {
                                // Si el error es que la organización no existe, devolver 404
                                if (result.message.contains("no encontrada", ignoreCase = true)) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                                } else {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error updating organization: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    logger.warn("Unauthorized user trying to update organization: role=${userData?.role}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden actualizar organizaciones"))
                }
            }

            delete("/{id}") {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    try {
                        when (val result = organizationService.delete(id)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.NoContent)
                            is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                        }
                    } catch (e: Exception) {
                        logger.error("Error deleting organization: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    logger.warn("Unauthorized user trying to delete organization: role=${userData?.role}")
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden eliminar organizaciones"))
                }
            }
        }
    }
}
