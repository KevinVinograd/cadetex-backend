package com.cadetex

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UsersIntegrationTest : IntegrationTestBase() {

    private suspend fun ApplicationTestBuilder.registerUser(organizationId: String, email: String, role: String): Pair<String, String> {
        val res = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$organizationId","name":"User","email":"$email","password":"secret","role":"$role"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        val json = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        val token = json["token"]!!.jsonPrimitive.content
        val user = json["user"]!!.jsonObject
        val userId = user["id"]!!.jsonPrimitive.content
        return userId to token
    }

    @Test
    fun `orgadmin can create user in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val (_, adminToken) = registerUser(orgId, "oa@x.com", "ORGADMIN")

        val res = client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Emp","email":"emp@x.com","password":"password123","role":"COURIER","isActive":true}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM users WHERE organization_id='$orgId'::uuid AND email='emp@x.com'").use { rs ->
                    rs.next(); assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot create user in other organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val (_, adminToken) = registerUser(orgA, "oa2@x.com", "ORGADMIN")

        val res = client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgB","name":"Emp","email":"emp2@x.com","password":"password123","role":"COURIER"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM users WHERE organization_id='$orgB'::uuid AND email='emp2@x.com'").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `superadmin can create user in any organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val (_, saToken) = registerUser(orgA, "sa@x.com", "SUPERADMIN")

        val res = client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $saToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgB","name":"Emp","email":"emp3@x.com","password":"password123","role":"COURIER"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
    }

    @Test
    fun `orgadmin can update all user fields in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val (_, adminToken) = registerUser(orgId, "oa3@x.com", "ORGADMIN")

        // Create target user
        val created = client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"U","email":"u@x.com","password":"password123","role":"COURIER"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val userId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val upd = client.put("/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"U2","email":"u2@x.com","role":"ORGADMIN","isActive":false}""")
        }
        assertEquals(HttpStatusCode.OK, upd.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name,email,role,is_active FROM users WHERE id='$userId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("U2", rs.getString(1))
                    assertEquals("u2@x.com", rs.getString(2))
                    assertEquals("ORGADMIN", rs.getString(3))
                    assertEquals(false, rs.getBoolean(4))
                }
            }
        }
    }

    @Test
    fun `user can update self basic fields`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val (userId, userToken) = registerUser(orgId, "self@x.com", "COURIER")

        val upd = client.put("/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Me","email":"me@x.com","password":"newpass"}""")
        }
        assertEquals(HttpStatusCode.OK, upd.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name,email FROM users WHERE id='$userId'::uuid").use { rs ->
                    rs.next()
                    assertEquals("Me", rs.getString(1))
                    assertEquals("me@x.com", rs.getString(2))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot update user of other organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val (_, adminToken) = registerUser(orgA, "oa4@x.com", "ORGADMIN")

        // Seed user in orgB
        val (userBId, _) = registerUser(orgB, "ub@x.com", "COURIER")

        val res = client.put("/users/$userBId") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Nope"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun `orgadmin can delete user in own organization`() = withReadyApp {
        val orgId = insertOrganization("OrgA")
        val (_, adminToken) = registerUser(orgId, "oa5@x.com", "ORGADMIN")
        val created = client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgId","name":"Del","email":"del@x.com","password":"password123","role":"COURIER"}""")
        }
        val userId = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val del = client.delete("/users/$userId") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        assertEquals(HttpStatusCode.NoContent, del.status)

        dbConnection().use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT count(*) FROM users WHERE id='$userId'::uuid").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `orgadmin cannot delete user in other organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val (_, adminToken) = registerUser(orgA, "oa6@x.com", "ORGADMIN")
        val (userBId, _) = registerUser(orgB, "delb@x.com", "COURIER")

        val del = client.delete("/users/$userBId") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        assertEquals(HttpStatusCode.Forbidden, del.status)
    }

    @Test
    fun `list users returns only own organization`() = withReadyApp {
        val orgA = insertOrganization("OrgA")
        val orgB = insertOrganization("OrgB")
        val (_, adminToken) = registerUser(orgA, "oa7@x.com", "ORGADMIN")
        // create 2 in A and 1 in B
        client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"A1","email":"a1@x.com","password":"password123","role":"COURIER"}""")
        }.let { assertEquals(HttpStatusCode.Created, it.status) }
        client.post("/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"organizationId":"$orgA","name":"A2","email":"a2@x.com","password":"password123","role":"COURIER"}""")
        }.let { assertEquals(HttpStatusCode.Created, it.status) }
        // seed in B (outside admin org)
        registerUser(orgB, "b1@x.com", "COURIER")

        val list = client.get("/users") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        assertEquals(HttpStatusCode.OK, list.status)
        val arr = Json.parseToJsonElement(list.bodyAsText()).jsonArray
        assertTrue(arr.size == 3) // includes the admin plus the 2 created in A
    }
}


