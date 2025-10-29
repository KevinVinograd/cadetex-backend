package com.cadetex

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

    protected lateinit var pg: PostgreSQLContainer<*>

    @BeforeAll
    fun globalSetup() {
        pg = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("kdt")
            withUsername("test")
            withPassword("test")
            start()
        }
    }

    @AfterAll
    fun globalTeardown() {
        pg.stop()
    }

    protected fun dbConnection(): Connection =
        DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password)

    @BeforeEach
    fun cleanDatabase() {
        dbConnection().use { conn ->
            conn.createStatement().use { st ->
                // Truncate only existing tables to avoid failures before schema is created
                st.execute(
                    """
                    DO $$
                    DECLARE
                      stmt text;
                    BEGIN
                      SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(tablename), ', ') || ' RESTART IDENTITY CASCADE'
                        INTO stmt
                      FROM pg_tables
                      WHERE schemaname = 'public';
                      IF stmt IS NOT NULL THEN
                        EXECUTE stmt;
                      END IF;
                    END $$;
                    """.trimIndent()
                )
            }
        }
    }

    protected fun insertOrganization(name: String = "Org-${UUID.randomUUID()}"): String {
        val id = UUID.randomUUID().toString()
        dbConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate("INSERT INTO organizations(id, name, created_at, updated_at) VALUES ('$id'::uuid, '${name.replace("'", "''")}', now(), now())")
            }
        }
        return id
    }

    protected fun withReadyApp(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "database.jdbcUrl" to pg.jdbcUrl,
                    "database.user" to pg.username,
                    "database.password" to pg.password,
                    "database.maxPoolSize" to "5"
                )
            }
            application { module() }
            val res = client.get("/")
            assertEquals(HttpStatusCode.OK, res.status)
            block()
        }
    }
}


