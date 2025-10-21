package com.cadetex.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {
    val jwtService = JwtService()
    
    install(Authentication) {
        jwt("jwt") {
            realm = "Cadetex API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtService.secret))
                    .withAudience(jwtService.audience)
                    .withIssuer(jwtService.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }
}

// Extensión para obtener datos del usuario autenticado
fun ApplicationCall.getUserData(): JwtTokenData? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.let {
        JwtTokenData(
            userId = it.payload.getClaim("userId").asString(),
            email = it.payload.getClaim("email").asString(),
            role = it.payload.getClaim("role").asString(),
            organizationId = it.payload.getClaim("organizationId").asString()
        )
    }
}
