package com.cadetex.repository

import com.cadetex.database.tables.Addresses
import com.cadetex.model.Address
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en los Services
 */
class AddressRepository {

    /**
     * Buscar address por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): Address? {
        return Addresses
            .selectAll()
            .where { Addresses.id eq id }
            .map(::rowToAddress)
            .singleOrNull()
    }

    /**
     * Insertar nueva address
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        street: String? = null,
        streetNumber: String? = null,
        addressComplement: String? = null,
        city: String? = null,
        province: String? = null,
        postalCode: String? = null,
        createdAt: kotlinx.datetime.Instant? = null,
        updatedAt: kotlinx.datetime.Instant? = null
    ): UUID {
        val id = UUID.randomUUID()
        val now = createdAt ?: kotlinx.datetime.Clock.System.now()

        Addresses.insert {
            it[Addresses.id] = id
            it[Addresses.street] = street
            it[Addresses.streetNumber] = streetNumber
            it[Addresses.addressComplement] = addressComplement
            it[Addresses.city] = city
            it[Addresses.province] = province
            it[Addresses.postalCode] = postalCode
            it[Addresses.createdAt] = now
            it[Addresses.updatedAt] = updatedAt ?: now
        }
        
        return id
    }

    /**
     * Actualizar address existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        street: String? = null,
        streetNumber: String? = null,
        addressComplement: String? = null,
        city: String? = null,
        province: String? = null,
        postalCode: String? = null
    ): Boolean {
        val now = kotlinx.datetime.Clock.System.now()

        val updated = Addresses.update({ Addresses.id eq id }) { row ->
            street?.let { row[Addresses.street] = it }
            streetNumber?.let { row[Addresses.streetNumber] = it }
            addressComplement?.let { row[Addresses.addressComplement] = it }
            city?.let { row[Addresses.city] = it }
            province?.let { row[Addresses.province] = it }
            postalCode?.let { row[Addresses.postalCode] = it }
            row[Addresses.updatedAt] = now
        }

        return updated > 0
    }

    /**
     * Eliminar address
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Addresses.deleteWhere { Addresses.id eq id } > 0
    }

    /**
     * Buscar por ciudad (usa índice idx_addresses_city)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByCity(city: String): List<Address> {
        return Addresses
            .selectAll()
            .where { Addresses.city eq city }
            .map(::rowToAddress)
    }

    /**
     * Mapper de ResultRow a Address
     */
    private fun rowToAddress(row: ResultRow) = Address(
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
}
