package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.service.CourierService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courierRoutes() {
    val courierService = CourierService()

    route("/couriers") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                val organizationId = userData?.organizationId ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo obtener la organización del usuario"))
                
                when (val result = courierService.findByOrganization(organizationId)) {
                    is com.cadetex.service.Result.Success -> call.respond(result.value)
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                when (val result = courierService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val courier = result.value
                        val userData = call.getUserData()
                        if (userData?.role == "SUPERADMIN" || courier.organizationId == userData?.organizationId) {
                            call.respond(courier)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este courier"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result.message))
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = courierService.findByOrganization(organizationId)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver couriers de esta organización"))
                }
            }

            get("/organization/{organizationId}/active") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = courierService.findActiveByOrganization(organizationId)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver couriers de esta organización"))
                }
            }

            get("/organization/{organizationId}/search/name/{name}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = courierService.searchByName(organizationId, name)) {
                        is com.cadetex.service.Result.Success -> call.respond(result.value)
                        is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            get("/organization/{organizationId}/search/phone/{phoneNumber}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val phoneNumber = call.parameters["phoneNumber"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    when (val result = courierService.searchByPhone(organizationId, phoneNumber)) {
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
                        val request = call.receive<CreateCourierRequest>()
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear couriers en otras organizaciones"))
                            return@post
                        }
                        when (val result = courierService.create(request)) {
                            is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.Created, result.value)
                            is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear couriers"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = courierService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingCourier = findResult.value
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingCourier.organizationId == userData.organizationId)) {
                            try {
                                val request = call.receive<UpdateCourierRequest>()
                                when (val updateResult = courierService.update(id, request)) {
                                    is com.cadetex.service.Result.Success -> call.respond(updateResult.value)
                                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to updateResult.message))
                                }
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este courier"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                
                when (val findResult = courierService.findById(id)) {
                    is com.cadetex.service.Result.Success -> {
                        val existingCourier = findResult.value
                        if (userData?.role == "SUPERADMIN" || 
                            (userData?.role == "ORGADMIN" && existingCourier.organizationId == userData.organizationId)) {
                            when (val deleteResult = courierService.delete(id)) {
                                is com.cadetex.service.Result.Success -> call.respond(HttpStatusCode.NoContent)
                                is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to deleteResult.message))
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este courier"))
                        }
                    }
                    is com.cadetex.service.Result.Error -> call.respond(HttpStatusCode.NotFound, mapOf("error" to findResult.message))
                }
            }
        }
    }
}
