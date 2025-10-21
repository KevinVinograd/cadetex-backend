package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class UserRole {
    SUPERADMIN, ADMIN, COURIER
}

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateUserRequest(
    val organizationId: String,
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val role: UserRole? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)
