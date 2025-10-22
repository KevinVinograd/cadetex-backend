package com.cadetex.routes

import com.cadetex.auth.AuthService
import com.cadetex.auth.AuthResult
import com.cadetex.model.UserRole
import com.cadetex.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthRoutes")

fun Route.authRoutes() {
    val authService = AuthService()
    val userRepository = UserRepository()

    route("/auth") {
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val result = authService.login(request.email, request.password)
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.OK, LoginResponse(
                            token = result.token,
                            user = result.user
                        ))
                    }
                    is AuthResult.Error -> {
                        logger.warn("Login failed for email: ${request.email}, reason: ${result.message}")
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse(
                            error = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in login endpoint: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(
                    error = "Error en el formato de la solicitud: ${e.message}"
                ))
            }
        }

        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                
                val userRole = try {
                    UserRole.valueOf(request.role.uppercase())
                } catch (e: IllegalArgumentException) {
                    logger.warn("Invalid role provided: ${request.role}")
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Rol inválido: ${request.role}"))
                    return@post
                }
                
                val result = authService.register(
                    organizationId = request.organizationId,
                    name = request.name,
                    email = request.email,
                    password = request.password,
                    role = userRole
                )
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.Created, LoginResponse(
                            token = result.token,
                            user = result.user
                        ))
                    }
                    is AuthResult.Error -> {
                        logger.warn("Registration failed for email: ${request.email}, reason: ${result.message}")
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(
                            error = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in register endpoint: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(
                    error = "Error en el formato de la solicitud: ${e.message}"
                ))
            }
        }

        post("/validate") {
            try {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (token == null) {
                    logger.warn("Token validation failed: No token provided")
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error = "Token no proporcionado"))
                    return@post
                }
                
                val tokenData = authService.validateToken(token)
                if (tokenData != null) {
                    // Buscar el usuario completo en la base de datos
                    val user = userRepository.findById(tokenData.userId)
                    if (user != null) {
                        call.respond(HttpStatusCode.OK, ValidateResponse(
                            valid = true,
                            user = user
                        ))
                    } else {
                        logger.warn("Token validation failed: User not found for ID: ${tokenData.userId}")
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error = "Usuario no encontrado"))
                    }
                } else {
                    logger.warn("Token validation failed: Invalid token")
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error = "Token inválido"))
                }
            } catch (e: Exception) {
                logger.error("Error in validate endpoint: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Error al validar token: ${e.message}"))
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
    val role: String
)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val token: String,
    val user: com.cadetex.model.User
)

@kotlinx.serialization.Serializable
data class ErrorResponse(
    val error: String
)

@kotlinx.serialization.Serializable
data class ValidateResponse(
    val valid: Boolean,
    val user: com.cadetex.model.User? = null
)

