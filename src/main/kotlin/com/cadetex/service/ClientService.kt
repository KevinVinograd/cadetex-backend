package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.AddressRepository
import com.cadetex.repository.ClientRepository
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("ClientService")

/**
 * Service para lógica de negocio de Clientes
 * Maneja validaciones, creación/actualización de addresses, y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class ClientService(
    private val clientRepository: ClientRepository = ClientRepository(),
    private val addressRepository: AddressRepository = AddressRepository()
) {

    /**
     * Buscar cliente por ID
     */
    suspend fun findById(id: String): Result<Client> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val client = clientRepository.findById(uuid)
            if (client != null) {
                success(client)
            } else {
                error("Cliente no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de cliente inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando cliente: ${e.message}", e)
            error("Error al buscar el cliente: ${e.message}")
        }
    }

    /**
     * Buscar clientes por organización
     */
    suspend fun findByOrganization(organizationId: String): Result<List<Client>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val clients = clientRepository.findByOrganization(uuid)
            success(clients)
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $organizationId")
        } catch (e: Exception) {
            logger.error("Error buscando clientes: ${e.message}", e)
            error("Error al buscar clientes: ${e.message}")
        }
    }

    /**
     * Buscar clientes por nombre
     */
    suspend fun searchByName(organizationId: String, name: String): Result<List<Client>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val clients = clientRepository.searchByName(uuid, name)
            success(clients)
        } catch (e: Exception) {
            logger.error("Error buscando clientes por nombre: ${e.message}", e)
            error("Error al buscar clientes: ${e.message}")
        }
    }

    /**
     * Buscar clientes por ciudad
     */
    suspend fun searchByCity(organizationId: String, city: String): Result<List<Client>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val clients = clientRepository.searchByCity(uuid, city)
            success(clients)
        } catch (e: Exception) {
            logger.error("Error buscando clientes por ciudad: ${e.message}", e)
            error("Error al buscar clientes: ${e.message}")
        }
    }

    /**
     * Crear nuevo cliente
     * Lógica de negocio: validaciones, creación de address si es necesario
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateClientRequest): Result<Client> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val now = Clock.System.now()
            val organizationId = UUID.fromString(request.organizationId)
            val nameTrimmed = request.name.trim()

            // Validación: nombre no puede estar vacío
            if (nameTrimmed.isBlank()) {
                return@newSuspendedTransaction error("El nombre del cliente es obligatorio")
            }

            // Validación: no puede existir un cliente con el mismo nombre en la misma organización
            if (clientRepository.existsByName(organizationId, nameTrimmed)) {
                return@newSuspendedTransaction error("Ya existe un cliente con el nombre '$nameTrimmed' en esta organización")
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

            // Insertar cliente
            val clientId = clientRepository.insert(
                organizationId = organizationId,
                name = nameTrimmed,
                addressId = addressId,
                phoneNumber = request.phoneNumber,
                email = request.email,
                isActive = request.isActive,
                createdAt = now,
                updatedAt = now
            )

            // Obtener el cliente creado con el address completo (si existe)
            val createdClient = clientRepository.findById(clientId)
            if (createdClient != null) {
                success(createdClient)
            } else {
                error("Error al recuperar el cliente creado")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando cliente: ${e.message}", e)
            error("Error al crear el cliente: ${e.message}")
        }
    }

    /**
     * Actualizar cliente existente
     * Lógica de negocio: validaciones, actualización/creación de address si es necesario
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateClientRequest): Result<Client> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val clientId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener cliente actual
            val currentClient = clientRepository.findById(clientId)
            if (currentClient == null) {
                return@newSuspendedTransaction error("Cliente no encontrado con ID: $id")
            }

            val organizationId = UUID.fromString(currentClient.organizationId)

            // Validación: si se cambia el nombre, verificar que no exista otro con el mismo nombre
            updateRequest.name?.let { newName ->
                val nameTrimmed = newName.trim()
                if (nameTrimmed.isBlank()) {
                    return@newSuspendedTransaction error("El nombre del cliente no puede estar vacío")
                }
                if (clientRepository.existsByNameExcludingId(organizationId, nameTrimmed, clientId)) {
                    return@newSuspendedTransaction error("Ya existe un cliente con el nombre '$nameTrimmed' en esta organización")
                }
            }

            // Manejar address: actualizar existente o crear nuevo
            var addressId: UUID? = currentClient.address?.id?.let { UUID.fromString(it) }
            
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

            // Actualizar cliente
            val updated = clientRepository.update(
                id = clientId,
                name = updateRequest.name?.trim(),
                addressId = addressId,
                phoneNumber = updateRequest.phoneNumber,
                email = updateRequest.email,
                isActive = updateRequest.isActive,
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar el cliente")
            }

            // Obtener cliente actualizado
            val updatedClient = clientRepository.findById(clientId)
            if (updatedClient != null) {
                success(updatedClient)
            } else {
                error("Error al recuperar el cliente actualizado")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de cliente inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando cliente: ${e.message}", e)
            error("Error al actualizar el cliente: ${e.message}")
        }
    }

    /**
     * Eliminar cliente
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = clientRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Cliente no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de cliente inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando cliente: ${e.message}", e)
            error("Error al eliminar el cliente: ${e.message}")
        }
    }
}

