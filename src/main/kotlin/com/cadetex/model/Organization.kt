package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Organization(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateOrganizationRequest(
    val name: String
)

@Serializable
data class UpdateOrganizationRequest(
    val name: String? = null
)
