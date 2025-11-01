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

        // Health check endpoint (público, para monitoreo)
        get("/health") {
            try {
                val runtime = Runtime.getRuntime()
                val totalMemory = runtime.totalMemory()
                val freeMemory = runtime.freeMemory()
                val usedMemory = totalMemory - freeMemory
                val maxMemory = runtime.maxMemory()
                
                call.respond(
                    mapOf(
                        "status" to "healthy",
                        "service" to "cadetex-backend",
                        "memory" to mapOf(
                            "usedMB" to (usedMemory / 1024 / 1024),
                            "totalMB" to (totalMemory / 1024 / 1024),
                            "maxMB" to (maxMemory / 1024 / 1024),
                            "freeMB" to (freeMemory / 1024 / 1024)
                        )
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    mapOf(
                        "status" to "healthy",
                        "service" to "cadetex-backend",
                        "note" to "memory info unavailable"
                    )
                )
            }
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
