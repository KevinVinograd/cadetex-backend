package com.cadetex.model

import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class Courier(
        val id: String = UUID.randomUUID().toString(),
        val userId: String? = null, // Reference to users table
        val organizationId: String,
        val name: String,
        val phoneNumber: String,
        val address: String? = null,
        val vehicleType: String? = null,
        val isActive: Boolean = true,
        val createdAt: String? = null,
        val updatedAt: String? = null
)

@Serializable
data class CreateCourierRequest(
        val userId: String? = null, // Optional - can create courier without user account
        val organizationId: String,
        val name: String,
        val phoneNumber: String,
        val address: String? = null,
        val vehicleType: String? = null
)

@Serializable
data class UpdateCourierRequest(
        val name: String? = null,
        val phoneNumber: String? = null,
        val address: String? = null,
        val vehicleType: String? = null,
        val isActive: Boolean? = null
)
