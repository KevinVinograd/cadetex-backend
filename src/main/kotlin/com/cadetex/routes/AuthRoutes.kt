package com.cadetex.routes

import com.cadetex.auth.AuthService
import com.cadetex.auth.AuthResult
import com.cadetex.model.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    val authService = AuthService()

    route("/auth") {
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val result = authService.login(request.email, request.password)
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.OK, LoginResponse(
                            success = true,
                            user = result.user,
                            token = result.token
                        ))
                    }
                    is AuthResult.Error -> {
                        call.respond(HttpStatusCode.Unauthorized, LoginResponse(
                            success = false,
                            error = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, LoginResponse(
                    success = false,
                    error = "Error en el formato de la solicitud: ${e.message}"
                ))
            }
        }

        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                val result = authService.register(
                    organizationId = request.organizationId,
                    name = request.name,
                    email = request.email,
                    password = request.password,
                    role = request.role
                )
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.Created, LoginResponse(
                            success = true,
                            user = result.user,
                            token = result.token
                        ))
                    }
                    is AuthResult.Error -> {
                        call.respond(HttpStatusCode.BadRequest, LoginResponse(
                            success = false,
                            error = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, LoginResponse(
                    success = false,
                    error = "Error en el formato de la solicitud: ${e.message}"
                ))
            }
        }

        post("/validate") {
            try {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (token == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token no proporcionado"))
                    return@post
                }
                
                val userData = authService.validateToken(token)
                if (userData != null) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "valid" to true,
                        "user" to userData
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Error al validar token"))
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class RegisterRequest(
    val organizationId: String,
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole
)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val success: Boolean,
    val user: com.cadetex.model.User? = null,
    val token: String? = null,
    val error: String? = null
)
