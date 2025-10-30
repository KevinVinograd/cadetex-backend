package com.cadetex.repository

import com.cadetex.database.tables.Addresses
import com.cadetex.database.tables.Tasks
import com.cadetex.database.tables.Clients
import com.cadetex.database.tables.Providers
import com.cadetex.database.tables.Couriers
import com.cadetex.model.Address
import com.cadetex.model.Task
import com.cadetex.model.TaskStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en TaskService
 */
class TaskRepository {

    // Alias para Addresses: override, client address, provider address
    private val AddressOverrideAlias = Addresses.alias("address_override")
    private val ClientAddressAlias = Addresses.alias("client_address")
    private val ProviderAddressAlias = Addresses.alias("provider_address")

    /**
     * Construye la query base con todos los joins necesarios
     * Debe ejecutarse dentro de una transacción activa
     */
    fun buildTasksQuery() = Tasks
        .leftJoin(Clients, { Tasks.clientId }, { Clients.id })
        .leftJoin(Providers, { Tasks.providerId }, { Providers.id })
        .leftJoin(Couriers, { Tasks.courierId }, { Couriers.id })
        .leftJoin(AddressOverrideAlias, { Tasks.addressOverrideId }, { AddressOverrideAlias[Addresses.id] })
        .leftJoin(ClientAddressAlias, { Clients.addressId }, { ClientAddressAlias[Addresses.id] })
        .leftJoin(ProviderAddressAlias, { Providers.addressId }, { ProviderAddressAlias[Addresses.id] })
        .select(
            Tasks.id,
            Tasks.organizationId,
            Tasks.type,
            Tasks.referenceNumber,
            Tasks.clientId,
            Clients.name,
            Tasks.providerId,
            Providers.name,
            Tasks.addressOverrideId,
            Tasks.contact,
            Tasks.courierId,
            Couriers.name,
            Tasks.status,
            Tasks.priority,
            Tasks.scheduledDate,
            Tasks.notes,
            Tasks.courierNotes,
            Tasks.mbl,
            Tasks.hbl,
            Tasks.freightCert,
            Tasks.foCert,
            Tasks.bunkerCert,
            Tasks.linkedTaskId,
            Tasks.receiptPhotoUrl,
            Tasks.photoRequired,
            Tasks.createdAt,
            Tasks.updatedAt,
            // Campos de AddressOverrideAlias
            AddressOverrideAlias[Addresses.id],
            AddressOverrideAlias[Addresses.street],
            AddressOverrideAlias[Addresses.streetNumber],
            AddressOverrideAlias[Addresses.addressComplement],
            AddressOverrideAlias[Addresses.city],
            AddressOverrideAlias[Addresses.province],
            AddressOverrideAlias[Addresses.postalCode],
            AddressOverrideAlias[Addresses.createdAt],
            AddressOverrideAlias[Addresses.updatedAt],
            // Campos de ClientAddressAlias
            ClientAddressAlias[Addresses.id],
            ClientAddressAlias[Addresses.street],
            ClientAddressAlias[Addresses.streetNumber],
            ClientAddressAlias[Addresses.addressComplement],
            ClientAddressAlias[Addresses.city],
            ClientAddressAlias[Addresses.province],
            ClientAddressAlias[Addresses.postalCode],
            ClientAddressAlias[Addresses.createdAt],
            ClientAddressAlias[Addresses.updatedAt],
            // Campos de ProviderAddressAlias
            ProviderAddressAlias[Addresses.id],
            ProviderAddressAlias[Addresses.street],
            ProviderAddressAlias[Addresses.streetNumber],
            ProviderAddressAlias[Addresses.addressComplement],
            ProviderAddressAlias[Addresses.city],
            ProviderAddressAlias[Addresses.province],
            ProviderAddressAlias[Addresses.postalCode],
            ProviderAddressAlias[Addresses.createdAt],
            ProviderAddressAlias[Addresses.updatedAt]
        )

