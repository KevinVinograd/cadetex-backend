package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.ProviderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ProviderRoutes")

fun Route.providerRoutes() {
    val providerService = ProviderService()

    route("/providers") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                when (val result = providerService.findByOrganization(organizationId)) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = providerService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val provider = result.value
                        val userData = call.getUserData()
                        // Verificar que el proveedor pertenece a la misma organización
                        if (userData?.role == "SUPERADMIN" || provider.organizationId == userData?.organizationId) {
                            call.respond(provider)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este proveedor"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = providerService.findByOrganization(organizationId)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver proveedores de esta organización"))
                }
            }

            get("/organization/{organizationId}/search/name/{name}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = providerService.searchByName(organizationId, name)) {
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
                    when (val result = providerService.searchByCity(organizationId, city)) {
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
                        val request = call.receive<CreateProviderRequest>()
                        // Verificar que el orgadmin solo puede crear proveedores en su organización
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear proveedores en otras organizaciones"))
                            return@post
                        }
                        when (val result = providerService.create(request)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                            is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating provider: ${e.message}", e)
                        val errorMessage = e.message ?: "Error desconocido"
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to errorMessage))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear proveedores"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = providerService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingProvider = findResult.value
                        
                        // Verificar permisos
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingProvider.organizationId == userData.organizationId)) {
                            try {
                                val request = call.receive<UpdateProviderRequest>()
                                when (val updateResult = providerService.update(id, request)) {
                                    is com.cadetex.service.Result.Success -> call.respond(updateResult.value)
                                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                }
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este proveedor"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = providerService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingProvider = findResult.value
                        
                        // Solo superadmin y orgadmin pueden eliminar proveedores
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingProvider.organizationId == userData.organizationId)) {
                            when (val deleteResult = providerService.delete(id)) {
                                is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.NoContent)
                                is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to deleteResult.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este proveedor"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }
        }
    }
}
