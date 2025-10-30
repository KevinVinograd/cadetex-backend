package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.AddressRepository
import com.cadetex.repository.TaskRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("TaskService")

/**
 * Service para lógica de negocio de Tasks
 * Maneja validaciones, creación/actualización de addresses, y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class TaskService(
    private val taskRepository: TaskRepository = TaskRepository(),
    private val addressRepository: AddressRepository = AddressRepository()
) {

    /**
     * Buscar tasks por organización
     */
    suspend fun findByOrganization(organizationId: String): Result<List<TaskResponse>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val tasks = taskRepository.findByOrganization(uuid)
                .map { (task, address) -> TaskResponse.fromTask(task, address) }
            success(tasks)
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $organizationId")
        } catch (e: Exception) {
            logger.error("Error buscando tasks: ${e.message}", e)
            error("Error al buscar tasks: ${e.message}")
        }
    }

    /**
     * Buscar tasks por courier
     */
    suspend fun findByCourier(courierId: String): Result<List<TaskResponse>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(courierId)
            val tasks = taskRepository.findByCourier(uuid)
                .map { (task, address) -> TaskResponse.fromTask(task, address) }
            success(tasks)
        } catch (e: IllegalArgumentException) {
            error("ID de courier inválido: $courierId")
        } catch (e: Exception) {
            logger.error("Error buscando tasks por courier: ${e.message}", e)
            error("Error al buscar tasks: ${e.message}")
        }
    }

    /**
     * Buscar tasks por status
     */
    suspend fun findByStatus(status: TaskStatus): Result<List<TaskResponse>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val tasks = taskRepository.findByStatus(status)
                .map { (task, address) -> TaskResponse.fromTask(task, address) }
            success(tasks)
        } catch (e: Exception) {
            logger.error("Error buscando tasks por status: ${e.message}", e)
            error("Error al buscar tasks: ${e.message}")
        }
    }

    /**
     * Buscar task por ID
     */
    suspend fun findById(id: String): Result<TaskResponse> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val taskPair = taskRepository.findById(uuid)
            if (taskPair != null) {
                val (task, address) = taskPair
                success(TaskResponse.fromTask(task, address))
            } else {
                error("Task no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando task: ${e.message}", e)
            error("Error al buscar la task: ${e.message}")
        }
    }

    /**
     * Buscar tasks con filtros
     */
    suspend fun findFiltered(
        organizationId: String,
        courierId: String? = null,
        unassigned: Boolean = false,
        statuses: List<TaskStatus> = emptyList()
    ): Result<List<TaskResponse>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val orgUuid = UUID.fromString(organizationId)
            val courierUuid = courierId?.let { UUID.fromString(it) }
            
            val tasks = taskRepository.findFiltered(
                organizationId = orgUuid,
                courierId = courierUuid,
                unassigned = unassigned,
                statuses = statuses
            ).map { (task, address) -> TaskResponse.fromTask(task, address) }
            
            success(tasks)
        } catch (e: IllegalArgumentException) {
            error("IDs inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error buscando tasks filtradas: ${e.message}", e)
            error("Error al buscar tasks: ${e.message}")
        }
    }

    /**
     * Crear nueva task
     * Lógica de negocio: validaciones, creación de address si es necesario
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateTaskRequest): Result<TaskResponse> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val now = Clock.System.now()
            val organizationId = UUID.fromString(request.organizationId)
            
            // Validaciones de UUIDs
            val clientId = request.clientId?.let { UUID.fromString(it) }
            val providerId = request.providerId?.let { UUID.fromString(it) }
            val courierId = request.courierId?.let { UUID.fromString(it) }
            val linkedTaskId = request.linkedTaskId?.let { UUID.fromString(it) }

            // Validación: no puede tener clientId y providerId al mismo tiempo
            if (clientId != null && providerId != null) {
                return@newSuspendedTransaction error("Una task no puede tener clientId y providerId al mismo tiempo")
            }

            // Validación: si hay referenceNumber, verificar que no exista otro con el mismo en la organización
            request.referenceNumber?.let { refNumber ->
                if (taskRepository.existsByReferenceNumber(organizationId, refNumber)) {
                    return@newSuspendedTransaction error("Ya existe una task con el número de referencia '$refNumber' en esta organización")
                }
            }

            // Crear address si se proporciona addressOverride
            var addressOverrideId: UUID? = null
            if (request.addressOverride != null) {
                val addressData = request.addressOverride
                if (addressData.street != null || addressData.city != null) {
                    addressOverrideId = addressRepository.insert(
                        street = addressData.street,
                        streetNumber = addressData.streetNumber,
                        addressComplement = addressData.addressComplement,
                        city = addressData.city,
                        province = addressData.province,
                        postalCode = addressData.postalCode,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }

            // Insertar task
            val taskId = taskRepository.insert(
                organizationId = organizationId,
                type = request.type,
                referenceNumber = request.referenceNumber,
                clientId = clientId,
                providerId = providerId,
                addressOverrideId = addressOverrideId,
                contact = request.contact,
                courierId = courierId,
                status = request.status,
                priority = request.priority,
                scheduledDate = request.scheduledDate,
                notes = request.notes,
                courierNotes = request.courierNotes,
                mbl = request.mbl,
                hbl = request.hbl,
                freightCert = request.freightCert,
                foCert = request.foCert,
                bunkerCert = request.bunkerCert,
                linkedTaskId = linkedTaskId,
                receiptPhotoUrl = request.receiptPhotoUrl,
                photoRequired = request.photoRequired,
                createdAt = now,
                updatedAt = now
            )

            // Obtener la task creada con el address completo
            val createdTaskPair = taskRepository.findById(taskId)
            if (createdTaskPair != null) {
                val (task, address) = createdTaskPair
                success(TaskResponse.fromTask(task, address))
            } else {
                error("Error al recuperar la task creada")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando task: ${e.message}", e)
            error("Error al crear la task: ${e.message}")
        }
    }

    /**
     * Actualizar task existente
     * Lógica de negocio: validaciones, actualización/creación de address si es necesario
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateTaskRequest): Result<TaskResponse> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val taskId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener task actual
            val currentTaskPair = taskRepository.findById(taskId)
            if (currentTaskPair == null) {
                return@newSuspendedTransaction error("Task no encontrada con ID: $id")
            }
            val (currentTask, _) = currentTaskPair

            // Validaciones
            if (updateRequest.clientId != null && updateRequest.providerId != null) {
                return@newSuspendedTransaction error("Una task no puede tener clientId y providerId al mismo tiempo")
            }

            // Validación de referenceNumber si se actualiza
            updateRequest.referenceNumber?.let { refNumber ->
                val organizationId = UUID.fromString(currentTask.organizationId)
                if (taskRepository.existsByReferenceNumber(organizationId, refNumber)) {
                    // Verificar que no sea la misma task
                    val existingTaskPair = taskRepository.findById(taskId)
                    val existingTask = existingTaskPair?.first
                    if (existingTask?.referenceNumber != refNumber) {
                        return@newSuspendedTransaction error("Ya existe una task con el número de referencia '$refNumber' en esta organización")
                    }
                }
            }

            // Manejar addressOverride: actualizar existente o crear nuevo
            var addressOverrideId: UUID? = currentTask.addressOverrideId?.let { UUID.fromString(it) }
            
            if (updateRequest.addressOverride != null) {
                val addressToUpdate = updateRequest.addressOverride
                
                if (addressOverrideId != null) {
                    // Actualizar address existente
                    val updated = addressRepository.update(
                        id = addressOverrideId,
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
                        addressOverrideId = addressRepository.insert(
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

            // Parsear UUIDs para la actualización
            val clientId = updateRequest.clientId?.let { UUID.fromString(it) }
            val providerId = updateRequest.providerId?.let { UUID.fromString(it) }
            val courierId = updateRequest.courierId?.let { UUID.fromString(it) }
            val linkedTaskId = updateRequest.linkedTaskId?.let { UUID.fromString(it) }

            // Actualizar task
            val updated = taskRepository.update(
                id = taskId,
                type = updateRequest.type,
                referenceNumber = updateRequest.referenceNumber,
                clientId = clientId,
                providerId = providerId,
                addressOverrideId = addressOverrideId,
                contact = updateRequest.contact,
                courierId = courierId,
                unassignCourier = updateRequest.unassignCourier == true,
                status = updateRequest.status,
                priority = updateRequest.priority,
                scheduledDate = updateRequest.scheduledDate,
                notes = updateRequest.notes,
                courierNotes = updateRequest.courierNotes,
                photoRequired = updateRequest.photoRequired,
                mbl = updateRequest.mbl,
                hbl = updateRequest.hbl,
                freightCert = updateRequest.freightCert,
                foCert = updateRequest.foCert,
                bunkerCert = updateRequest.bunkerCert,
                linkedTaskId = linkedTaskId,
                receiptPhotoUrl = updateRequest.receiptPhotoUrl,
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar la task")
            }

            // Obtener task actualizada
            val updatedTaskPair = taskRepository.findById(taskId)
            if (updatedTaskPair != null) {
                val (task, address) = updatedTaskPair
                success(TaskResponse.fromTask(task, address))
            } else {
                error("Error al recuperar la task actualizada")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando task: ${e.message}", e)
            error("Error al actualizar la task: ${e.message}")
        }
    }

    /**
     * Eliminar task
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = taskRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Task no encontrada con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de task inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando task: ${e.message}", e)
            error("Error al eliminar la task: ${e.message}")
        }
    }
}

