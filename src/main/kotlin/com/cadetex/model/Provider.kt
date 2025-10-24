package com.cadetex.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.*

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Provider(
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val name: String,
    val address: String,
    val city: String? = null,
    val province: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    @EncodeDefault val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class CreateProviderRequest(
    val organizationId: String,
    val name: String,
    val address: String,
    val city: String? = null,
    val province: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    @EncodeDefault val isActive: Boolean = true
)

@Serializable
data class UpdateProviderRequest(
    val name: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val isActive: Boolean? = null
)

