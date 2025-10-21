package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.ProviderRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.providerRoutes() {
    val providerRepository = ProviderRepository()

    route("/providers") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val providers = providerRepository.allProviders()
                    call.respond(providers)
                } else {
                    // Los orgadmin solo ven proveedores de su organización
                    val providers = providerRepository.findByOrganization(userData?.organizationId ?: "")
                    call.respond(providers)
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val provider = providerRepository.findById(id)
                if (provider != null) {
                    val userData = call.getUserData()
                    // Verificar que el proveedor pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || provider.organizationId == userData?.organizationId) {
                        call.respond(provider)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este proveedor"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val providers = providerRepository.findByOrganization(organizationId)
                    call.respond(providers)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver proveedores de esta organización"))
                }
            }

            get("/organization/{organizationId}/search/name/{name}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val providers = providerRepository.searchByName(organizationId, name)
                    call.respond(providers)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            get("/organization/{organizationId}/search/city/{city}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val city = call.parameters["city"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val providers = providerRepository.searchByCity(organizationId, city)
                    call.respond(providers)
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
                        val provider = providerRepository.create(request)
                        call.respond(HttpStatusCode.Created, provider)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear proveedores"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingProvider = providerRepository.findById(id)
                
                if (existingProvider == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingProvider.organizationId == userData.organizationId)) {
                    try {
                        val request = call.receive<UpdateProviderRequest>()
                        val provider = providerRepository.update(id, request)
                        if (provider != null) {
                            call.respond(provider)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este proveedor"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingProvider = providerRepository.findById(id)
                
                if (existingProvider == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar proveedores
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingProvider.organizationId == userData.organizationId)) {
                    val deleted = providerRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este proveedor"))
                }
            }
        }
    }
}
