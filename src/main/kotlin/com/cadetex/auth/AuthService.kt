package com.cadetex.auth

import com.cadetex.model.User
import com.cadetex.model.UserRole
import com.cadetex.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService {
    
    private val userRepository = UserRepository()
    private val jwtService = JwtService()
    
    suspend fun login(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(email)
        
        if (user == null) {
            return AuthResult.Error("Usuario no encontrado")
        }
        
        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return AuthResult.Error("Contraseña incorrecta")
        }
        
        val token = jwtService.generateToken(user)
        return AuthResult.Success(user, token)
    }
    
    suspend fun register(
        organizationId: String,
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): AuthResult {
        // Verificar si el usuario ya existe
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            return AuthResult.Error("El usuario ya existe")
        }
        
        // Crear nuevo usuario
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val createRequest = com.cadetex.model.CreateUserRequest(
            organizationId = organizationId,
            name = name,
            email = email,
            password = password, // El repository se encarga del hashing
            role = role
        )
        
        try {
            val user = userRepository.create(createRequest)
            val token = jwtService.generateToken(user)
            return AuthResult.Success(user, token)
        } catch (e: Exception) {
            return AuthResult.Error("Error al crear usuario: ${e.message}")
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
