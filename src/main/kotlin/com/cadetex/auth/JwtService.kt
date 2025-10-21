package com.cadetex.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.cadetex.model.User
import io.ktor.server.config.*
import java.util.*

class JwtService {
    
    val secret = "cadetex-secret-key" // En producción, usar variable de entorno
    val issuer = "cadetex-api"
    val audience = "cadetex-client"
    private val algorithm = Algorithm.HMAC256(secret)
    
    fun generateToken(user: User): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withClaim("organizationId", user.organizationId)
            .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 horas
            .sign(algorithm)
    }
    
    fun validateToken(token: String): JwtTokenData? {
        return try {
            val verifier = JWT.require(algorithm)
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
            
            val decoded = verifier.verify(token)
            
            JwtTokenData(
                userId = decoded.getClaim("userId").asString(),
                email = decoded.getClaim("email").asString(),
                role = decoded.getClaim("role").asString(),
                organizationId = decoded.getClaim("organizationId").asString()
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class JwtTokenData(
    val userId: String,
    val email: String,
    val role: String,
    val organizationId: String
)
