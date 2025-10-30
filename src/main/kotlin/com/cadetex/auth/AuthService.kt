package com.cadetex.auth

import com.cadetex.model.User
import com.cadetex.model.UserRole
import com.cadetex.service.UserService
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthService")

class AuthService {

    private val userService = UserService()
    private val jwtService = JwtService()

    suspend fun login(email: String, password: String): AuthResult {
        val userResult = userService.findByEmail(email)
        
        when (userResult) {
            is com.cadetex.service.Result.Success -> {
                val user = userResult.value ?: return AuthResult.Error("Usuario no encontrado")
                
                if (!userService.verifyPassword(password, user.passwordHash)) {
                    return AuthResult.Error("Contraseña incorrecta")
                }

                val token = jwtService.generateToken(user)
                return AuthResult.Success(user, token)
            }
            is com.cadetex.service.Result.Error -> {
                logger.warn("Login failed: ${userResult.message}")
                return AuthResult.Error("Usuario no encontrado")
            }
        }
    }

    suspend fun register(
            organizationId: String,
            name: String,
            email: String,
            password: String,
            role: UserRole
    ): AuthResult {
        // Verificar si el usuario ya existe
        val existingUserResult = userService.findByEmail(email)
        
        when (existingUserResult) {
            is com.cadetex.service.Result.Success -> {
                if (existingUserResult.value != null) {
                    return AuthResult.Error("El usuario ya existe")
                }
            }
            is com.cadetex.service.Result.Error -> {
                // Si hay un error al buscar, continuar con el registro
                logger.debug("No se encontró usuario existente, continuando con registro")
            }
        }

        // Crear nuevo usuario usando UserService
        val createRequest = com.cadetex.model.CreateUserRequest(
                organizationId = organizationId,
                name = name,
                email = email,
                password = password,
                role = role
        )

        val createResult = userService.create(createRequest)
        
        when (createResult) {
            is com.cadetex.service.Result.Success -> {
                val user = createResult.value
                val token = jwtService.generateToken(user)
                return AuthResult.Success(user, token)
            }
            is com.cadetex.service.Result.Error -> {
                logger.error("Error creating user: ${createResult.message}")
                return AuthResult.Error("Error al crear usuario: ${createResult.message}")
            }
        }
    }

    suspend fun findUserById(userId: String): User? {
        return when (val result = userService.findById(userId)) {
            is com.cadetex.service.Result.Success -> result.value
            is com.cadetex.service.Result.Error -> null
        }
    }

    fun validateToken(token: String): JwtTokenData? {
        return jwtService.validateToken(token)
    }
}

sealed class AuthResult {
    data class Success(val user: User, val token: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
