package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Client(
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateClientRequest(
    val organizationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class UpdateClientRequest(
    val name: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val isActive: Boolean? = null
)

