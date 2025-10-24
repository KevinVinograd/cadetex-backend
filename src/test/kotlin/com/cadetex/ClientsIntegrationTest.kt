package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientsIntegrationTest : IntegrationTestBase() {

    @Test
    fun `orgadmin can create client in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")

        val regRes = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"OrgAdmin","email":"oa@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        assertEquals(HttpStatusCode.Created, regRes.status)
        val token = Json.parseToJsonElement(regRes.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val payload = """
            {"organizationId":"$orgId","name":"Acme","address":"Street 1","city":"BA","province":"BA","phoneNumber":"123","email":"acme@x.com","isActive":true}
        """.trimIndent()
        val createRes = client.post("/clients") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, createRes.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM clients WHERE organization_id = '$orgId'::uuid AND name='Acme'").use { rs ->
                    rs.next()
                    assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin can update all client fields in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"OrgAdmin","email":"oa3@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Create initial client
        val createPayload = """{"organizationId":"$orgId","name":"A","address":"Addr","city":"City","province":"Prov","phoneNumber":"111","email":"a@x.com","isActive":true}"""
        val created = client.post("/clients") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(createPayload)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val createdJson = Json.parseToJsonElement(created.bodyAsText()).jsonObject
        val clientId = createdJson["id"]!!.jsonPrimitive.content

        // Update all mutable fields
        val updatePayload = """
            {
              "name":"B",
              "address":"Addr2",
              "city":"City2",
              "province":"Prov2",
              "phoneNumber":"222",
              "email":"b@x.com",
              "isActive": false
            }
        """.trimIndent()
        val updated = client.put("/clients/$clientId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(updatePayload)
        }
        assertEquals(HttpStatusCode.OK, updated.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name,address,city,province,phone_number,email,is_active FROM clients WHERE id = '$clientId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("B", rs.getString(1))
                    assertEquals("Addr2", rs.getString(2))
                    assertEquals("City2", rs.getString(3))
                    assertEquals("Prov2", rs.getString(4))
                    assertEquals("222", rs.getString(5))
                    assertEquals("b@x.com", rs.getString(6))
                    assertEquals(false, rs.getBoolean(7))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot update client of another organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")

        // Seed client in orgB directly
        val clientId = java.util.UUID.randomUUID().toString()
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeUpdate(
                    "INSERT INTO clients(id, organization_id, name, address, city, province, phone_number, email, is_active, created_at, updated_at) " +
                    "VALUES ('$clientId'::uuid, '$orgB'::uuid, 'N', 'A', 'C', 'P', '111', 'n@x.com', true, now(), now())"
                )
            }
        }

        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"OrgAdmin","email":"oa4@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val res = client.put("/clients/$clientId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"X"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)

        // DB unchanged
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name FROM clients WHERE id = '$clientId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("N", rs.getString(1))
                }
            }
        }
    }
    @Test
    fun `orgadmin cannot create client in different organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")

        val regRes = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"OrgAdmin","email":"oa2@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(regRes.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val payload = """
            {"organizationId":"$orgB","name":"Other","address":"Street 1","city":"BA","province":"BA"}
        """.trimIndent()
        val res = client.post("/clients") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM clients WHERE organization_id = '$orgB'::uuid AND name='Other'").use { rs ->
                    rs.next()
                    assertEquals(0, rs.getInt(1))
                }
            }
        }
    }
}


