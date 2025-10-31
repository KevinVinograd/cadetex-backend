package com.cadetex.repository

import com.cadetex.database.tables.Addresses
import com.cadetex.database.tables.Providers
import com.cadetex.model.Address
import com.cadetex.model.Provider
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en ProviderService
 */
class ProviderRepository {

    /**
     * Buscar proveedor por ID
     * Usa índice: id es PRIMARY KEY (automático), address_id tiene idx_providers_address_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): Provider? {
        val row = (Providers leftJoin Addresses)
            .selectAll()
            .where { Providers.id eq id }
            .singleOrNull()
        
        return row?.let { rowToProvider(it) }
    }

    /**
     * Buscar proveedores por organización
     * Usa índice: idx_providers_organization_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByOrganization(organizationId: UUID): List<Provider> {
        return (Providers leftJoin Addresses)
            .selectAll()
            .where { Providers.organizationId eq organizationId }
            .map(::rowToProvider)
    }

    /**
     * Buscar por nombre (LIKE search)
     * Usa índice: idx_providers_org_name (composite: organization_id, name)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun searchByName(organizationId: UUID, name: String): List<Provider> {
        return (Providers leftJoin Addresses)
            .selectAll()
            .where { 
                (Providers.organizationId eq organizationId) and
                (Providers.name like "%$name%")
            }
            .map(::rowToProvider)
    }

    /**
     * Buscar por ciudad
     * Usa índices: idx_providers_organization_id, idx_providers_address_id, idx_addresses_city
     * Debe ejecutarse dentro de una transacción activa
     */
    fun searchByCity(organizationId: UUID, city: String): List<Provider> {
        return (Providers leftJoin Addresses)
            .selectAll()
            .where { 
                (Providers.organizationId eq organizationId) and
                (Addresses.city like "%$city%")
            }
            .map(::rowToProvider)
    }

    /**
     * Verificar si existe un proveedor con el mismo nombre en la organización
     * Usa índice: idx_providers_org_name
     * Debe ejecutarse dentro de una transacción activa
     */
    fun existsByName(organizationId: UUID, name: String): Boolean {
        return Providers
            .select(Providers.id)
            .where { 
                (Providers.organizationId eq organizationId) and
                (Providers.name eq name.trim())
            }
            .firstOrNull() != null
    }

    /**
     * Verificar si existe otro proveedor con el mismo nombre (excluyendo uno específico)
     * Usa índice: idx_providers_org_name
     * Debe ejecutarse dentro de una transacción activa
     */
    fun existsByNameExcludingId(organizationId: UUID, name: String, excludeId: UUID): Boolean {
        return Providers
            .select(Providers.id)
            .where { 
                (Providers.organizationId eq organizationId) and
                (Providers.name eq name.trim())
            }
            .firstOrNull()
            ?.let { row ->
                val foundId = row[Providers.id].value
                foundId != excludeId
            } ?: false
    }

    /**
     * Insertar nuevo proveedor
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        organizationId: UUID,
        name: String,
        addressId: UUID? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        isActive: Boolean = true,
        createdAt: kotlinx.datetime.Instant,
        updatedAt: kotlinx.datetime.Instant
    ): UUID {
        val id = UUID.randomUUID()

        Providers.insert {
            it[Providers.id] = id
            it[Providers.organizationId] = organizationId
            it[Providers.name] = name
            it[Providers.addressId] = addressId
            it[Providers.contactName] = contactName
            it[Providers.contactPhone] = contactPhone
            it[Providers.isActive] = isActive
            it[Providers.createdAt] = createdAt
            it[Providers.updatedAt] = updatedAt
        }
        
        return id
    }

    /**
     * Actualizar proveedor existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        name: String? = null,
        addressId: UUID? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        isActive: Boolean? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = Providers.update({ Providers.id eq id }) { row ->
            name?.let { row[Providers.name] = it }
            addressId?.let { row[Providers.addressId] = it }
            contactName?.let { row[Providers.contactName] = it }
            contactPhone?.let { row[Providers.contactPhone] = it }
            isActive?.let { row[Providers.isActive] = it }
            row[Providers.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar proveedor
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Providers.deleteWhere { Providers.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a Provider
     */
    private fun rowToProvider(row: ResultRow): Provider {
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
        
        return Provider(
            id = row[Providers.id].value.toString(),
            organizationId = row[Providers.organizationId].value.toString(),
            name = row[Providers.name],
            address = address,
            contactName = row[Providers.contactName],
            contactPhone = row[Providers.contactPhone],
            isActive = row[Providers.isActive],
            createdAt = row[Providers.createdAt].toString(),
            updatedAt = row[Providers.updatedAt].toString()
        )
    }
}
