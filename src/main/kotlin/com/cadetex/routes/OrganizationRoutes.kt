package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.OrganizationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.organizationRoutes() {
    val organizationRepository = OrganizationRepository()

    route("/organizations") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val organizations = organizationRepository.allOrganizations()
                    call.respond(organizations)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden ver todas las organizaciones"))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val organization = organizationRepository.findById(id)
                if (organization != null) {
                    call.respond(organization)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    try {
                        val request = call.receive<CreateOrganizationRequest>()
                        val organization = organizationRepository.create(request)
                        call.respond(HttpStatusCode.Created, organization)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden crear organizaciones"))
                }
            }

            put("/{id}") {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                    try {
                        val request = call.receive<UpdateOrganizationRequest>()
                        val organization = organizationRepository.update(id, request)
                        if (organization != null) {
                            call.respond(organization)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden actualizar organizaciones"))
                }
            }

            delete("/{id}") {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val deleted = organizationRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo superadministradores pueden eliminar organizaciones"))
                }
            }
        }
    }
}
