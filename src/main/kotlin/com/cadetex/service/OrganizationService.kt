package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.OrganizationRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("OrganizationService")

/**
 * Service para lógica de negocio de Organizaciones
 * Maneja validaciones y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class OrganizationService(
    private val organizationRepository: OrganizationRepository = OrganizationRepository()
) {

    /**
     * Buscar todas las organizaciones
     */
    suspend fun findAll(): Result<List<Organization>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val organizations = organizationRepository.findAll()
            success(organizations)
        } catch (e: Exception) {
            logger.error("Error buscando organizaciones: ${e.message}", e)
            error("Error al buscar organizaciones: ${e.message}")
        }
    }

    /**
     * Buscar organización por ID
     */
    suspend fun findById(id: String): Result<Organization> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val organization = organizationRepository.findById(uuid)
            if (organization != null) {
                success(organization)
            } else {
                error("Organización no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando organización: ${e.message}", e)
            error("Error al buscar la organización: ${e.message}")
        }
    }

    /**
     * Buscar organización por nombre
     */
    suspend fun findByName(name: String): Result<Organization?> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val organization = organizationRepository.findByName(name)
            success(organization)
        } catch (e: Exception) {
            logger.error("Error buscando organización por nombre: ${e.message}", e)
            error("Error al buscar la organización: ${e.message}")
        }
    }

    /**
     * Crear nueva organización
     * Lógica de negocio: validaciones
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateOrganizationRequest): Result<Organization> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val now = Clock.System.now()
            val nameTrimmed = request.name.trim()

            // Validación: nombre no puede estar vacío
            if (nameTrimmed.isBlank()) {
                return@newSuspendedTransaction error("El nombre de la organización es obligatorio")
            }

            // Validación: no puede existir una organización con el mismo nombre
            val existingOrganization = organizationRepository.findByName(nameTrimmed)
            if (existingOrganization != null) {
                return@newSuspendedTransaction error("Ya existe una organización con el nombre '$nameTrimmed'")
            }

            // Insertar organización
            val organizationId = organizationRepository.insert(
                name = nameTrimmed,
                createdAt = now,
                updatedAt = now
            )

            // Obtener la organización creada
            val createdOrganization = organizationRepository.findById(organizationId)
            if (createdOrganization != null) {
                success(createdOrganization)
            } else {
                error("Error al recuperar la organización creada")
            }
        } catch (e: Exception) {
            logger.error("Error creando organización: ${e.message}", e)
            error("Error al crear la organización: ${e.message}")
        }
    }

    /**
     * Actualizar organización existente
     * Lógica de negocio: validaciones
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateOrganizationRequest): Result<Organization> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val organizationId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener organización actual
            val currentOrganization = organizationRepository.findById(organizationId)
            if (currentOrganization == null) {
                return@newSuspendedTransaction error("Organización no encontrada con ID: $id")
            }

            // Validación: si se cambia el nombre, verificar que no exista otra con el mismo nombre
            updateRequest.name?.let { newName ->
                val nameTrimmed = newName.trim()
                if (nameTrimmed.isBlank()) {
                    return@newSuspendedTransaction error("El nombre de la organización no puede estar vacío")
                }
                val existingOrganization = organizationRepository.findByName(nameTrimmed)
                if (existingOrganization != null && existingOrganization.id != id) {
                    return@newSuspendedTransaction error("Ya existe una organización con el nombre '$nameTrimmed'")
                }
            }

            // Actualizar organización
            val updated = organizationRepository.update(
                id = organizationId,
                name = updateRequest.name?.trim(),
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar la organización")
            }

            // Obtener organización actualizada
            val updatedOrganization = organizationRepository.findById(organizationId)
            if (updatedOrganization != null) {
                success(updatedOrganization)
            } else {
                error("Error al recuperar la organización actualizada")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando organización: ${e.message}", e)
            error("Error al actualizar la organización: ${e.message}")
        }
    }

    /**
     * Eliminar organización
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = organizationRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Organización no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando organización: ${e.message}", e)
            error("Error al eliminar la organización: ${e.message}")
        }
    }
}

