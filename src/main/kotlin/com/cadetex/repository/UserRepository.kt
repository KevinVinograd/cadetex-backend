package com.cadetex.repository

import com.cadetex.database.tables.Users
import com.cadetex.model.User
import com.cadetex.model.CreateUserRequest
import com.cadetex.model.UpdateUserRequest
import com.cadetex.model.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.datetime.Clock
import java.util.*
import org.mindrot.jbcrypt.BCrypt

class UserRepository {


    suspend fun findById(id: String): User? = newSuspendedTransaction {
        Users
            .selectAll()
            .where { Users.id eq UUID.fromString(id) }
            .map(::rowToUser)
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): User? = newSuspendedTransaction {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map(::rowToUser)
            .singleOrNull()
    }

    suspend fun findByOrganization(organizationId: String): List<User> = newSuspendedTransaction {
        Users
            .selectAll()
            .where { Users.organizationId eq UUID.fromString(organizationId) }
            .map(::rowToUser)
    }

    suspend fun findByRole(role: UserRole): List<User> = newSuspendedTransaction {
        Users
            .selectAll()
            .where { Users.role eq role.name }
            .map(::rowToUser)
    }

    suspend fun create(request: CreateUserRequest): User = newSuspendedTransaction {
        val now = Clock.System.now()
        val passwordHash = hashPassword(request.password)
        val id = UUID.randomUUID()

        Users.insert {
            it[Users.id] = id
            it[Users.organizationId] = UUID.fromString(request.organizationId)
            it[Users.name] = request.name
            it[Users.email] = request.email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = request.role.name
            it[Users.isActive] = request.isActive
            it[Users.createdAt] = now
            it[Users.updatedAt] = now
        }

        User(
            id = id.toString(),
            organizationId = request.organizationId,
            name = request.name,
            email = request.email,
            passwordHash = passwordHash,
            role = request.role,
            isActive = request.isActive,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
    }

    suspend fun update(id: String, updateRequest: UpdateUserRequest): User? = newSuspendedTransaction {
        val now = Clock.System.now()

        val updated = Users.update({ Users.id eq UUID.fromString(id) }) { row ->
            updateRequest.name?.let { newName -> row[Users.name] = newName }
            updateRequest.email?.let { newEmail -> row[Users.email] = newEmail }
            updateRequest.password?.let { newPassword -> row[Users.passwordHash] = hashPassword(newPassword) }
            updateRequest.role?.let { newRole -> row[Users.role] = newRole.name }
            updateRequest.isActive?.let { newIsActive -> row[Users.isActive] = newIsActive }
            row[Users.updatedAt] = now
        }

        if (updated > 0) findById(id) else null
    }

    suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        Users.deleteWhere { Users.id eq UUID.fromString(id) } > 0
    }

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

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
