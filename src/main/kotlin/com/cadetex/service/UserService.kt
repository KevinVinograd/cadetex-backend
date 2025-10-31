package com.cadetex.service

import com.cadetex.model.*
import com.cadetex.repository.UserRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import org.mindrot.jbcrypt.BCrypt
import java.util.*

private val logger = LoggerFactory.getLogger("UserService")

/**
 * Service para lógica de negocio de Usuarios
 * Maneja validaciones, hash de passwords, y coordina todas las operaciones
 * Todo dentro de la misma transacción
 */
class UserService(
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Hash de password usando BCrypt
     */
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Verificar password
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }

    /**
     * Buscar usuario por ID
     */
    suspend fun findById(id: String): Result<User> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val user = userRepository.findById(uuid)
            if (user != null) {
                success(user)
            } else {
                error("Usuario no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de usuario inválido: $id")
        } catch (e: Exception) {
            logger.error("Error buscando usuario: ${e.message}", e)
            error("Error al buscar el usuario: ${e.message}")
        }
    }

    /**
     * Buscar usuario por email
     */
    suspend fun findByEmail(email: String): Result<User?> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val user = userRepository.findByEmail(email)
            success(user)
        } catch (e: Exception) {
            logger.error("Error buscando usuario por email: ${e.message}", e)
            error("Error al buscar el usuario: ${e.message}")
        }
    }

    /**
     * Buscar usuarios por organización
     */
    suspend fun findByOrganization(organizationId: String): Result<List<User>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(organizationId)
            val users = userRepository.findByOrganization(uuid)
            success(users)
        } catch (e: IllegalArgumentException) {
            error("ID de organización inválido: $organizationId")
        } catch (e: Exception) {
            logger.error("Error buscando usuarios: ${e.message}", e)
            error("Error al buscar usuarios: ${e.message}")
        }
    }

    /**
     * Buscar usuarios por rol
     */
    suspend fun findByRole(role: UserRole): Result<List<User>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val users = userRepository.findByRole(role)
            success(users)
        } catch (e: Exception) {
            logger.error("Error buscando usuarios por rol: ${e.message}", e)
            error("Error al buscar usuarios: ${e.message}")
        }
    }

    /**
     * Buscar todos los usuarios (solo para SUPERADMIN)
     */
    suspend fun findAll(): Result<List<User>> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val users = userRepository.findAll()
            success(users)
        } catch (e: Exception) {
            logger.error("Error buscando todos los usuarios: ${e.message}", e)
            error("Error al buscar usuarios: ${e.message}")
        }
    }

    /**
     * Crear nuevo usuario
     * Lógica de negocio: validaciones, hash de password
     * Todo en una sola transacción
     */
    suspend fun create(request: CreateUserRequest): Result<User> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val now = Clock.System.now()
            val organizationId = UUID.fromString(request.organizationId)
            val nameTrimmed = request.name.trim()
            val emailTrimmed = request.email.trim().lowercase()

            // Validación: nombre no puede estar vacío
            if (nameTrimmed.isBlank()) {
                return@newSuspendedTransaction error("El nombre del usuario es obligatorio")
            }

            // Validación: email no puede estar vacío
            if (emailTrimmed.isBlank()) {
                return@newSuspendedTransaction error("El email del usuario es obligatorio")
            }

            // Validación: email debe ser válido
            if (!emailTrimmed.contains("@")) {
                return@newSuspendedTransaction error("El email debe ser válido")
            }

            // Validación: password no puede estar vacío y debe tener al menos 6 caracteres
            if (request.password.isBlank() || request.password.length < 6) {
                return@newSuspendedTransaction error("La contraseña debe tener al menos 6 caracteres")
            }

            // Validación: no puede existir un usuario con el mismo email
            val existingUser = userRepository.findByEmail(emailTrimmed)
            if (existingUser != null) {
                return@newSuspendedTransaction error("Ya existe un usuario con el email '$emailTrimmed'")
            }

            // Hash de password
            val passwordHash = hashPassword(request.password)

            // Insertar usuario
            val userId = userRepository.insert(
                organizationId = organizationId,
                name = nameTrimmed,
                email = emailTrimmed,
                passwordHash = passwordHash,
                role = request.role,
                isActive = request.isActive,
                createdAt = now,
                updatedAt = now
            )

            // Obtener el usuario creado
            val createdUser = userRepository.findById(userId)
            if (createdUser != null) {
                success(createdUser)
            } else {
                error("Error al recuperar el usuario creado")
            }
        } catch (e: IllegalArgumentException) {
            error("Datos inválidos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error creando usuario: ${e.message}", e)
            error("Error al crear el usuario: ${e.message}")
        }
    }

    /**
     * Actualizar usuario existente
     * Lógica de negocio: validaciones, hash de password si se actualiza
     * Todo en una sola transacción
     */
    suspend fun update(id: String, updateRequest: UpdateUserRequest): Result<User> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val userId = UUID.fromString(id)
            val now = Clock.System.now()

            // Obtener usuario actual
            val currentUser = userRepository.findById(userId)
            if (currentUser == null) {
                return@newSuspendedTransaction error("Usuario no encontrado con ID: $id")
            }

            // Validaciones
            updateRequest.name?.let {
                if (it.trim().isBlank()) {
                    return@newSuspendedTransaction error("El nombre del usuario no puede estar vacío")
                }
            }

            updateRequest.email?.let { newEmail ->
                val emailTrimmed = newEmail.trim().lowercase()
                if (emailTrimmed.isBlank()) {
                    return@newSuspendedTransaction error("El email del usuario no puede estar vacío")
                }
                if (!emailTrimmed.contains("@")) {
                    return@newSuspendedTransaction error("El email debe ser válido")
                }
                // Verificar que no existe otro usuario con el mismo email
                val existingUser = userRepository.findByEmail(emailTrimmed)
                if (existingUser != null && existingUser.id != id) {
                    return@newSuspendedTransaction error("Ya existe un usuario con el email '$emailTrimmed'")
                }
            }

            updateRequest.password?.let {
                if (it.isBlank() || it.length < 6) {
                    return@newSuspendedTransaction error("La contraseña debe tener al menos 6 caracteres")
                }
            }

            // Hash de password si se actualiza
            val passwordHash = updateRequest.password?.let { hashPassword(it) }

            // Actualizar usuario
            val updated = userRepository.update(
                id = userId,
                name = updateRequest.name?.trim(),
                email = updateRequest.email?.trim()?.lowercase(),
                passwordHash = passwordHash,
                role = updateRequest.role,
                isActive = updateRequest.isActive,
                updatedAt = now
            )

            if (!updated) {
                return@newSuspendedTransaction error("Error al actualizar el usuario")
            }

            // Obtener usuario actualizado
            val updatedUser = userRepository.findById(userId)
            if (updatedUser != null) {
                success(updatedUser)
            } else {
                error("Error al recuperar el usuario actualizado")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de usuario inválido o datos incorrectos: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error actualizando usuario: ${e.message}", e)
            error("Error al actualizar el usuario: ${e.message}")
        }
    }

    /**
     * Eliminar usuario
     */
    suspend fun delete(id: String): Result<Boolean> = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction {
        try {
            val uuid = UUID.fromString(id)
            val deleted = userRepository.delete(uuid)
            if (deleted) {
                success(true)
            } else {
                error("Usuario no encontrado con ID: $id")
            }
        } catch (e: IllegalArgumentException) {
            error("ID de usuario inválido: $id")
        } catch (e: Exception) {
            logger.error("Error eliminando usuario: ${e.message}", e)
            error("Error al eliminar el usuario: ${e.message}")
        }
    }
}

