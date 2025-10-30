package com.cadetex.repository

import com.cadetex.database.tables.Organizations
import com.cadetex.model.Organization
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en OrganizationService
 */
class OrganizationRepository {

    /**
     * Buscar todas las organizaciones
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findAll(): List<Organization> {
        return Organizations.selectAll().map(::rowToOrganization)
    }

    /**
     * Buscar organización por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): Organization? {
        return Organizations
            .selectAll()
            .where { Organizations.id eq id }
            .map(::rowToOrganization)
            .singleOrNull()
    }

    /**
     * Buscar organización por nombre
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByName(name: String): Organization? {
        return Organizations
            .selectAll()
            .where { Organizations.name eq name }
            .map(::rowToOrganization)
            .singleOrNull()
    }

    /**
     * Insertar nueva organización
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        name: String,
        createdAt: kotlinx.datetime.Instant,
        updatedAt: kotlinx.datetime.Instant
    ): UUID {
        val inserted = Organizations.insertAndGetId {
            it[Organizations.name] = name
            it[Organizations.createdAt] = createdAt
            it[Organizations.updatedAt] = updatedAt
        }

        return inserted.value
    }

    /**
     * Actualizar organización existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        name: String? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = Organizations.update({ Organizations.id eq id }) { row ->
            name?.let { row[Organizations.name] = it }
            row[Organizations.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar organización
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Organizations.deleteWhere { Organizations.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a Organization
     */
    private fun rowToOrganization(row: ResultRow) = Organization(
        id = row[Organizations.id].value.toString(),
        name = row[Organizations.name],
        createdAt = row[Organizations.createdAt].toString(),
        updatedAt = row[Organizations.updatedAt].toString()
    )
}
