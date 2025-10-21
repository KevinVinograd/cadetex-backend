package com.cadetex.routes

import com.cadetex.auth.getUserData
import com.cadetex.model.*
import com.cadetex.repository.CourierRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courierRoutes() {
    val courierRepository = CourierRepository()

    route("/couriers") {
        authenticate("jwt") {
            get {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN") {
                    val couriers = courierRepository.allCouriers()
                    call.respond(couriers)
                } else {
                    // Los orgadmin solo ven couriers de su organización
                    val couriers = courierRepository.findByOrganization(userData?.organizationId ?: "")
                    call.respond(couriers)
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val courier = courierRepository.findById(id)
                if (courier != null) {
                    val userData = call.getUserData()
                    // Verificar que el courier pertenece a la misma organización
                    if (userData?.role == "SUPERADMIN" || courier.organizationId == userData?.organizationId) {
                        call.respond(courier)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver este courier"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/organization/{organizationId}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val couriers = courierRepository.findByOrganization(organizationId)
                    call.respond(couriers)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver couriers de esta organización"))
                }
            }

            get("/organization/{organizationId}/active") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val couriers = courierRepository.findActiveByOrganization(organizationId)
                    call.respond(couriers)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para ver couriers de esta organización"))
                }
            }

            get("/organization/{organizationId}/search/name/{name}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val couriers = courierRepository.searchByName(organizationId, name)
                    call.respond(couriers)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            get("/organization/{organizationId}/search/phone/{phoneNumber}") {
                val organizationId = call.parameters["organizationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val phoneNumber = call.parameters["phoneNumber"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.organizationId == organizationId) {
                    val couriers = courierRepository.searchByPhone(organizationId, phoneNumber)
                    call.respond(couriers)
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para buscar en esta organización"))
                }
            }

            post {
                val userData = call.getUserData()
                if (userData?.role == "SUPERADMIN" || userData?.role == "ORGADMIN") {
                    try {
                        val request = call.receive<CreateCourierRequest>()
                        // Verificar que el orgadmin solo puede crear couriers en su organización
                        if (userData.role == "ORGADMIN" && request.organizationId != userData.organizationId) {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No puedes crear couriers en otras organizaciones"))
                            return@post
                        }
                        val courier = courierRepository.create(request)
                        call.respond(HttpStatusCode.Created, courier)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo administradores pueden crear couriers"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingCourier = courierRepository.findById(id)
                
                if (existingCourier == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                
                // Verificar permisos
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingCourier.organizationId == userData.organizationId)) {
                    try {
                        val request = call.receive<UpdateCourierRequest>()
                        val courier = courierRepository.update(id, request)
                        if (courier != null) {
                            call.respond(courier)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para actualizar este courier"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userData = call.getUserData()
                val existingCourier = courierRepository.findById(id)
                
                if (existingCourier == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                
                // Solo superadmin y orgadmin pueden eliminar couriers
                if (userData?.role == "SUPERADMIN" || 
                    (userData?.role == "ORGADMIN" && existingCourier.organizationId == userData.organizationId)) {
                    val deleted = courierRepository.delete(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No tienes permisos para eliminar este courier"))
                }
            }
        }
    }
}
