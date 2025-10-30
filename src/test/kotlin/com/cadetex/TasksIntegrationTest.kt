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
            {"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL","notes":"n"}
        """.trimIndent()
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM tasks WHERE organization_id = '$orgId'::uuid AND type='DELIVER'").use { rs ->
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
              "addressOverride":{
                "street":"Addr2",
                "city":"City2",
                "province":"Prov2"
              },
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
                // Verificar campos directos en tasks
                st.executeQuery("""
                    SELECT t.type, t.reference_number, t.contact, t.status, t.priority, 
                           t.scheduled_date, t.notes, t.photo_required, t.mbl, t.hbl, 
                           t.freight_cert, t.fo_cert, t.bunker_cert, t.receipt_photo_url,
                           a.city, a.province, a.street
                    FROM tasks t
                    LEFT JOIN addresses a ON t.address_override_id = a.id
                    WHERE t.id = '$taskId'::uuid
                """.trimIndent()).use { rs ->
                    rs.next()
                    assertEquals("RETIRE", rs.getString(1))
                    assertEquals("REF2", rs.getString(2))
                    assertEquals("John", rs.getString(3))
                    assertEquals("CONFIRMED", rs.getString(4))
                    assertEquals("URGENT", rs.getString(5))
                    assertEquals("2025-01-01", rs.getString(6))
                    assertEquals("Updated", rs.getString(7))
                    assertTrue(rs.getBoolean(8))
                    assertEquals("M1", rs.getString(9))
                    assertEquals("H1", rs.getString(10))
                    assertTrue(rs.getBoolean(11))
                    assertTrue(rs.getBoolean(12))
                    assertTrue(rs.getBoolean(13))
                    assertEquals("http://example/receipt.png", rs.getString(14))
                    // Verificar dirección en addresses
                    assertEquals("City2", rs.getString(15))
                    assertEquals("Prov2", rs.getString(16))
                    assertEquals("Addr2", rs.getString(17))
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

    @Test
    fun `task returns client address when clientId is set`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"tg@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Crear dirección del cliente
        val clientAddressId = insertAddress(street = "Client Street", city = "Client City", province = "Client Province")
        val clientId = insertClient(orgId, "Test Client", clientAddressId)

        // Crear tarea con clientId
        val payload = """
            {"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL","clientId":"$clientId"}
        """.trimIndent()
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)
        
        val taskJson = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        val addressJson = taskJson["address"]?.jsonObject
        assertTrue(addressJson != null, "Task should have address from client")
        assertEquals("Client Street", addressJson?.get("street")?.jsonPrimitive?.content)
        assertEquals("Client City", addressJson?.get("city")?.jsonPrimitive?.content)
        assertEquals("Client Province", addressJson?.get("province")?.jsonPrimitive?.content)
    }

    @Test
    fun `task returns provider address when providerId is set`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"th@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Crear dirección del proveedor
        val providerAddressId = insertAddress(street = "Provider Street", city = "Provider City", province = "Provider Province")
        val providerId = insertProvider(orgId, "Test Provider", providerAddressId)

        // Crear tarea con providerId
        val payload = """
            {"organizationId":"$orgId","type":"RETIRE","status":"PENDING","priority":"NORMAL","providerId":"$providerId"}
        """.trimIndent()
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)
        
        val taskJson = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        val addressJson = taskJson["address"]?.jsonObject
        assertTrue(addressJson != null, "Task should have address from provider")
        assertEquals("Provider Street", addressJson?.get("street")?.jsonPrimitive?.content)
        assertEquals("Provider City", addressJson?.get("city")?.jsonPrimitive?.content)
        assertEquals("Provider Province", addressJson?.get("province")?.jsonPrimitive?.content)
    }

    @Test
    fun `task returns override address when addressOverrideId is set, even with client`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"ti@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Crear dirección del cliente
        val clientAddressId = insertAddress(street = "Client Street", city = "Client City", province = "Client Province")
        val clientId = insertClient(orgId, "Test Client", clientAddressId)

        // Crear tarea con clientId y addressOverride
        val payload = """
            {
              "organizationId":"$orgId",
              "type":"DELIVER",
              "status":"PENDING",
              "priority":"NORMAL",
              "clientId":"$clientId",
              "addressOverride":{
                "street":"Override Street",
                "city":"Override City",
                "province":"Override Province"
              }
            }
        """.trimIndent()
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)
        
        val taskJson = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        val addressJson = taskJson["address"]?.jsonObject
        assertTrue(addressJson != null, "Task should have override address")
        // Debe devolver la dirección del override, no la del cliente
        assertEquals("Override Street", addressJson?.get("street")?.jsonPrimitive?.content)
        assertEquals("Override City", addressJson?.get("city")?.jsonPrimitive?.content)
        assertEquals("Override Province", addressJson?.get("province")?.jsonPrimitive?.content)
        
        // Verificar que addressOverrideId está guardado
        val taskId = taskJson["id"]?.jsonPrimitive?.content
        assertTrue(taskId != null)
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT address_override_id FROM tasks WHERE id = '$taskId'::uuid").use { rs ->
                    rs.next()
                    assertTrue(rs.getString(1) != null, "addressOverrideId should be set")
                }
            }
        }
    }

    @Test
    fun `task without client, provider or override returns null address`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"tj@x.com","password":"secret","role":"ORGADMIN"}""")
        }
        val token = Json.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content

        // Crear tarea sin client, provider ni override
        val payload = """
            {"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL"}
        """.trimIndent()
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.Created, res.status)
        
        val taskJson = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        val addressJson = taskJson["address"]
        // address puede ser null en JSON si no hay client, provider ni override
        assertTrue(addressJson == null, "Task without address source should have null address")
    }
}


