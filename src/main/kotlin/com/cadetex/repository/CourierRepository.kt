package com.cadetex.repository

import com.cadetex.database.tables.Couriers
import com.cadetex.model.Courier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en CourierService
 */
class CourierRepository {

    /**
     * Buscar courier por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): Courier? {
        return Couriers.selectAll()
            .where { Couriers.id eq id }
            .map(::rowToCourier)
            .singleOrNull()
    }

    /**
     * Buscar couriers por organización
     * Usa índice: idx_couriers_organization_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByOrganization(organizationId: UUID): List<Courier> {
        return Couriers.selectAll()
            .where { Couriers.organizationId eq organizationId }
            .map(::rowToCourier)
    }

    /**
     * Buscar couriers activos por organización
     * Usa índices: idx_couriers_organization_id, idx_couriers_org_active
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findActiveByOrganization(organizationId: UUID): List<Courier> {
        return Couriers.selectAll()
            .where {
                (Couriers.organizationId eq organizationId) and
                (Couriers.isActive eq true)
            }
            .map(::rowToCourier)
    }

    /**
     * Buscar por nombre
     * Usa índice: idx_couriers_organization_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun searchByName(organizationId: UUID, name: String): List<Courier> {
        return Couriers.selectAll()
            .where {
                (Couriers.organizationId eq organizationId) and
                (Couriers.name like "%$name%")
            }
            .map(::rowToCourier)
    }

    /**
     * Buscar por teléfono
     * Usa índice: idx_couriers_organization_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun searchByPhone(organizationId: UUID, phoneNumber: String): List<Courier> {
        return Couriers.selectAll()
            .where {
                (Couriers.organizationId eq organizationId) and
                (Couriers.phoneNumber like "%$phoneNumber%")
            }
            .map(::rowToCourier)
    }

    /**
     * Buscar por userId
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByUserId(userId: UUID): Courier? {
        return Couriers.selectAll()
            .where { Couriers.userId eq userId }
            .map(::rowToCourier)
            .singleOrNull()
    }

    /**
     * Buscar couriers con cuentas de usuario
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findCouriersWithUserAccounts(): List<Courier> {
        return Couriers.selectAll()
            .where { Couriers.userId.isNotNull() }
            .map(::rowToCourier)
    }

    /**
     * Buscar couriers sin cuentas de usuario
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findCouriersWithoutUserAccounts(): List<Courier> {
        return Couriers.selectAll()
            .where { Couriers.userId.isNull() }
            .map(::rowToCourier)
    }

    /**
     * Insertar nuevo courier
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        userId: UUID? = null,
        organizationId: UUID,
        name: String,
        phoneNumber: String,
        address: String? = null,
        vehicleType: String? = null,
        isActive: Boolean = true,
        createdAt: kotlinx.datetime.Instant,
        updatedAt: kotlinx.datetime.Instant
    ): UUID {
        val id = UUID.randomUUID()

        Couriers.insert {
            it[Couriers.id] = id
            it[Couriers.userId] = userId
            it[Couriers.organizationId] = organizationId
            it[Couriers.name] = name
            it[Couriers.phoneNumber] = phoneNumber
            it[Couriers.address] = address
            it[Couriers.vehicleType] = vehicleType
            it[Couriers.isActive] = isActive
            it[Couriers.createdAt] = createdAt
            it[Couriers.updatedAt] = updatedAt
        }

        return id
    }

    /**
     * Actualizar courier existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        name: String? = null,
        phoneNumber: String? = null,
        address: String? = null,
        vehicleType: String? = null,
        isActive: Boolean? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = Couriers.update({ Couriers.id eq id }) { row ->
            name?.let { row[Couriers.name] = it }
            phoneNumber?.let { row[Couriers.phoneNumber] = it }
            address?.let { row[Couriers.address] = it }
            vehicleType?.let { row[Couriers.vehicleType] = it }
            isActive?.let { row[Couriers.isActive] = it }
            row[Couriers.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar courier
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Couriers.deleteWhere { Couriers.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a Courier
     */
    private fun rowToCourier(row: ResultRow) = Courier(
        id = row[Couriers.id].value.toString(),
        userId = row[Couriers.userId]?.value?.toString(),
        organizationId = row[Couriers.organizationId].value.toString(),
        name = row[Couriers.name],
        phoneNumber = row[Couriers.phoneNumber],
        address = row[Couriers.address],
        vehicleType = row[Couriers.vehicleType],
        isActive = row[Couriers.isActive],
        createdAt = row[Couriers.createdAt].toString(),
        updatedAt = row[Couriers.updatedAt].toString()
    )
}
