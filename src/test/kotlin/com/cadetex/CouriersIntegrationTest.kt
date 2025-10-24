package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CouriersIntegrationTest : IntegrationTestBase() {

    @Test
    fun `orgadmin can create courier in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"co@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val res = client.post("/couriers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"C1","phoneNumber":"123","address":"Addr","vehicleType":"Bike"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM couriers WHERE organization_id = '$orgId'::uuid AND name='C1'").use { rs ->
                    rs.next(); assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot create courier in different organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"Admin","email":"co2@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val res = client.post("/couriers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgB","name":"C2","phoneNumber":"123"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun `orgadmin can update all courier fields`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"co3@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val created = client.post("/couriers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"C","phoneNumber":"111"}""")
        }
        val courierId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val updated = client.put("/couriers/$courierId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"C2","phoneNumber":"222","address":"A2","vehicleType":"Car","isActive":false}""")
        }
        assertEquals(HttpStatusCode.OK, updated.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name, phone_number, address, vehicle_type, is_active FROM couriers WHERE id = '$courierId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("C2", rs.getString(1))
                    assertEquals("222", rs.getString(2))
                    assertEquals("A2", rs.getString(3))
                    assertEquals("Car", rs.getString(4))
                    assertEquals(false, rs.getBoolean(5))
                }
            }
        }
    }

    @Test
    fun `orgadmin can delete courier`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"co4@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val created = client.post("/couriers") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"DelC","phoneNumber":"111"}""")
        }
        val courierId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val del = client.delete("/couriers/$courierId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, del.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM couriers WHERE id = '$courierId'::uuid").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `list couriers returns own org only`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"Admin","email":"co5@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Create 2 in A
        repeat(2) {
            client.post("/couriers") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"organizationId":"$orgA","name":"C$it","phoneNumber":"111"}""")
            }
        }
        // Create 1 in B via SQL
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeUpdate("INSERT INTO couriers(id, organization_id, name, phone_number, is_active, created_at, updated_at) VALUES (gen_random_uuid(),'$orgB'::uuid,'CB','000',true,now(),now())")
            }
        }

        val res = client.get("/couriers") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, res.status)
        val arr = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        assertEquals(2, arr.size)
        assertTrue(arr.all { it.jsonObject["organizationId"]!!.jsonPrimitive.content == orgA })
    }
}


