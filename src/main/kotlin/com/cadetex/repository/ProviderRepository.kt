package com.cadetex.repository

import com.cadetex.database.tables.Providers
import com.cadetex.model.Provider
import com.cadetex.model.CreateProviderRequest
import com.cadetex.model.UpdateProviderRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.datetime.Clock
import java.util.*

class ProviderRepository {

    suspend fun allProviders(): List<Provider> = newSuspendedTransaction {
        Providers.selectAll().map(::rowToProvider)
    }

    suspend fun findById(id: String): Provider? = newSuspendedTransaction {
        Providers
            .selectAll()
            .where { Providers.id eq UUID.fromString(id) }
            .map(::rowToProvider)
            .singleOrNull()
    }

    suspend fun findByOrganization(organizationId: String): List<Provider> = newSuspendedTransaction {
        Providers
            .selectAll()
            .where { Providers.organizationId eq UUID.fromString(organizationId) }
            .map(::rowToProvider)
    }

    suspend fun searchByName(organizationId: String, name: String): List<Provider> = newSuspendedTransaction {
        Providers
            .selectAll()
            .where { 
                (Providers.organizationId eq UUID.fromString(organizationId)) and
                (Providers.name like "%$name%")
            }
            .map(::rowToProvider)
    }

    suspend fun searchByCity(organizationId: String, city: String): List<Provider> = newSuspendedTransaction {
        Providers
            .selectAll()
            .where { 
                (Providers.organizationId eq UUID.fromString(organizationId)) and
                (Providers.city like "%$city%")
            }
            .map(::rowToProvider)
    }

    suspend fun create(request: CreateProviderRequest): Provider = newSuspendedTransaction {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Providers.insert {
            it[Providers.id] = id
            it[organizationId] = UUID.fromString(request.organizationId)
            it[name] = request.name
            it[address] = request.address
            it[city] = request.city
            it[province] = request.province
            it[contactName] = request.contactName
            it[contactPhone] = request.contactPhone
            it[createdAt] = now
            it[updatedAt] = now
        }

        Provider(
            id = id.toString(),
            organizationId = request.organizationId,
            name = request.name,
            address = request.address,
            city = request.city,
            province = request.province,
            contactName = request.contactName,
            contactPhone = request.contactPhone,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
    }

    suspend fun update(id: String, updateRequest: UpdateProviderRequest): Provider? = newSuspendedTransaction {
        val now = Clock.System.now()

        val updated = Providers.update({ Providers.id eq UUID.fromString(id) }) { row ->
            updateRequest.name?.let { newName -> row[Providers.name] = newName }
            updateRequest.address?.let { newAddress -> row[Providers.address] = newAddress }
            updateRequest.city?.let { newCity -> row[Providers.city] = newCity }
            updateRequest.province?.let { newProvince -> row[Providers.province] = newProvince }
            updateRequest.contactName?.let { newContactName -> row[Providers.contactName] = newContactName }
            updateRequest.contactPhone?.let { newContactPhone -> row[Providers.contactPhone] = newContactPhone }
            row[Providers.updatedAt] = now
        }

        if (updated > 0) findById(id) else null
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        Providers.deleteWhere { Providers.id eq UUID.fromString(id) } > 0
    }

    private fun rowToProvider(row: ResultRow) = Provider(
        id = row[Providers.id].value.toString(),
        organizationId = row[Providers.organizationId].value.toString(),
        name = row[Providers.name],
        address = row[Providers.address],
        city = row[Providers.city],
        province = row[Providers.province],
        contactName = row[Providers.contactName],
        contactPhone = row[Providers.contactPhone],
        createdAt = row[Providers.createdAt].toString(),
        updatedAt = row[Providers.updatedAt].toString()
    )
}
