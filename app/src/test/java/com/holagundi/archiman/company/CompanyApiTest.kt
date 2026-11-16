package com.holagundi.archiman.company

import com.holagundi.archiman.sync.HttpResponse
import com.holagundi.archiman.sync.HttpTransport
import com.holagundi.archiman.sync.KeyValues
import com.holagundi.archiman.sync.SecretBox
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CompanyApiTest {
    private val connection = CompanyConnection("https://company.example", "/api/auth")
    private fun response(body: String = "{}", status: Int = 200, headers: Map<String, List<String>> = emptyMap()) = HttpResponse(status, headers, body)
    private class FakeHttp : HttpTransport {
        val responses = java.util.ArrayDeque<HttpResponse>()
        val requests = mutableListOf<Request>()
        var failure: IOException? = null
        class Request(val method: String, val url: String, val headers: Map<String, String>, val body: String?)
        override fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse {
            requests += Request(method, url, headers, body)
            failure?.let { throw it }
            return responses.removeFirst()
        }
    }
    private class Values : KeyValues {
        val map = mutableMapOf<String, String>()
        override fun get(key: String) = map[key]
        override fun put(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
    }
    private val box = object : SecretBox {
        override fun seal(plain: ByteArray) = plain.map { (it.toInt() xor 0x53).toByte() }.toByteArray()
        override fun open(sealed: ByteArray) = seal(sealed)
    }
    private fun me() = response("""{"user":{"id":"user-1","email":"staff@company.example","name":"Demo Staff","twoFactorEnabled":true},"organization":{"id":"22222222-2222-4222-8222-222222222222","name":"Demo Company"},"roles":["Staff"]}""")
    private fun permissions() = response("""{"permissions":["organization.read"]}""")
    private fun FakeHttp.identity() { responses += me(); responses += permissions() }
    private fun fixture() = javaClass.getResourceAsStream("/contracts/mobile-connection-v1.json")!!.bufferedReader().use { it.readText() }

    @Test fun `connection uses the exported backend contract and readiness`() {
        val http = FakeHttp()
        http.responses += response("""{"status":"ok","service":"myna-api","version":"0.1.0"}""")
        http.responses += response(fixture())
        http.responses += response("""{"status":"ready"}""")
        assertEquals(connection, CompanyApiClient(http).connect("https://company.example/"))
        assertEquals(listOf("/health", "/v1/mobile/connection", "/health/ready"), http.requests.map { java.net.URI(it.url).path })
        assertTrue(http.requests.all { "Authorization" !in it.headers })
    }

    @Test fun `rejects plain http credentials query fragments paths and invalid ports`() {
        val api = CompanyApiClient(FakeHttp())
        listOf("http://company.example", "https://a:b@company.example", "https://company.example?token=x", "https://company.example/#x", "https://company.example/api", "https://company.example:0", "https://company.example:99999", "file:///tmp/demo").forEach {
            try { api.normalizeAddress(it); fail("unsafe address accepted") } catch (e: CompanyApiException) { assertEquals("INVALID_ADDRESS", e.errorCode) }
        }
        val debug = CompanyApiClient(FakeHttp(), true)
        assertEquals("http://127.0.0.1:34872", debug.normalizeAddress("http://127.0.0.1:34872"))
        try { debug.normalizeAddress("http://office.example"); fail() } catch (_: CompanyApiException) { }
    }

    @Test fun `rejects a newer incompatible API before any login`() {
        val http = FakeHttp()
        http.responses += response("""{"status":"ok","service":"myna-api"}""")
        http.responses += response(JSONObject(fixture()).put("minimumVersionCode", 999).toString())
        try { CompanyApiClient(http).connect(connection.url); fail() } catch (e: CompanyApiException) { assertEquals("INCOMPATIBLE_API", e.errorCode) }
        assertEquals(2, http.requests.size)
    }

    @Test fun `rejects an auth path on a different origin`() {
        val http = FakeHttp()
        http.responses += response("""{"status":"ok","service":"myna-api"}""")
        http.responses += response(JSONObject(fixture()).put("authBasePath", "https://other.example/auth").toString())
        try { CompanyApiClient(http).connect(connection.url); fail() } catch (e: CompanyApiException) { assertEquals("INCOMPATIBLE_API", e.errorCode) }
    }

    @Test fun `pending MFA header is never stored and the verified replacement token is sealed`() {
        val http = FakeHttp(); val values = Values(); val store = CompanyAccountStore(values, box); store.connect(connection)
        val account = CompanyAccount(store, CompanyApiClient(http))
        http.responses += response("""{"twoFactorRedirect":true}""", headers = mapOf("set-auth-token" to listOf("not-a-session"), "Set-Cookie" to listOf("mfa=challenge; HttpOnly; Secure")))
        account.signIn("staff@company.example", "a private password")
        assertNull(store.load()); assertNotNull(account.pending); assertFalse(values.map.containsKey("session"))
        assertFalse(values.map.toString().contains("challenge"))
        http.responses += response(headers = mapOf("set-auth-token" to listOf("rotated-secret")))
        http.identity()
        account.verify("123456", false)
        assertEquals("rotated-secret", store.load()!!.token); assertNull(account.pending)
        assertEquals("mfa=challenge", http.requests[1].headers["Cookie"])
        assertFalse(http.requests[1].headers.containsKey("Authorization"))
        assertEquals("Bearer rotated-secret", http.requests[2].headers["Authorization"])
        assertFalse(values.map.toString().contains("rotated-secret"))
        assertFalse(values.map.toString().contains("staff@company.example"))
    }

    @Test fun `a rejected membership does not persist a new session`() {
        val http = FakeHttp(); val values = Values(); val store = CompanyAccountStore(values, box); store.connect(connection)
        http.responses += response(headers = mapOf("set-auth-token" to listOf("rejected-token")))
        http.responses += response("""{"error":{"code":"NOT_A_MEMBER","message":"private server details"}}""", 403)
        try { CompanyAccount(store, CompanyApiClient(http)).signIn("x@example.com", "password"); fail() } catch (e: CompanyApiException) { assertFalse(e.message!!.contains("private")) }
        assertNull(store.load())
    }

    private fun stored(store: CompanyAccountStore) = CompanySession(connection, "original-secret", CompanyIdentity("1", "x@example.com", "Demo", "Demo", listOf("Project Manager"), listOf("organization.read", "projects.read", "projects.write"), false, "22222222-2222-4222-8222-222222222222")).also { store.connect(connection); store.save(it) }

    @Test fun `revoked sessions and suspended memberships clear all cached identity and permissions`() {
        listOf(401 to "UNAUTHENTICATED", 403 to "MEMBERSHIP_SUSPENDED").forEach { (status, code) ->
            val values = Values(); val store = CompanyAccountStore(values, box); stored(store); val http = FakeHttp()
            http.responses += response("""{"error":{"code":"$code"}}""", status)
            try { CompanyAccount(store, CompanyApiClient(http)).refresh(); fail() } catch (_: CompanyApiException) { }
            assertNull(store.load()); assertFalse(values.map.containsKey("session")); assertNotNull(store.connection())
        }
    }

    @Test fun `offline check keeps only the encrypted credential and clears visible identity`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp(); http.failure = IOException("offline")
        try { CompanyAccount(store, CompanyApiClient(http)).refresh(); fail() } catch (_: IOException) { }
        assertEquals("original-secret", store.savedToken())
        assertNull(store.load())
    }

    @Test fun `sign out clears local secrets even when server revocation is unreachable`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp(); http.failure = IOException("offline")
        try { CompanyAccount(store, CompanyApiClient(http)).signOut(); fail() } catch (_: IOException) { }
        assertNull(store.load())
    }

    @Test fun `cannot switch company while signed in and no token goes to a new host`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp()
        try { CompanyAccount(store, CompanyApiClient(http)).connect("https://other.example"); fail() } catch (_: IllegalStateException) { }
        assertTrue(http.requests.isEmpty()); assertEquals(connection, store.connection())
    }

    @Test fun `current session revocation clears local state`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp(); http.responses += response()
        CompanyAccount(store, CompanyApiClient(http)).revoke(CompanyDeviceSession("original-secret", "", "", true))
        assertNull(store.load()); assertEquals("original-secret", JSONObject(http.requests.single().body!!).getString("token"))
    }

    @Test fun `enrollment saves only the newly rotated session after validation`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp()
        http.responses += response(headers = mapOf("set-auth-token" to listOf("new-secret")))
        http.identity()
        CompanyAccount(store, CompanyApiClient(http)).verifyEnrollment("123456")
        assertEquals("new-secret", store.load()!!.token)
        assertEquals("Bearer original-secret", http.requests[0].headers["Authorization"])
        assertEquals("Bearer new-secret", http.requests[1].headers["Authorization"])
    }

    @Test fun `backup code verification uses pending cookie and backup route`() {
        val http = FakeHttp(); http.responses += response(headers = mapOf("set-auth-token" to listOf("new-secret")))
        assertEquals("new-secret", CompanyApiClient(http).verifySignIn(connection, PendingCompanyMfa("mfa=pending"), "backup-code", true))
        assertTrue(http.requests.single().url.endsWith("/two-factor/verify-backup-code"))
    }

    @Test fun `invitation has no public registration or database path`() {
        val http = FakeHttp(); http.responses += response(status = 201)
        CompanyApiClient(http).acceptInvitation(connection, "invitation-token", "Demo Name", "new long password")
        assertTrue(http.requests.single().url.endsWith("/v1/invitations/accept"))
        assertFalse(http.requests.single().headers.containsKey("Authorization"))
    }

    @Test fun `session strings redact bearer tokens`() {
        val store = CompanyAccountStore(Values(), box)
        assertFalse(stored(store).toString().contains("original-secret"))
        assertFalse(PendingCompanyMfa("cookie-secret").toString().contains("cookie-secret"))
    }

    @Test fun `current device matches the raw server token in a signed bearer session`() {
        val http = FakeHttp()
        http.responses += response("""[{"token":"raw-session","createdAt":"today","expiresAt":"tomorrow"}]""")
        val original = stored(CompanyAccountStore(Values(), box))
        val signed = CompanySession(connection, "raw-session.signature", original.identity)
        assertTrue(CompanyApiClient(http).sessions(signed).single().current)
    }

    @Test fun `company projects use the scoped read API and bounded pagination`() {
        val http = FakeHttp()
        http.responses += response("""{"projects":[{"id":"p1","name":"Demo Villa","code":"DEMO-01","status":"active","version":2}],"nextCursor":"next-id"}""")
        val session = stored(CompanyAccountStore(Values(), box))
        val page = CompanyApiClient(http).projects(session)
        assertEquals("Demo Villa", page.projects.single().name)
        assertEquals("next-id", page.nextCursor)
        assertTrue(http.requests.single().url.endsWith("/v1/projects?limit=25"))
        assertEquals("Bearer original-secret", http.requests.single().headers["Authorization"])
    }

    @Test fun `project request cursor cannot inject a query or leak data to another URL`() {
        val http = FakeHttp(); http.responses += response("""{"projects":[],"nextCursor":null}""")
        CompanyApiClient(http).projects(stored(CompanyAccountStore(Values(), box)), "next&token=bad")
        assertTrue(http.requests.single().url.endsWith("after=next%26token%3Dbad"))
    }

    @Test fun `project access suspension clears the encrypted company session`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp()
        http.responses += response("""{"error":{"code":"MEMBERSHIP_SUSPENDED"}}""", 403)
        try { CompanyAccount(store, CompanyApiClient(http)).projects(); fail() } catch (_: CompanyApiException) { }
        assertNull(store.load())
    }

    @Test fun `sign out clears cached identity before contacting the server`() {
        val store = CompanyAccountStore(Values(), box); stored(store)
        val http = object : HttpTransport {
            override fun request(method: String, url: String, headers: Map<String,String>, body: String?): HttpResponse {
                assertNull(store.load())
                return response()
            }
        }
        CompanyAccount(store, CompanyApiClient(http)).signOut()
    }

    private val editableProject = CompanyProject("11111111-1111-4111-8111-111111111111", "Villa", "V-01", "active", 1)
    private fun editReply(edit: CompanyProjectEdit, conflict: Boolean = false): String {
        val current = JSONObject().put("id", edit.base.id).put("name", "Company villa").put("code", "V-01").put("status", "active").put("version", 2)
        val result = JSONObject().put("operationId", edit.operationId).put("outcome", if (conflict) "conflict" else "applied")
        return if (conflict) {
            result.put("expectedVersion", edit.base.version).put("submitted", JSONObject().put("name", edit.name.trim()).put("code", edit.code.trim()).put("status", edit.status)).put("current", current)
            JSONObject().put("error", JSONObject().put("code", "VERSION_CONFLICT")).put("conflict", result).toString()
        } else result.put("project", current).toString()
    }

    @Test fun `online project edit sends expected version and stable operation id to the company API`() {
        val http = FakeHttp(); val edit = CompanyProjectEdit(editableProject, name = " Company villa ")
        http.responses += response(editReply(edit))
        val saved = CompanyApiClient(http).editProject(stored(CompanyAccountStore(Values(), box)), edit) as CompanyProjectEditResult.Applied
        assertEquals(2, saved.project.version)
        val request = http.requests.single()
        assertEquals("PATCH", request.method)
        assertTrue(request.url.endsWith("/v1/projects/${editableProject.id}"))
        val body = JSONObject(request.body!!)
        assertEquals(edit.operationId, body.getString("operationId"))
        assertEquals(1, body.getInt("expectedVersion"))
        assertEquals("Company villa", body.getString("name"))
        assertFalse(body.has("organizationId"))
    }

    @Test fun `project conflict preserves company version for explicit review and keeps the session`() {
        val http = FakeHttp(); val store = CompanyAccountStore(Values(), box); stored(store)
        val edit = CompanyProjectEdit(editableProject, name = "My draft")
        http.responses += response(editReply(edit, true), 409)
        val result = CompanyAccount(store, CompanyApiClient(http)).editProject(edit) as CompanyProjectEditResult.Conflict
        assertEquals("Company villa", result.current.name)
        assertEquals("My draft", edit.name)
        assertNotNull(store.load())
    }

    @Test fun `network retry retains the exact project edit payload and operation id`() {
        val http = FakeHttp(); val edit = CompanyProjectEdit(editableProject)
        val api = CompanyApiClient(http); val session = stored(CompanyAccountStore(Values(), box))
        http.failure = IOException("interrupted")
        try { api.editProject(session, edit); fail() } catch (_: IOException) { }
        http.failure = null
        http.responses += response(editReply(edit))
        api.editProject(session, edit)
        assertEquals(2, http.requests.size)
        assertEquals(http.requests[0].body, http.requests[1].body)
    }

    @Test fun `edit response cannot substitute another operation or project`() {
        val edit = CompanyProjectEdit(editableProject)
        for (wrong in listOf(
            JSONObject(editReply(edit)).put("operationId", java.util.UUID.randomUUID().toString()),
            JSONObject(editReply(edit)).also { it.getJSONObject("project").put("id", java.util.UUID.randomUUID().toString()) }
        )) {
            val http = FakeHttp(); http.responses += response(wrong.toString())
            try { CompanyApiClient(http).editProject(stored(CompanyAccountStore(Values(), box)), edit); fail() }
            catch (e: CompanyApiException) { assertEquals("INVALID_RESPONSE", e.errorCode) }
        }
    }

    @Test fun `project edits reject invalid input before network traffic`() {
        val http = FakeHttp(); val api = CompanyApiClient(http); val session = stored(CompanyAccountStore(Values(), box))
        for (edit in listOf(CompanyProjectEdit(editableProject, name = " "), CompanyProjectEdit(editableProject, status = "unknown"),
            CompanyProjectEdit(editableProject.copy(id = "../secret")), CompanyProjectEdit(editableProject.copy(version = Int.MAX_VALUE)))) {
            try { api.editProject(session, edit); fail() } catch (e: CompanyApiException) { assertEquals("INVALID_PROJECT_EDIT", e.errorCode) }
        }
        assertTrue(http.requests.isEmpty())
    }

    @Test fun `project edit suspension clears the saved session`() {
        val http = FakeHttp(); val store = CompanyAccountStore(Values(), box); stored(store)
        http.responses += response("""{"error":{"code":"MEMBERSHIP_SUSPENDED"}}""", 403)
        try { CompanyAccount(store, CompanyApiClient(http)).editProject(CompanyProjectEdit(editableProject)); fail() } catch (_: CompanyApiException) { }
        assertNull(store.load())
    }

    @Test fun `project permission refusal clears visible identity until a new online check`() {
        val store = CompanyAccountStore(Values(), box); stored(store); val http = FakeHttp()
        http.responses += response("""{"error":{"code":"PERMISSION_DENIED"}}""", 403)
        try { CompanyAccount(store, CompanyApiClient(http)).projects(); fail() } catch (_: CompanyApiException) { }
        assertNull(store.load())
        assertEquals("original-secret", store.savedToken())
        assertEquals(1, http.requests.size)
    }
}