    /**
     * Mapea ResultRow a Task (sin address)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun rowToTaskWithJoins(row: ResultRow): Task {
        val taskAddressOverrideId = row.getOrNull(Tasks.addressOverrideId)?.value
        val clientId = row.getOrNull(Tasks.clientId)?.value
        val providerId = row.getOrNull(Tasks.providerId)?.value

        return Task(
            id = row[Tasks.id].value.toString(),
            organizationId = row[Tasks.organizationId].value.toString(),
            type = com.cadetex.model.TaskType.valueOf(row[Tasks.type]),
            referenceNumber = row[Tasks.referenceNumber],
            clientId = clientId?.toString(),
            clientName = row.getOrNull(Clients.name),
            providerId = providerId?.toString(),
            providerName = row.getOrNull(Providers.name),
            addressOverrideId = taskAddressOverrideId?.toString(),
            contact = row[Tasks.contact],
            courierId = row[Tasks.courierId]?.value?.toString(),
            courierName = row.getOrNull(Couriers.name),
            status = TaskStatus.valueOf(row[Tasks.status]),
            priority = com.cadetex.model.TaskPriority.valueOf(row[Tasks.priority]),
            scheduledDate = row[Tasks.scheduledDate],
            notes = row[Tasks.notes],
            courierNotes = row[Tasks.courierNotes],
            mbl = row[Tasks.mbl],
            hbl = row[Tasks.hbl],
            freightCert = row[Tasks.freightCert],
            foCert = row[Tasks.foCert],
            bunkerCert = row[Tasks.bunkerCert],
            linkedTaskId = row[Tasks.linkedTaskId]?.value?.toString(),
            receiptPhotoUrl = row[Tasks.receiptPhotoUrl],
            photoRequired = row[Tasks.photoRequired],
            createdAt = row[Tasks.createdAt].toString(),
            updatedAt = row[Tasks.updatedAt].toString()
        )
    }

    /**
     * Calcula la dirección efectiva desde los joins
     * Prioridad: addressOverrideId > clientId > providerId
     * Debe ejecutarse dentro de una transacción activa
     */
    fun rowToAddressFromJoins(row: ResultRow): Address? {
        val taskAddressOverrideId = row.getOrNull(Tasks.addressOverrideId)?.value
        val clientId = row.getOrNull(Tasks.clientId)?.value
        val providerId = row.getOrNull(Tasks.providerId)?.value
        
        return when {
            taskAddressOverrideId != null -> {
                row.getOrNull(AddressOverrideAlias[Addresses.id])?.value?.let {
                    Address(
                        id = it.toString(),
                        street = row.getOrNull(AddressOverrideAlias[Addresses.street]),
                        streetNumber = row.getOrNull(AddressOverrideAlias[Addresses.streetNumber]),
                        addressComplement = row.getOrNull(AddressOverrideAlias[Addresses.addressComplement]),
                        city = row.getOrNull(AddressOverrideAlias[Addresses.city]),
                        province = row.getOrNull(AddressOverrideAlias[Addresses.province]),
                        postalCode = row.getOrNull(AddressOverrideAlias[Addresses.postalCode]),
                        createdAt = row.getOrNull(AddressOverrideAlias[Addresses.createdAt])?.toString(),
                        updatedAt = row.getOrNull(AddressOverrideAlias[Addresses.updatedAt])?.toString()
                    )
                }
            }
            clientId != null -> {
                row.getOrNull(ClientAddressAlias[Addresses.id])?.value?.let {
                    Address(
                        id = it.toString(),
                        street = row.getOrNull(ClientAddressAlias[Addresses.street]),
                        streetNumber = row.getOrNull(ClientAddressAlias[Addresses.streetNumber]),
                        addressComplement = row.getOrNull(ClientAddressAlias[Addresses.addressComplement]),
                        city = row.getOrNull(ClientAddressAlias[Addresses.city]),
                        province = row.getOrNull(ClientAddressAlias[Addresses.province]),
                        postalCode = row.getOrNull(ClientAddressAlias[Addresses.postalCode]),
                        createdAt = row.getOrNull(ClientAddressAlias[Addresses.createdAt])?.toString(),
                        updatedAt = row.getOrNull(ClientAddressAlias[Addresses.updatedAt])?.toString()
                    )
                }
            }
            providerId != null -> {
                row.getOrNull(ProviderAddressAlias[Addresses.id])?.value?.let {
                    Address(
                        id = it.toString(),
                        street = row.getOrNull(ProviderAddressAlias[Addresses.street]),
                        streetNumber = row.getOrNull(ProviderAddressAlias[Addresses.streetNumber]),
                        addressComplement = row.getOrNull(ProviderAddressAlias[Addresses.addressComplement]),
                        city = row.getOrNull(ProviderAddressAlias[Addresses.city]),
                        province = row.getOrNull(ProviderAddressAlias[Addresses.province]),
                        postalCode = row.getOrNull(ProviderAddressAlias[Addresses.postalCode]),
                        createdAt = row.getOrNull(ProviderAddressAlias[Addresses.createdAt])?.toString(),
                        updatedAt = row.getOrNull(ProviderAddressAlias[Addresses.updatedAt])?.toString()
                    )
                }
            }
            else -> null
        }
    }

