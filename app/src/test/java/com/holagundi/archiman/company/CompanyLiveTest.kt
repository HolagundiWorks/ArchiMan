package com.holagundi.archiman.company

import com.holagundi.archiman.sync.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.Base64

class CompanyLiveTest {
    @Test fun `live task pages are scoped validated and never persisted`() {
        val values = Values(); val store = seeded(values); val http = Http()
        val row = JSONObject().put("id","22222222-2222-4222-8222-222222222222").put("projectId",project.id)
            .put("title","Company task").put("description","Live only").put("dueAt",JSONObject.NULL)
            .put("status","open").put("priority","normal").put("version",1)
        val page = JSONObject().put("projectId",project.id).put("tasks",org.json.JSONArray().put(row)).put("nextCursor",JSONObject.NULL)
        http.handler = { response(body().put("taskPage",page)) }
        val before = values.data.toMap()
        val account = CompanyAccount(store,CompanyApiClient(http))
        val snapshot = account.liveProjects(null,null,project.id)
        assertEquals("Company task",snapshot.taskPage!!.tasks.single().title)
        assertEquals(before,values.data)
        assertTrue(http.urls.last().contains("taskProjectId="+project.id))
        row.put("projectId","33333333-3333-4333-8333-333333333333")
        try { account.liveProjects(null,null,project.id); fail() } catch (error: CompanyApiException) { assertEquals("INVALID_RESPONSE",error.errorCode) }
        row.put("projectId",project.id)
        page.put("tasks",org.json.JSONArray().also { rows -> repeat(26) { rows.put(row) } })
        try { account.liveProjects(null,null,project.id); fail() } catch (error: CompanyApiException) { assertEquals("INVALID_RESPONSE",error.errorCode) }
        assertEquals(before,values.data)
    }
    private val connection = CompanyConnection("https://company.example", "/api/auth")
    private val identity = CompanyIdentity("u-1", "demo@example.test", "Demo", "Company", listOf("Staff"), listOf("projects.read"), false, "org")
    private val project = CompanyProject("11111111-1111-4111-8111-111111111111", "Private villa", "LIVE-01", "active", 1)
    private val box = object : SecretBox {
        override fun seal(plain: ByteArray) = plain.map { (it.toInt() xor 0x53).toByte() }.toByteArray()
        override fun open(sealed: ByteArray) = seal(sealed)
    }
    private class Values : KeyValues {
        val data = mutableMapOf<String, String>()
        override fun get(key: String) = data[key]
        override fun put(key: String, value: String) { data[key] = value }
        override fun remove(key: String) { data.remove(key) }
    }
    private class Http : HttpTransport {
        val urls = mutableListOf<String>()
        var cancelled = false
        var handler: (String) -> HttpResponse = { throw IOException("offline") }
        override fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse { urls += url; return handler(url) }
        override fun cancel() { cancelled = true }
    }
    private fun seeded(values: Values): CompanyAccountStore = CompanyAccountStore(values, box).also {
        it.connect(connection); it.save(CompanySession(connection, "private-token", identity))
    }
    private fun body() = JSONObject().put("revision", "a".repeat(64)).put("projects", org.json.JSONArray().put(projectJson(project)))
        .put("nextCursor", JSONObject.NULL).put("roles", org.json.JSONArray(listOf("Staff"))).put("permissions", org.json.JSONArray(listOf("projects.read")))
    private fun response(body: JSONObject, code: Int = 200) = HttpResponse(code, emptyMap(), body.toString())

    @Test fun `credentials alone survive recreation and old snapshots are scrubbed without contacting the API`() {
        val values = Values(); seeded(values)
        val old = JSONObject().put("url", connection.url).put("token", "private-token").put("email", identity.email)
            .put("projectCache", JSONObject().put("name", project.name)).put("projectEdit", JSONObject().put("name", "draft"))
        values.data["session"] = Base64.getEncoder().encodeToString(box.seal(old.toString().toByteArray()))
        val restored = CompanyAccountStore(values, box)
        assertNull(restored.load()); assertEquals("private-token", restored.savedToken())
        val clean = JSONObject(String(box.open(Base64.getDecoder().decode(values.data.getValue("session")))))
        assertEquals(setOf("url", "token"), clean.keys().asSequence().toSet())
        assertFalse(clean.toString().contains(project.name))
    }

