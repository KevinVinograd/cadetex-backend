package com.cadetex.repository

import com.cadetex.database.tables.Clients
import com.cadetex.model.Client
import com.cadetex.model.CreateClientRequest
import com.cadetex.model.UpdateClientRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.datetime.Clock
import java.util.*

class ClientRepository {

    suspend fun allClients(): List<Client> = newSuspendedTransaction {
        Clients.selectAll().map(::rowToClient)
    }

    suspend fun findById(id: String): Client? = newSuspendedTransaction {
        Clients
            .selectAll()
            .where { Clients.id eq UUID.fromString(id) }
            .map(::rowToClient)
            .singleOrNull()
    }

    suspend fun findByOrganization(organizationId: String): List<Client> = newSuspendedTransaction {
        Clients
            .selectAll()
            .where { Clients.organizationId eq UUID.fromString(organizationId) }
            .map(::rowToClient)
    }

    suspend fun searchByName(organizationId: String, name: String): List<Client> = newSuspendedTransaction {
        Clients
            .selectAll()
            .where { 
                (Clients.organizationId eq UUID.fromString(organizationId)) and
                (Clients.name like "%$name%")
            }
            .map(::rowToClient)
    }

    suspend fun searchByCity(organizationId: String, city: String): List<Client> = newSuspendedTransaction {
        Clients
            .selectAll()
            .where { 
                (Clients.organizationId eq UUID.fromString(organizationId)) and
                (Clients.city like "%$city%")
            }
            .map(::rowToClient)
    }

    suspend fun create(request: CreateClientRequest): Client = newSuspendedTransaction {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Clients.insert {
            it[Clients.id] = id
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

        Client(
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

    suspend fun update(id: String, updateRequest: UpdateClientRequest): Client? = newSuspendedTransaction {
        val now = Clock.System.now()

        val updated = Clients.update({ Clients.id eq UUID.fromString(id) }) { row ->
            updateRequest.name?.let { newName -> row[Clients.name] = newName }
            updateRequest.address?.let { newAddress -> row[Clients.address] = newAddress }
            updateRequest.city?.let { newCity -> row[Clients.city] = newCity }
            updateRequest.province?.let { newProvince -> row[Clients.province] = newProvince }
            updateRequest.contactName?.let { newContactName -> row[Clients.contactName] = newContactName }
            updateRequest.contactPhone?.let { newContactPhone -> row[Clients.contactPhone] = newContactPhone }
            row[Clients.updatedAt] = now
        }

        if (updated > 0) findById(id) else null
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        Clients.deleteWhere { Clients.id eq UUID.fromString(id) } > 0
    }

    private fun rowToClient(row: ResultRow) = Client(
        id = row[Clients.id].value.toString(),
        organizationId = row[Clients.organizationId].value.toString(),
        name = row[Clients.name],
        address = row[Clients.address],
        city = row[Clients.city],
        province = row[Clients.province],
        contactName = row[Clients.contactName],
        contactPhone = row[Clients.contactPhone],
        createdAt = row[Clients.createdAt].toString(),
        updatedAt = row[Clients.updatedAt].toString()
    )
}