    /**
     * Buscar tasks por organización
     * Usa índice: idx_tasks_organization_id, idx_tasks_created_at
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByOrganization(organizationId: UUID): List<Pair<Task, Address?>> {
        return buildTasksQuery()
            .where { Tasks.organizationId eq organizationId }
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { row ->
                val task = rowToTaskWithJoins(row)
                val address = rowToAddressFromJoins(row)
                Pair(task, address)
            }
    }

    /**
     * Buscar tasks por courier
     * Usa índice: idx_tasks_courier_id, idx_tasks_created_at
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByCourier(courierId: UUID): List<Pair<Task, Address?>> {
        return buildTasksQuery()
            .where { Tasks.courierId eq courierId }
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { row ->
                val task = rowToTaskWithJoins(row)
                val address = rowToAddressFromJoins(row)
                Pair(task, address)
            }
    }

    /**
     * Buscar tasks por status
     * Usa índice: idx_tasks_status, idx_tasks_created_at
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByStatus(status: TaskStatus): List<Pair<Task, Address?>> {
        return buildTasksQuery()
            .where { Tasks.status eq status.name }
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { row ->
                val task = rowToTaskWithJoins(row)
                val address = rowToAddressFromJoins(row)
                Pair(task, address)
            }
    }

    /**
     * Buscar task por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): Pair<Task, Address?>? {
        return buildTasksQuery()
            .where { Tasks.id eq id }
            .singleOrNull()
            ?.let { row ->
                val task = rowToTaskWithJoins(row)
                val address = rowToAddressFromJoins(row)
                Pair(task, address)
            }
    }

    /**
     * Verificar si existe task por referenceNumber
     * Usa índice: idx_tasks_org_reference
     * Debe ejecutarse dentro de una transacción activa
     */
    fun existsByReferenceNumber(organizationId: UUID, referenceNumber: String): Boolean {
        if (referenceNumber.isBlank()) return false
        
        return Tasks
            .select(Tasks.id)
            .where { 
                (Tasks.organizationId eq organizationId) and 
                (Tasks.referenceNumber eq referenceNumber)
            }
            .singleOrNull() != null
    }