    @Test fun `live reads use a bounded revision and update memory without persisting project or identity fields`() {
        val values = Values(); val store = seeded(values); val http = Http()
        http.handler = { response(body()) }
        val before = values.data.getValue("session")
        val account = CompanyAccount(store, CompanyApiClient(http))
        val first = account.liveProjects(null, null)
        assertEquals(project, first.page.projects.single())
        assertEquals(before, values.data.getValue("session"))
        account.liveProjects(null, first.revision)
        assertTrue(http.urls.last().endsWith("?since=" + first.revision))
        assertNull(CompanyAccountStore(values, box).load())
    }

    @Test fun `retired business data from a formerly selected origin is also scrubbed`() {
        val values = Values(); seeded(values)
        val old = JSONObject().put("url", "https://old.example").put("token", "old-token").put("projectCache", JSONObject().put("name", project.name))
        values.data["session"] = Base64.getEncoder().encodeToString(box.seal(old.toString().toByteArray()))
        val restored = CompanyAccountStore(values, box)
        assertNull(restored.savedToken())
        val clean = JSONObject(String(box.open(Base64.getDecoder().decode(values.data.getValue("session")))))
        assertEquals(setOf("url", "token"), clean.keys().asSequence().toSet())
    }

    @Test fun `connection loss clears memory and never falls back to business data`() {
        val values = Values(); val store = seeded(values); val http = Http()
        val account = CompanyAccount(store, CompanyApiClient(http))
        try { account.liveProjects(null, null); fail() } catch (_: IOException) { }
        assertNull(account.session()); assertTrue(account.hasSavedSession())
        assertFalse(values.data.toString().contains(project.name))
    }

    @Test fun `suspension removes even the credential and recreated clients cannot restore company identity`() {
        val values = Values(); val store = seeded(values); val http = Http()
        http.handler = { response(JSONObject().put("error", JSONObject().put("code", "MEMBERSHIP_SUSPENDED")), 403) }
        val account = CompanyAccount(store, CompanyApiClient(http))
        try { account.liveProjects(null, null); fail() } catch (_: CompanyApiException) { }
        assertNull(account.session()); assertFalse(account.hasSavedSession())
        assertNull(CompanyAccountStore(values, box).savedToken())
    }

    @Test fun `cancelling an old live request cannot clear the active edit session`() {
        val values = Values(); val store = seeded(values); val http = Http()
        val account = CompanyAccount(store, CompanyApiClient(http))
        http.handler = { account.cancelLive(); throw IOException("closed") }
        try { account.liveProjects(null, null); fail() } catch (_: java.util.concurrent.CancellationException) { }
        assertTrue(http.cancelled); assertNotNull(account.session())
        assertEquals("private-token", store.savedToken())
    }

    @Test fun `a refused older token cannot erase a newly rotated session`() {
        val values = Values(); val store = seeded(values); val http = Http()
        val account = CompanyAccount(store, CompanyApiClient(http))
        http.handler = {
            store.save(CompanySession(connection, "rotated-token", identity))
            response(JSONObject().put("error", JSONObject().put("code", "UNAUTHENTICATED")), 401)
        }
        try { account.liveProjects(null, null); fail() } catch (_: CompanyApiException) { }
        assertEquals("rotated-token", store.savedToken()); assertNotNull(store.load())
    }

    @Test fun `revision injection and oversized live pages are refused`() {
        val values = Values(); val store = seeded(values); val http = Http(); val api = CompanyApiClient(http)
        try { api.liveProjects(store.load()!!, null, "bad&token=x"); fail() } catch (_: CompanyApiException) { }
        assertTrue(http.urls.isEmpty())
        http.handler = { response(body().put("projects", org.json.JSONArray().also { rows -> repeat(26) { rows.put(projectJson(project)) } })) }
        try { api.liveProjects(store.load()!!, null, null); fail() } catch (e: CompanyApiException) { assertEquals("INVALID_RESPONSE", e.errorCode) }
    }
}
