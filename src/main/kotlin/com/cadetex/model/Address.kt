package com.cadetex.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Address(
    val id: String = UUID.randomUUID().toString(),
    val street: String? = null,
    val streetNumber: String? = null,
    val addressComplement: String? = null,
    val city: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toFullAddressString(): String {
        val parts = mutableListOf<String>()
        if (!street.isNullOrBlank()) {
            if (!streetNumber.isNullOrBlank()) {
                parts.add("$street $streetNumber")
            } else {
                parts.add(street)
            }
        }
        if (!addressComplement.isNullOrBlank()) {
            parts.add(addressComplement)
        }
        val addressPart = parts.joinToString(" ").trim()
        
        val locationParts = mutableListOf<String>()
        if (addressPart.isNotBlank()) {
            locationParts.add(addressPart)
        }
        if (!city.isNullOrBlank()) {
            locationParts.add(city)
        }
        if (!province.isNullOrBlank()) {
            locationParts.add(province)
        }
        
        return locationParts.joinToString(", ")
    }
}