    /**
     * Buscar tasks con filtros
     * Usa índices según los filtros aplicados
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findFiltered(
        organizationId: UUID,
        courierId: UUID? = null,
        unassigned: Boolean = false,
        statuses: List<TaskStatus> = emptyList()
    ): List<Pair<Task, Address?>> {
        var query = buildTasksQuery()
            .where { Tasks.organizationId eq organizationId }
        
        // Aplicar filtros
        if (courierId != null) {
            query = query.andWhere { Tasks.courierId eq courierId }
        } else if (unassigned) {
            query = query.andWhere { Tasks.courierId.isNull() }
        }
        
        if (statuses.isNotEmpty()) {
            val statusNames = statuses.map { it.name }
            query = query.andWhere { Tasks.status.inList(statusNames) }
        }
        
        return query
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { row ->
                val task = rowToTaskWithJoins(row)
                val address = rowToAddressFromJoins(row)
                Pair(task, address)
            }
    }

    /**
     * Insertar nueva task
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        organizationId: UUID,
        type: com.cadetex.model.TaskType,
        referenceNumber: String? = null,
        clientId: UUID? = null,
        providerId: UUID? = null,
        addressOverrideId: UUID? = null,
        contact: String? = null,
        courierId: UUID? = null,
        status: TaskStatus,
        priority: com.cadetex.model.TaskPriority,
        scheduledDate: String? = null,
        notes: String? = null,
        courierNotes: String? = null,
        mbl: String? = null,
        hbl: String? = null,
        freightCert: Boolean = false,
        foCert: Boolean = false,
        bunkerCert: Boolean = false,
        linkedTaskId: UUID? = null,
        receiptPhotoUrl: String? = null,
        photoRequired: Boolean = false,
        createdAt: kotlinx.datetime.Instant,
        updatedAt: kotlinx.datetime.Instant
    ): UUID {
        val id = UUID.randomUUID()

        Tasks.insert {
            it[Tasks.id] = id
            it[Tasks.organizationId] = organizationId
            it[Tasks.type] = type.name
            it[Tasks.referenceNumber] = referenceNumber
            it[Tasks.clientId] = clientId
            it[Tasks.providerId] = providerId
            it[Tasks.addressOverrideId] = addressOverrideId
            it[Tasks.contact] = contact
            it[Tasks.courierId] = courierId
            it[Tasks.status] = status.name
            it[Tasks.priority] = priority.name
            it[Tasks.scheduledDate] = scheduledDate
            it[Tasks.notes] = notes
            it[Tasks.courierNotes] = courierNotes
            it[Tasks.mbl] = mbl
            it[Tasks.hbl] = hbl
            it[Tasks.freightCert] = freightCert
            it[Tasks.foCert] = foCert
            it[Tasks.bunkerCert] = bunkerCert
            it[Tasks.linkedTaskId] = linkedTaskId
            it[Tasks.receiptPhotoUrl] = receiptPhotoUrl
            it[Tasks.photoRequired] = photoRequired
            it[Tasks.createdAt] = createdAt
            it[Tasks.updatedAt] = updatedAt
        }

        return id
    }

    /**
     * Actualizar task existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        type: com.cadetex.model.TaskType? = null,
        referenceNumber: String? = null,
        clientId: UUID? = null,
        providerId: UUID? = null,
        addressOverrideId: UUID? = null,
        contact: String? = null,
        courierId: UUID? = null,
        unassignCourier: Boolean = false,
        status: TaskStatus? = null,
        priority: com.cadetex.model.TaskPriority? = null,
        scheduledDate: String? = null,
        notes: String? = null,
        courierNotes: String? = null,
        photoRequired: Boolean? = null,
        mbl: String? = null,
        hbl: String? = null,
        freightCert: Boolean? = null,
        foCert: Boolean? = null,
        bunkerCert: Boolean? = null,
        linkedTaskId: UUID? = null,
        receiptPhotoUrl: String? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = Tasks.update({ Tasks.id eq id }) { row ->
            type?.let { row[Tasks.type] = it.name }
            referenceNumber?.let { row[Tasks.referenceNumber] = it }
            // Manejar clientId y providerId - solo uno puede estar presente
            if (clientId != null) {
                row[Tasks.clientId] = clientId
                row[Tasks.providerId] = null
            } else if (providerId != null) {
                row[Tasks.providerId] = providerId
                row[Tasks.clientId] = null
            }
            addressOverrideId?.let { row[Tasks.addressOverrideId] = it }
            contact?.let { row[Tasks.contact] = it }
            // Handle courierId assignment and unassignment
            if (unassignCourier) {
                row[Tasks.courierId] = null
            } else {
                courierId?.let { row[Tasks.courierId] = it }
            }
            status?.let { row[Tasks.status] = it.name }
            priority?.let { row[Tasks.priority] = it.name }
            scheduledDate?.let { row[Tasks.scheduledDate] = it }
            notes?.let { row[Tasks.notes] = it }
            courierNotes?.let { row[Tasks.courierNotes] = it }
            photoRequired?.let { row[Tasks.photoRequired] = it }
            mbl?.let { row[Tasks.mbl] = it }
            hbl?.let { row[Tasks.hbl] = it }
            freightCert?.let { row[Tasks.freightCert] = it }
            foCert?.let { row[Tasks.foCert] = it }
            bunkerCert?.let { row[Tasks.bunkerCert] = it }
            linkedTaskId?.let { row[Tasks.linkedTaskId] = it }
            receiptPhotoUrl?.let { row[Tasks.receiptPhotoUrl] = it }
            row[Tasks.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar task
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Tasks.deleteWhere { Tasks.id eq id } > 0
    }
}
