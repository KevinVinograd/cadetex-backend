package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.CourierRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("CourierService")

/**
 * Service para lógica de negocio de Couriers
 * Maneja validaciones y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class CourierService(
    private val courierRepository: CourierRepository = CourierRepository()
) {

    /**
     * Buscar courier por ID
     */
    suspend fun findById(id: String): Result<Courier> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val courier = courierRepository.findById(uuid)
            if (courier != null) {
                success(courier)
            } else {
                error("Courier no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de courier inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando courier: ${e.message}", e)
            error("Error al buscar el courier: ${e.message}")
        }
    }

    /**
     * Buscar couriers por organización
     */
    suspend fun findByOrganization(organizationId: String): Result<List<Courier>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val couriers = courierRepository.findByOrganization(uuid)
            success(couriers)
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $organizationId")
        } catch (e: Exception) {
            logger.error("Error buscando couriers: ${e.message}", e)
            error("Error al buscar couriers: ${e.message}")
        }
    }

    /**
     * Buscar couriers activos por organización
     */
    suspend fun findActiveByOrganization(organizationId: String): Result<List<Courier>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val couriers = courierRepository.findActiveByOrganization(uuid)
            success(couriers)
        } catch (e: Exception) {
            logger.error("Error buscando couriers activos: ${e.message}", e)
            error("Error al buscar couriers: ${e.message}")
        }
    }

    /**
     * Buscar couriers por nombre
     */
    suspend fun searchByName(organizationId: String, name: String): Result<List<Courier>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val couriers = courierRepository.searchByName(uuid, name)
            success(couriers)
        } catch (e: Exception) {
            logger.error("Error buscando couriers por nombre: ${e.message}", e)
            error("Error al buscar couriers: ${e.message}")
        }
    }

    /**
     * Buscar couriers por teléfono
     */
    suspend fun searchByPhone(organizationId: String, phoneNumber: String): Result<List<Courier>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val couriers = courierRepository.searchByPhone(uuid, phoneNumber)
            success(couriers)
        } catch (e: Exception) {
            logger.error("Error buscando couriers por teléfono: ${e.message}", e)
            error("Error al buscar couriers: ${e.message}")
        }
    }

    /**
     * Buscar courier por userId
     */
    suspend fun findByUserId(userId: String): Result<Courier?> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(userId)
            val courier = courierRepository.findByUserId(uuid)
            success(courier)
        } catch (e: Exception) {
            logger.error("Error buscando courier por userId: ${e.message}", e)
            error("Error al buscar courier: ${e.message}")
        }
    }

    /**
     * Crear nuevo courier
     * Lógica de negocio: validaciones
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateCourierRequest): Result<Courier> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val now = Clock.System.now()
            val organizationId = UUID.fromString(request.organizationId)
            val userId = request.userId?.let { UUID.fromString(it) }

            // Validación: nombre no puede estar vacío
            if (request.name.trim().isBlank()) {
                return@newSuspendedTransaction error("El nombre del courier es obligatorio")
            }

            // Validación: teléfono no puede estar vacío
            if (request.phoneNumber.trim().isBlank()) {
                return@newSuspendedTransaction error("El teléfono del courier es obligatorio")
            }

            // Insertar courier
            val courierId = courierRepository.insert(
                userId = userId,
                organizationId = organizationId,
                name = request.name.trim(),
                phoneNumber = request.phoneNumber.trim(),
                address = request.address?.trim(),
                vehicleType = request.vehicleType?.trim(),
                isActive = true,
                createdAt = now,
                updatedAt = now
            )

            // Obtener el courier creado
            val createdCourier = courierRepository.findById(courierId)
            if (createdCourier != null) {
                success(createdCourier)
            } else {
                error("Error al recuperar el courier creado")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando courier: ${e.message}", e)
            error("Error al crear el courier: ${e.message}")
        }
    }

    /**
     * Actualizar courier existente
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateCourierRequest): Result<Courier> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val courierId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener courier actual
            val currentCourier = courierRepository.findById(courierId)
            if (currentCourier == null) {
                return@newSuspendedTransaction error("Courier no encontrado con ID: $id")
            }

            // Validaciones
            updateRequest.name?.let {
                if (it.trim().isBlank()) {
                    return@newSuspendedTransaction error("El nombre del courier no puede estar vacío")
                }
            }

            updateRequest.phoneNumber?.let {
                if (it.trim().isBlank()) {
                    return@newSuspendedTransaction error("El teléfono del courier no puede estar vacío")
                }
            }

            // Actualizar courier
            val updated = courierRepository.update(
                id = courierId,
                name = updateRequest.name?.trim(),
                phoneNumber = updateRequest.phoneNumber?.trim(),
                address = updateRequest.address?.trim(),
                vehicleType = updateRequest.vehicleType?.trim(),
                isActive = updateRequest.isActive,
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar el courier")
            }

            // Obtener courier actualizado
            val updatedCourier = courierRepository.findById(courierId)
            if (updatedCourier != null) {
                success(updatedCourier)
            } else {
                error("Error al recuperar el courier actualizado")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de courier inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando courier: ${e.message}", e)
            error("Error al actualizar el courier: ${e.message}")
        }
    }

    /**
     * Eliminar courier
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = courierRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Courier no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de courier inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando courier: ${e.message}", e)
            error("Error al eliminar el courier: ${e.message}")
        }
    }
}

