package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import io.ktor.client.call.*

class OrganizationsIntegrationTest : IntegrationTestBase() {

    @Serializable
    data class CreateOrganizationRequest(val name: String)

    @Test
    fun `create organization as SUPERADMIN persists in DB`() = withReadyApp {
        // Seed org for registering superadmin
        val orgId = insertOrganization("SeedOrg")

        // Register SUPERADMIN
        val registerPayload = """
            {
              "organizationId": "$orgId",
              "name": "Admin",
              "email": "admin@x.com",
              "password": "secret",
              "role": "SUPERADMIN"
            }
        """.trimIndent()

        val regRes = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerPayload)
        }
        assertEquals(HttpStatusCode.Created, regRes.status)
        val regBody = Json.parseToJsonElement(regRes.bodyAsText()).jsonObject
        val token = regBody["token"]!!.jsonPrimitive.content
        assertNotNull(token)

        // Create organization via endpoint
        val createPayload = Json.encodeToString(CreateOrganizationRequest.serializer(), CreateOrganizationRequest("Org1"))
        val createRes = client.post("/organizations") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(createPayload)
        }
        assertEquals(HttpStatusCode.Created, createRes.status)

        // Verify in DB
        dbConnection().use { conn ->
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT count(*) FROM organizations WHERE name = 'Org1'")
                rs.next()
                val count = rs.getInt(1)
                assertEquals(1, count)
            }
        }
    }

    @Test
    fun `list organizations requires SUPERADMIN`() = withReadyApp {
        val orgId = insertOrganization("SeedOrg")

        // Register ORGADMIN
        val registerOrgAdmin = """
            {"organizationId":"$orgId","name":"OrgAdmin","email":"orgadmin@x.com","password":"secret","role":"ORGADMIN"}
        """.trimIndent()
        val regRes = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerOrgAdmin)
        }
        assertEquals(HttpStatusCode.Created, regRes.status)
        val token = Json.parseToJsonElement(regRes.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val listResForbidden = client.get("/organizations") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Forbidden, listResForbidden.status)

        // Register SUPERADMIN
        val regSuper = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"admin2@x.com","password":"secret","role":"SUPERADMIN"}""")
        }
        assertEquals(HttpStatusCode.Created, regSuper.status)
        val tokenSuper = Json.parseToJsonElement(regSuper.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // As SUPERADMIN -> 200 OK
        val listRes = client.get("/organizations") {
            header(HttpHeaders.Authorization, "Bearer $tokenSuper")
        }
        assertEquals(HttpStatusCode.OK, listRes.status)
    }

    @Test
    fun `orgadmin cannot create organization`() = withReadyApp {
        val orgId = insertOrganization("SeedOrg")
        val regOrgAdmin = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"OrgAdmin","email":"orgadmin2@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        assertEquals(HttpStatusCode.Created, regOrgAdmin.status)
        val tokenOrgAdmin = Json.parseToJsonElement(regOrgAdmin.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val createRes = client.post("/organizations") {
            header(HttpHeaders.Authorization, "Bearer $tokenOrgAdmin")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"ShouldFail"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, createRes.status)
    }

    @Test
    fun `superadmin can update organization name and updated_at`() = withReadyApp {
        val seedId = insertOrganization("Before")
        // superadmin
        val regSuper = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$seedId","name":"Admin","email":"admin3@x.com","password":"secret","role":"SUPERADMIN"}""")
        }
        assertEquals(HttpStatusCode.Created, regSuper.status)
        val tokenSuper = Json.parseToJsonElement(regSuper.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // capture previous updated_at
        var beforeUpdatedAt: String? = null
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT updated_at FROM organizations WHERE id = '$seedId'::uuid").use { rs ->
                    if (rs.next()) beforeUpdatedAt = rs.getTimestamp(1).toInstant().toString()
                }
            }
        }

        val updateRes = client.put("/organizations/$seedId") {
            header(HttpHeaders.Authorization, "Bearer $tokenSuper")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"After"}""")
        }
        assertEquals(HttpStatusCode.OK, updateRes.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name, updated_at FROM organizations WHERE id = '$seedId'::uuid").use { rs ->
                    rs.next()
                    val name = rs.getString(1)
                    val updatedAt = rs.getTimestamp(2).toInstant().toString()
                    assertEquals("After", name)
                    // updated_at debe ser >= beforeUpdatedAt
                    if (beforeUpdatedAt != null) {
                        assert(updatedAt >= beforeUpdatedAt!!) { "updated_at not advanced" }
                    }
                }
            }
        }
    }

    @Test
    fun `update returns 404 for unknown organization`() = withReadyApp {
        val seedId = insertOrganization("Seed")
        val regSuper = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$seedId","name":"Admin","email":"admin4@x.com","password":"secret","role":"SUPERADMIN"}""")
        }
        val tokenSuper = Json.parseToJsonElement(regSuper.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val randomId = java.util.UUID.randomUUID().toString()
        val res = client.put("/organizations/$randomId") {
            header(HttpHeaders.Authorization, "Bearer $tokenSuper")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"X"}""")
        }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }
}


