package com.cadetex

import com.cadetex.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Cadetex API - Sistema de Gestión de Cadetes")
        }

        // Rutas de autenticación (públicas)
        authRoutes()

        // Rutas de la API (protegidas)
        organizationRoutes()
        userRoutes()
        clientRoutes()
        providerRoutes()
        courierRoutes()
        taskRoutes()
        taskPhotoRoutes()
        taskHistoryRoutes()
    }
}
