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

class TasksIntegrationTest : IntegrationTestBase() {

    @Test
    fun `orgadmin can create task in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"ta@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val payload = """
            {"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL","city":"BA","notes":"n"}
        """.trimIndent()
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM tasks WHERE organization_id = '$orgId'::uuid AND type='DELIVER' AND city='BA'").use { rs ->
                    rs.next(); assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot create task in different organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"Admin","email":"tb@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val payload = """{"organizationId":"$orgB","type":"DELIVER","status":"PENDING","priority":"NORMAL"}"""
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun `orgadmin can update all mutable fields of task`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"tc@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val created = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL"}""")
        }
        val taskId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val updatePayload = """
            {
              "type":"RETIRE",
              "referenceNumber":"REF2",
              "addressOverride":"Addr2",
              "city":"City2",
              "province":"Prov2",
              "contact":"John",
              "status":"CONFIRMED",
              "priority":"URGENT",
              "scheduledDate":"2025-01-01",
              "notes":"Updated",
              "photoRequired": true,
              "mbl":"M1",
              "hbl":"H1",
              "freightCert": true,
              "foCert": true,
              "bunkerCert": true,
              "receiptPhotoUrl":"http://example/receipt.png"
            }
        """.trimIndent()
        val upd = client.put("/tasks/$taskId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(updatePayload)
        }
        assertEquals(HttpStatusCode.OK, upd.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT type,reference_number,address_override,city,province,contact,status,priority,scheduled_date,notes,photo_required,mbl,hbl,freight_cert,fo_cert,bunker_cert,receipt_photo_url FROM tasks WHERE id = '$taskId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("RETIRE", rs.getString(1))
                    assertEquals("REF2", rs.getString(2))
                    assertEquals("Addr2", rs.getString(3))
                    assertEquals("City2", rs.getString(4))
                    assertEquals("Prov2", rs.getString(5))
                    assertEquals("John", rs.getString(6))
                    assertEquals("CONFIRMED", rs.getString(7))
                    assertEquals("URGENT", rs.getString(8))
                    assertEquals("2025-01-01", rs.getString(9))
                    assertEquals("Updated", rs.getString(10))
                    assertTrue(rs.getBoolean(11))
                    assertEquals("M1", rs.getString(12))
                    assertEquals("H1", rs.getString(13))
                    assertTrue(rs.getBoolean(14))
                    assertTrue(rs.getBoolean(15))
                    assertTrue(rs.getBoolean(16))
                    assertEquals("http://example/receipt.png", rs.getString(17))
                }
            }
        }
    }

    @Test
    fun `orgadmin can delete task in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"td@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        val created = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL"}""")
        }
        val taskId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val del = client.delete("/tasks/$taskId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, del.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM tasks WHERE id = '$taskId'::uuid").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `list tasks by organization returns only own org`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"Admin","email":"te@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Two tasks in A via API
        repeat(2) {
            client.post("/tasks") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"organizationId":"$orgA","type":"DELIVER","status":"PENDING","priority":"NORMAL"}""")
            }
        }
        // One task in B directly via SQL
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeUpdate("INSERT INTO tasks(id,organization_id,type,status,priority,created_at,updated_at) VALUES (gen_random_uuid(),'$orgB'::uuid,'DELIVER','PENDING','NORMAL',now(),now())")
            }
        }

        val list = client.get("/tasks") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, list.status)
        val arr = Json.parseToJsonElement(list.bodyAsText()).jsonArray
        assertEquals(2, arr.size)
        assertTrue(arr.all { it.jsonObject["organizationId"]!!.jsonPrimitive.content == orgA })
    }

    @Test
    fun `list tasks by status filters and returns only own org`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"Admin","email":"tf@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // In OrgA: one CONFIRMED and one PENDING
        client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","type":"DELIVER","status":"CONFIRMED","priority":"NORMAL"}""")
        }
        client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","type":"DELIVER","status":"PENDING","priority":"NORMAL"}""")
        }
        // In OrgB: one CONFIRMED via SQL
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeUpdate("INSERT INTO tasks(id,organization_id,type,status,priority,created_at,updated_at) VALUES (gen_random_uuid(),'$orgB'::uuid,'DELIVER','CONFIRMED','NORMAL',now(),now())")
            }
        }

        val res = client.get("/tasks/status/CONFIRMED") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, res.status)
        val arr = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        assertEquals(1, arr.size)
        assertTrue(arr.all { it.jsonObject["organizationId"]!!.jsonPrimitive.content == orgA })
    }
}


