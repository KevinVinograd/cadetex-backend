package com.cadetex.repository

import com.cadetex.database.tables.Users
import com.cadetex.model.User
import com.cadetex.model.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Repository simplificado: solo queries simples
 * NO maneja transacciones - debe ser llamado desde dentro de una transacción (en los Services)
 * Toda la lógica de negocio está en UserService
 */
class UserRepository {

    /**
     * Buscar usuario por ID
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findById(id: UUID): User? {
        return Users
            .selectAll()
            .where { Users.id eq id }
            .map(::rowToUser)
            .singleOrNull()
    }

    /**
     * Buscar usuario por email
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByEmail(email: String): User? {
        return Users
            .selectAll()
            .where { Users.email eq email }
            .map(::rowToUser)
            .singleOrNull()
    }

    /**
     * Buscar usuarios por organización
     * Usa índice: idx_users_organization_id
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByOrganization(organizationId: UUID): List<User> {
        return Users
            .selectAll()
            .where { Users.organizationId eq organizationId }
            .map(::rowToUser)
    }

    /**
     * Buscar usuarios por rol
     * Debe ejecutarse dentro de una transacción activa
     */
    fun findByRole(role: UserRole): List<User> {
        return Users
            .selectAll()
            .where { Users.role eq role.name }
            .map(::rowToUser)
    }

    /**
     * Insertar nuevo usuario
     * Retorna el ID insertado
     * Debe ejecutarse dentro de una transacción activa
     */
    fun insert(
        organizationId: UUID,
        name: String,
        email: String,
        passwordHash: String,
        role: UserRole,
        isActive: Boolean = true,
        createdAt: kotlinx.datetime.Instant,
        updatedAt: kotlinx.datetime.Instant
    ): UUID {
        val id = UUID.randomUUID()

        Users.insert {
            it[Users.id] = id
            it[Users.organizationId] = organizationId
            it[Users.name] = name
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role.name
            it[Users.isActive] = isActive
            it[Users.createdAt] = createdAt
            it[Users.updatedAt] = updatedAt
        }

        return id
    }

    /**
     * Actualizar usuario existente
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun update(
        id: UUID,
        name: String? = null,
        email: String? = null,
        passwordHash: String? = null,
        role: UserRole? = null,
        isActive: Boolean? = null,
        updatedAt: kotlinx.datetime.Instant
    ): Boolean {
        val updated = Users.update({ Users.id eq id }) { row ->
            name?.let { row[Users.name] = it }
            email?.let { row[Users.email] = it }
            passwordHash?.let { row[Users.passwordHash] = it }
            role?.let { row[Users.role] = it.name }
            isActive?.let { row[Users.isActive] = it }
            row[Users.updatedAt] = updatedAt
        }

        return updated > 0
    }

    /**
     * Eliminar usuario
     * Usa índice: id es PRIMARY KEY (automático)
     * Debe ejecutarse dentro de una transacción activa
     */
    fun delete(id: UUID): Boolean {
        return Users.deleteWhere { Users.id eq id } > 0
    }

    /**
     * Mapper de ResultRow a User
     */
    private fun rowToUser(row: ResultRow) = User(
        id = row[Users.id].value.toString(),
        organizationId = row[Users.organizationId].value.toString(),
        name = row[Users.name],
        email = row[Users.email],
        passwordHash = row[Users.passwordHash],
        role = UserRole.valueOf(row[Users.role]),
        isActive = row[Users.isActive],
        createdAt = row[Users.createdAt].toString(),
        updatedAt = row[Users.updatedAt].toString()
    )
}
