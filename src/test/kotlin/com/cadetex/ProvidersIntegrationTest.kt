package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProvidersIntegrationTest : IntegrationTestBase() {

    @Test
    fun `orgadmin can create provider in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val regRes = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"OrgAdmin","email":"pa@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(regRes.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val payload = """
            {"organizationId":"$orgId","name":"Prov1","address":"Addr","city":"City","province":"Prov","contactName":"John","contactPhone":"123","isActive":true}
        """.trimIndent()
        val res = client.post("/providers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM providers WHERE organization_id = '$orgId'::uuid AND name='Prov1'").use { rs ->
                    rs.next(); assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot create provider in different organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")

        val regRes = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"OrgAdmin","email":"pa2@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(regRes.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val payload = """
            {"organizationId":"$orgB","name":"ProvX","address":"Addr"}
        """.trimIndent()
        val res = client.post("/providers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM providers WHERE organization_id = '$orgB'::uuid AND name='ProvX'").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin can update all provider fields in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"OrgAdmin","email":"pa3@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Create
        val created = client.post("/providers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"A","address":"Adr","city":"C","province":"P","contactName":"CN","contactPhone":"111"}""")
        }
        val createdJson = Json.parseToJsonElement(created.bodyAsText()).jsonObject
        val providerId = createdJson["id"]!!.jsonPrimitive.content

        // Update all mutable fields
        val updated = client.put("/providers/$providerId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""
                {"name":"B","address":"Adr2","city":"C2","province":"P2","contactName":"CN2","contactPhone":"222","isActive":false}
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.OK, updated.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name,address,city,province,contact_name,contact_phone FROM providers WHERE id = '$providerId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("B", rs.getString(1))
                    assertEquals("Adr2", rs.getString(2))
                    assertEquals("C2", rs.getString(3))
                    assertEquals("P2", rs.getString(4))
                    assertEquals("CN2", rs.getString(5))
                    assertEquals("222", rs.getString(6))
                }
            }
        }
    }

    @Test
    fun `orgadmin can delete provider in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"OrgAdmin","email":"pa4@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Create
        val created = client.post("/providers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"DelMe","address":"Adr"}""")
        }
        val providerId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val delRes = client.delete("/providers/$providerId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NoContent, delRes.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM providers WHERE id = '$providerId'::uuid").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }
}


