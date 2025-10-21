package com.cadetex.repository

import com.cadetex.database.tables.Organizations
import com.cadetex.model.Organization
import com.cadetex.model.CreateOrganizationRequest
import com.cadetex.model.UpdateOrganizationRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.*

class OrganizationRepository {

    suspend fun allOrganizations(): List<Organization> = newSuspendedTransaction {
        Organizations.selectAll().map(::rowToOrganization)
    }

    suspend fun findById(id: String): Organization? = newSuspendedTransaction {
        Organizations
            .selectAll()
            .where { Organizations.id eq UUID.fromString(id) }
            .map(::rowToOrganization)
            .singleOrNull()
    }

    suspend fun findByName(name: String): Organization? = newSuspendedTransaction {
        Organizations
            .selectAll()
            .where { Organizations.name eq name }
            .map(::rowToOrganization)
            .singleOrNull()
    }

    suspend fun create(request: CreateOrganizationRequest): Organization = newSuspendedTransaction {
        val now = Clock.System.now()

        val inserted = Organizations.insertAndGetId {
            it[name] = request.name
            it[createdAt] = now
            it[updatedAt] = now
        }

        Organization(
            id = inserted.value.toString(), // UUID generado automáticamente
            name = request.name,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
    }

    suspend fun update(id: String, updateRequest: UpdateOrganizationRequest): Organization? = newSuspendedTransaction {
        val now = Clock.System.now()

        val updated = Organizations.update({ Organizations.id eq UUID.fromString(id) }) { row ->
            updateRequest.name?.let { newName -> row[Organizations.name] = newName }
            row[Organizations.updatedAt] = now
        }

        if (updated > 0) findById(id) else null
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        Organizations.deleteWhere { Organizations.id eq UUID.fromString(id) } > 0
    }

    private fun rowToOrganization(row: ResultRow) = Organization(
        id = row[Organizations.id].value.toString(), // ✅ usar .value con UUIDTable
        name = row[Organizations.name],
        createdAt = row[Organizations.createdAt].toString(),
        updatedAt = row[Organizations.updatedAt].toString()
    )
}
