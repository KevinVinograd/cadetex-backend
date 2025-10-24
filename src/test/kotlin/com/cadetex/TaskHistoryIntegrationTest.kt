package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskHistoryIntegrationTest : IntegrationTestBase() {

    private suspend fun ApplicationTestBuilder.registerOrgAdmin(orgId: String, email: String): String {
        val res = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Admin","email":"$email","password":"secret","role":"ORGADMIN"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
    }

    private suspend fun ApplicationTestBuilder.createTask(orgId: String, token: String): String {
        val res = client.post("/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","type":"DELIVER","status":"PENDING","priority":"NORMAL"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content
    }

    @Test
    fun `org user can create and list task history in own org`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val token = registerOrgAdmin(orgId, "th@x.com")
        val taskId = createTask(orgId, token)

        val create = client.post("/task-history") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"taskId":"$taskId","previousStatus":"PENDING","newStatus":"CONFIRMED","changedBy":null}""")
        }
        assertEquals(HttpStatusCode.Created, create.status)

        val list = client.get("/task-history/task/$taskId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, list.status)
        val arr = Json.parseToJsonElement(list.bodyAsText()).jsonArray
        assertEquals(1, arr.size)
        assertTrue(arr[0].jsonObject["newStatus"]!!.jsonPrimitive.content == "CONFIRMED")
    }

    @Test
    fun `org user cannot create history for task in other org`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val token = registerOrgAdmin(orgA, "th2@x.com")

        // create task in B directly
        val taskId = java.util.UUID.randomUUID().toString()
        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeUpdate("INSERT INTO tasks(id,organization_id,type,status,priority,created_at,updated_at) VALUES ('$taskId'::uuid,'$orgB'::uuid,'DELIVER','PENDING','NORMAL',now(),now())")
            }
        }

        val res = client.post("/task-history") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"taskId":"$taskId","newStatus":"CONFIRMED"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun `update and delete task history`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val token = registerOrgAdmin(orgId, "th3@x.com")
        val taskId = createTask(orgId, token)

        val created = client.post("/task-history") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"taskId":"$taskId","previousStatus":"PENDING","newStatus":"CONFIRMED"}""")
        }
        val histId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val upd = client.put("/task-history/$histId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"newStatus":"COMPLETED"}""")
        }
        assertEquals(HttpStatusCode.OK, upd.status)

        val del = client.delete("/task-history/$histId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, del.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM task_history WHERE id = '$histId'::uuid").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }
}


