package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.AddressRepository
import com.cadetex.repository.ProviderRepository
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("ProviderService")

/**
 * Service para lógica de negocio de Proveedores
 * Maneja validaciones, creación/actualización de addresses, y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class ProviderService(
    private val providerRepository: ProviderRepository = ProviderRepository(),
    private val addressRepository: AddressRepository = AddressRepository()
) {

    /**
     * Buscar proveedor por ID
     */
    suspend fun findById(id: String): Result<Provider> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val provider = providerRepository.findById(uuid)
            if (provider != null) {
                success(provider)
            } else {
                error("Proveedor no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de proveedor inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando proveedor: ${e.message}", e)
            error("Error al buscar el proveedor: ${e.message}")
        }
    }

    /**
     * Buscar proveedores por organización
     */
    suspend fun findByOrganization(organizationId: String): Result<List<Provider>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val providers = providerRepository.findByOrganization(uuid)
            success(providers)
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $organizationId")
        } catch (e: Exception) {
            logger.error("Error buscando proveedores: ${e.message}", e)
            error("Error al buscar proveedores: ${e.message}")
        }
    }

    /**
     * Buscar proveedores por nombre
     */
    suspend fun searchByName(organizationId: String, name: String): Result<List<Provider>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val providers = providerRepository.searchByName(uuid, name)
            success(providers)
        } catch (e: Exception) {
            logger.error("Error buscando proveedores por nombre: ${e.message}", e)
            error("Error al buscar proveedores: ${e.message}")
        }
    }

    /**
     * Buscar proveedores por ciudad
     */
    suspend fun searchByCity(organizationId: String, city: String): Result<List<Provider>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val providers = providerRepository.searchByCity(uuid, city)
            success(providers)
        } catch (e: Exception) {
            logger.error("Error buscando proveedores por ciudad: ${e.message}", e)
            error("Error al buscar proveedores: ${e.message}")
        }
    }

    /**
     * Crear nuevo proveedor
     * Lógica de negocio: validaciones, creación de address si es necesario
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateProviderRequest): Result<Provider> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val now = Clock.System.now()
            val organizationId = UUID.fromString(request.organizationId)
            val nameTrimmed = request.name.trim()

            // Validación: nombre no puede estar vacío
            if (nameTrimmed.isBlank()) {
                return@newSuspendedTransaction error("El nombre del proveedor es obligatorio")
            }

            // Validación: no puede existir un proveedor con el mismo nombre en la misma organización
            if (providerRepository.existsByName(organizationId, nameTrimmed)) {
                return@newSuspendedTransaction error("Ya existe un proveedor con el nombre '$nameTrimmed' en esta organización")
            }

            // Crear address si se proporciona
            val addressId: UUID? = request.address?.let { addressData ->
                if (addressData.street != null || addressData.city != null) {
                    addressRepository.insert(
                        street = addressData.street,
                        streetNumber = addressData.streetNumber,
                        addressComplement = addressData.addressComplement,
                        city = addressData.city,
                        province = addressData.province,
                        postalCode = addressData.postalCode,
                        createdAt = now,
                        updatedAt = now
                    )
                } else {
                    null
                }
            }

            // Insertar proveedor
            val providerId = providerRepository.insert(
                organizationId = organizationId,
                name = nameTrimmed,
                addressId = addressId,
                contactName = request.contactName,
                contactPhone = request.contactPhone,
                isActive = request.isActive,
                createdAt = now,
                updatedAt = now
            )

            // Obtener el proveedor creado con el address completo (si existe)
            val createdProvider = providerRepository.findById(providerId)
            if (createdProvider != null) {
                success(createdProvider)
            } else {
                error("Error al recuperar el proveedor creado")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando proveedor: ${e.message}", e)
            error("Error al crear el proveedor: ${e.message}")
        }
    }

    /**
     * Actualizar proveedor existente
     * Lógica de negocio: validaciones, actualización/creación de address si es necesario
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateProviderRequest): Result<Provider> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val providerId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener proveedor actual
            val currentProvider = providerRepository.findById(providerId)
            if (currentProvider == null) {
                return@newSuspendedTransaction error("Proveedor no encontrado con ID: $id")
            }

            val organizationId = UUID.fromString(currentProvider.organizationId)

            // Validación: si se cambia el nombre, verificar que no exista otro con el mismo nombre
            updateRequest.name?.let { newName ->
                val nameTrimmed = newName.trim()
                if (nameTrimmed.isBlank()) {
                    return@newSuspendedTransaction error("El nombre del proveedor no puede estar vacío")
                }
                if (providerRepository.existsByNameExcludingId(organizationId, nameTrimmed, providerId)) {
                    return@newSuspendedTransaction error("Ya existe un proveedor con el nombre '$nameTrimmed' en esta organización")
                }
            }

            // Manejar address: actualizar existente o crear nuevo
            var addressId: UUID? = currentProvider.address?.id?.let { UUID.fromString(it) }
            
            if (updateRequest.address != null) {
                val addressToUpdate = updateRequest.address
                
                if (addressId != null) {
                    // Actualizar address existente
                    val updated = addressRepository.update(
                        id = addressId,
                        street = addressToUpdate.street,
                        streetNumber = addressToUpdate.streetNumber,
                        addressComplement = addressToUpdate.addressComplement,
                        city = addressToUpdate.city,
                        province = addressToUpdate.province,
                        postalCode = addressToUpdate.postalCode
                    )
                    if (!updated) {
                        return@newSuspendedTransaction error("Error al actualizar la dirección")
                    }
                } else {
                    // Crear nueva address
                    if (addressToUpdate.street != null || addressToUpdate.city != null) {
                        addressId = addressRepository.insert(
                            street = addressToUpdate.street,
                            streetNumber = addressToUpdate.streetNumber,
                            addressComplement = addressToUpdate.addressComplement,
                            city = addressToUpdate.city,
                            province = addressToUpdate.province,
                            postalCode = addressToUpdate.postalCode,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }
            }

            // Actualizar proveedor
            val updated = providerRepository.update(
                id = providerId,
                name = updateRequest.name?.trim(),
                addressId = addressId,
                contactName = updateRequest.contactName,
                contactPhone = updateRequest.contactPhone,
                isActive = updateRequest.isActive,
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar el proveedor")
            }

            // Obtener proveedor actualizado
            val updatedProvider = providerRepository.findById(providerId)
            if (updatedProvider != null) {
                success(updatedProvider)
            } else {
                error("Error al recuperar el proveedor actualizado")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de proveedor inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando proveedor: ${e.message}", e)
            error("Error al actualizar el proveedor: ${e.message}")
        }
    }

    /**
     * Eliminar proveedor
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = providerRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Proveedor no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de proveedor inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando proveedor: ${e.message}", e)
            error("Error al eliminar el proveedor: ${e.message}")
        }
    }
}

