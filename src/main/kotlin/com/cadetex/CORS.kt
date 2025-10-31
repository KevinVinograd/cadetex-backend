package com.cadetex

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        // Dominios permitidos (prod)
        allowHost("kdt-frontend-prod-sa-east-1.s3-website.sa-east-1.amazonaws.com", schemes = listOf("http"))
        allowHost("don0yfk21axa5.cloudfront.net", schemes = listOf("https", "http"))
        // Para desarrollo local
        allowHost("localhost:5173", schemes = listOf("http"))
    }
}

