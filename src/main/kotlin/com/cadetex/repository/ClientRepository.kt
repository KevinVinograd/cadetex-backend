package com.cadetex.repository

import com.cadetex.database.tables.Addresses
import com.cadetex.database.tables.Clients
import com.cadetex.model.Address
import com.cadetex.model.Client
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en ClientService
 */
class ClientRepository {

    /**
     * Buscar cliente por ID
     * Usa índice: id es PRIMARY KEY (automático), address_id tiene idx_clients_address_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): Client? {
        val row = (Clients leftJoin Addresses)
            .selectAll()
            .where { Clients.id eq id }
            .singleOrNull()
        
        return row?.let { rowToClient(it) }
    }

    /**
     * Buscar clientes por organización
     * Usa índice: idx_clients_organization_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByOrganization(organizationId: UUID): List<Client> {
        return (Clients leftJoin Addresses)
            .selectAll()
            .where { Clients.organizationId eq organizationId }
            .map(::rowToClient)
    }

    /**
     * Buscar por nombre (LIKE search)
     * Usa índice: idx_clients_org_name (composite: organization_id, name)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun searchByName(organizationId: UUID, name: String): List<Client> {
        return (Clients leftJoin Addresses)
            .selectAll()
            .where { 
                (Clients.organizationId eq organizationId) and
                (Clients.name like "%$name%")
            }
            .map(::rowToClient)
    }

    /**
     * Buscar por ciudad
     * Usa índices: idx_clients_organization_id, idx_clients_address_id, idx_addresses_city
     * Debe ejecutarse dentro de una transacción activa
     */
    fun searchByCity(organizationId: UUID, city: String): List<Client> {
        return (Clients leftJoin Addresses)
            .selectAll()
            .where { 
                (Clients.organizationId eq organizationId) and
                (Addresses.city like "%$city%")
            }
            .map(::rowToClient)
    }

    /**
     * Verificar si existe un cliente con el mismo nombre en la organización
     * Usa índice: idx_clients_org_name
     * Debe ejecutarse dentro de una transacción activa
     */
    fun existsByName(organizationId: UUID, name: String): Boolean {
        return Clients
            .select(Clients.id)
            .where { 
                (Clients.organizationId eq organizationId) and
                (Clients.name eq name.trim())
            }
            .firstOrNull() != null
    }

    /**
     * Verificar si existe otro cliente con el mismo nombre (excluyendo uno específico)
     * Usa índice: idx_clients_org_name
     * Debe ejecutarse dentro de una transacción activa
     */
    fun existsByNameExcludingId(organizationId: UUID, name: String, excludeId: UUID): Boolean {
        return Clients
            .select(Clients.id)
            .where { 
                (Clients.organizationId eq organizationId) and
                (Clients.name eq name.trim())
            }
            .firstOrNull()
            ?.let { row ->
                val foundId = row[Clients.id].value
                foundId != excludeId
            } ?: false
    }

    /**
     * Insertar nuevo cliente
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        organizationId: UUID,
        name: String,
        addressId: UUID? = null,
        phoneNumber: String? = null,
        email: String? = null,
        isActive: Boolean = true,
        createdAt: kotlinx.datetime.Instant,
        updatedAt: kotlinx.datetime.Instant
    ): UUID {
        val id = UUID.randomUUID()

        Clients.insert {
            it[Clients.id] = id
            it[Clients.organizationId] = organizationId
            it[Clients.name] = name
            it[Clients.addressId] = addressId
            it[Clients.phoneNumber] = phoneNumber
            it[Clients.email] = email
            it[Clients.isActive] = isActive
            it[Clients.createdAt] = createdAt
            it[Clients.updatedAt] = updatedAt
        }
        
        return id
    }

    /**
     * Actualizar cliente existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        name: String? = null,
        addressId: UUID? = null,
        phoneNumber: String? = null,
        email: String? = null,
        isActive: Boolean? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = Clients.update({ Clients.id eq id }) { row ->
            name?.let { row[Clients.name] = it }
            addressId?.let { row[Clients.addressId] = it }
            phoneNumber?.let { row[Clients.phoneNumber] = it }
            email?.let { row[Clients.email] = it }
            isActive?.let { row[Clients.isActive] = it }
            row[Clients.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar cliente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Clients.deleteWhere { Clients.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a Client
     */
    private fun rowToClient(row: ResultRow): Client {
        val address = if (row.getOrNull(Addresses.id) != null) {
            Address(
                id = row[Addresses.id].value.toString(),
                street = row[Addresses.street],
                streetNumber = row[Addresses.streetNumber],
                addressComplement = row[Addresses.addressComplement],
                city = row[Addresses.city],
                province = row[Addresses.province],
                postalCode = row[Addresses.postalCode],
                createdAt = row[Addresses.createdAt].toString(),
                updatedAt = row[Addresses.updatedAt].toString()
            )
        } else {
            null
        }
        
        return Client(
            id = row[Clients.id].value.toString(),
            organizationId = row[Clients.organizationId].value.toString(),
            name = row[Clients.name],
            address = address,
            phoneNumber = row[Clients.phoneNumber],
            email = row[Clients.email],
            isActive = row[Clients.isActive],
            createdAt = row[Clients.createdAt].toString(),
            updatedAt = row[Clients.updatedAt].toString()
        )
    }
}
