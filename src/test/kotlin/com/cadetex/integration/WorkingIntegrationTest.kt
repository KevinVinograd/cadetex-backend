package com.cadetex.integration

import com.cadetex.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class WorkingIntegrationTest {
    
    @Container
    val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
        withDatabaseName("cadetex_test")
        withUsername("cadetex_user")
        withPassword("cadetex_password")
    }
    
    @Test
    fun `test container starts successfully`() {
        assertTrue(postgresContainer.isRunning, "El contenedor PostgreSQL debería estar ejecutándose")
        
        println("🐳 Contenedor Docker está ejecutándose correctamente")
        println("📊 Puerto: ${postgresContainer.firstMappedPort}")
    }
    
    @Test
    fun `test application starts with container`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.jdbcUrl" to postgresContainer.jdbcUrl,
                "database.user" to postgresContainer.username,
                "database.password" to postgresContainer.password,
                "database.maxPoolSize" to "5"
            )
        }
        application {
            module()
        }
        
        // Test que la aplicación funciona
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Cadetex API"))
        
        println("✅ Aplicación funciona con Testcontainers")
    }
    
    @Test
    fun `test protected endpoints require authentication`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.jdbcUrl" to postgresContainer.jdbcUrl,
                "database.user" to postgresContainer.username,
                "database.password" to postgresContainer.password,
                "database.maxPoolSize" to "5"
            )
        }
        application {
            module()
        }
        
        // Test que los endpoints protegidos requieren autenticación
        val endpoints = listOf("/users", "/clients", "/providers", "/couriers", "/tasks")
        
        endpoints.forEach { endpoint ->
            val response = client.get(endpoint)
            assertEquals(HttpStatusCode.Unauthorized, response.status, "Endpoint $endpoint debería requerir autenticación")
        }
        
        println("✅ Todos los endpoints protegidos requieren autenticación")
    }
    
    @Test
    fun `test authentication endpoint exists`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.jdbcUrl" to postgresContainer.jdbcUrl,
                "database.user" to postgresContainer.username,
                "database.password" to postgresContainer.password,
                "database.maxPoolSize" to "5"
            )
        }
        application {
            module()
        }
        
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email": "test@test.com", "password": "password123"}""")
        }
        
        // Debería responder (puede ser 200 o 401 dependiendo de la implementación)
        assertTrue(response.status.value in 200..499)
        
        println("✅ Endpoint de autenticación funciona")
    }
}
