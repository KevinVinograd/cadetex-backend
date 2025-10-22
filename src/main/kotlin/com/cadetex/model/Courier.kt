package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Courier(
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val address: String? = null,
    val vehicleType: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateCourierRequest(
    val organizationId: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val address: String? = null,
    val vehicleType: String? = null
)

@Serializable
data class UpdateCourierRequest(
    val name: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val address: String? = null,
    val vehicleType: String? = null,
    val isActive: Boolean? = null
)

