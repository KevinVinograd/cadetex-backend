package com.cadetex.repository

import com.cadetex.database.tables.Couriers
import com.cadetex.model.Courier
import com.cadetex.model.CreateCourierRequest
import com.cadetex.model.UpdateCourierRequest
import java.util.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CourierRepository {

    private fun isValidUUID(uuidString: String): Boolean {
        return try {
            UUID.fromString(uuidString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun validateUUID(uuidString: String, fieldName: String) {
        if (!isValidUUID(uuidString)) {
            throw IllegalArgumentException("Invalid UUID format for $fieldName: $uuidString")
        }
    }

    suspend fun allCouriers(): List<Courier> = newSuspendedTransaction {
        Couriers.selectAll().map(::rowToCourier)
    }

    suspend fun findById(id: String): Courier? = newSuspendedTransaction {
        validateUUID(id, "courier ID")
        Couriers.selectAll()
                .where { Couriers.id eq UUID.fromString(id) }
                .map(::rowToCourier)
                .singleOrNull()
    }

    suspend fun findByOrganization(organizationId: String): List<Courier> =
            newSuspendedTransaction {
                validateUUID(organizationId, "organization ID")
                Couriers.selectAll()
                        .where { Couriers.organizationId eq UUID.fromString(organizationId) }
                        .map(::rowToCourier)
            }

    suspend fun findActiveByOrganization(organizationId: String): List<Courier> =
            newSuspendedTransaction {
                validateUUID(organizationId, "organization ID")
                Couriers.selectAll()
                        .where {
                            (Couriers.organizationId eq UUID.fromString(organizationId)) and
                                    (Couriers.isActive eq true)
                        }
                        .map(::rowToCourier)
            }

    suspend fun searchByName(organizationId: String, name: String): List<Courier> =
            newSuspendedTransaction {
                validateUUID(organizationId, "organization ID")
                Couriers.selectAll()
                        .where {
                            (Couriers.organizationId eq UUID.fromString(organizationId)) and
                                    (Couriers.name like "%$name%")
                        }
                        .map(::rowToCourier)
            }

    suspend fun searchByPhone(organizationId: String, phoneNumber: String): List<Courier> =
            newSuspendedTransaction {
                validateUUID(organizationId, "organization ID")
                Couriers.selectAll()
                        .where {
                            (Couriers.organizationId eq UUID.fromString(organizationId)) and
                                    (Couriers.phoneNumber like "%$phoneNumber%")
                        }
                        .map(::rowToCourier)
            }

    suspend fun create(request: CreateCourierRequest): Courier = newSuspendedTransaction {
        validateUUID(request.organizationId, "organization ID")
        request.userId?.let { validateUUID(it, "user ID") }

        val now = Clock.System.now()
        val id = UUID.randomUUID()

        Couriers.insert {
            it[Couriers.id] = id
            it[userId] = request.userId?.let { UUID.fromString(it) }
            it[organizationId] = UUID.fromString(request.organizationId)
            it[name] = request.name
            it[phoneNumber] = request.phoneNumber
            it[address] = request.address
            it[vehicleType] = request.vehicleType
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }

        Courier(
                id = id.toString(),
                userId = request.userId,
                organizationId = request.organizationId,
                name = request.name,
                phoneNumber = request.phoneNumber,
                address = request.address,
                vehicleType = request.vehicleType,
                isActive = true,
                createdAt = now.toString(),
                updatedAt = now.toString()
        )
    }

    suspend fun update(id: String, updateRequest: UpdateCourierRequest): Courier? =
            newSuspendedTransaction {
                validateUUID(id, "courier ID")
                val now = Clock.System.now()

                val updated =
                        Couriers.update({ Couriers.id eq UUID.fromString(id) }) { row ->
                            updateRequest.name?.let { newName -> row[Couriers.name] = newName }
                            updateRequest.phoneNumber?.let { newPhoneNumber ->
                                row[Couriers.phoneNumber] = newPhoneNumber
                            }
                            updateRequest.address?.let { newAddress ->
                                row[Couriers.address] = newAddress
                            }
                            updateRequest.vehicleType?.let { newVehicleType ->
                                row[Couriers.vehicleType] = newVehicleType
                            }
                            updateRequest.isActive?.let { newIsActive ->
                                row[Couriers.isActive] = newIsActive
                            }
                            row[Couriers.updatedAt] = now
                        }

                if (updated > 0) findById(id) else null
            }

    suspend fun findByUserId(userId: String): Courier? = newSuspendedTransaction {
        validateUUID(userId, "user ID")
        Couriers.selectAll()
                .where { Couriers.userId eq UUID.fromString(userId) }
                .map(::rowToCourier)
                .singleOrNull()
    }

    suspend fun findCouriersWithUserAccounts(): List<Courier> = newSuspendedTransaction {
        Couriers.selectAll().where { Couriers.userId.isNotNull() }.map(::rowToCourier)
    }

    suspend fun findCouriersWithoutUserAccounts(): List<Courier> = newSuspendedTransaction {
        Couriers.selectAll().where { Couriers.userId.isNull() }.map(::rowToCourier)
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        validateUUID(id, "courier ID")
        Couriers.deleteWhere { Couriers.id eq UUID.fromString(id) } > 0
    }

    private fun rowToCourier(row: ResultRow) =
            Courier(
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
