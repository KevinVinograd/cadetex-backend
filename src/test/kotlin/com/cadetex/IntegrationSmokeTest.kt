package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class IntegrationSmokeTest {

    @Test
    fun `app boots with Testcontainers Postgres and serves root`() = testApplication {
        val container = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("cadetex")
            withUsername("test")
            withPassword("test")
            start()
        }
        try {
            // Configurar aplicación para apuntar al contenedor sin tocar system properties
            environment {
                config = MapApplicationConfig(
                    "database.jdbcUrl" to container.jdbcUrl,
                    "database.user" to container.username,
                    "database.password" to container.password,
                    "database.maxPoolSize" to "5"
                )
            }
            application {
                module()
            }

            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Cadetex API"))

            // Verificar que las tablas se hayan creado por Exposed en el arranque
            DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname='public'")
                    val tables = mutableSetOf<String>()
                    while (rs.next()) tables.add(rs.getString(1))
                    // Algunas tablas clave
                    assertTrue("organizations" in tables)
                    assertTrue("users" in tables)
                    assertTrue("tasks" in tables)
                }
            }
        } finally {
            container.stop()
        }
    }
}


