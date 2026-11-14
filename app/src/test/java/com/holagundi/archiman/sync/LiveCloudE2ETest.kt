package com.holagundi.archiman.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manual end-to-end step against a real Neon DEV branch, run between the two desktop steps of MynaHub's Adms.SyncCheck (e2e1 and e2e3).
 * Does nothing unless MYNA_E2E_COMPANY, MYNA_E2E_AUTH_URL, MYNA_E2E_API_URL, MYNA_E2E_EMAIL and MYNA_E2E_PASSWORD are all set, so a pass without
 * them proves nothing. Use a disposable account on the dev branch only; never production.
 */
class LiveCloudE2ETest {
    private fun env(name: String) = System.getenv(name).orEmpty()

    @Test fun the_phone_joins_a_desktop_company_receives_edits_and_sends_back() {
        val company = env("MYNA_E2E_COMPANY")
        val authUrl = env("MYNA_E2E_AUTH_URL")
        val apiUrl = env("MYNA_E2E_API_URL")
        val email = env("MYNA_E2E_EMAIL")
        val password = env("MYNA_E2E_PASSWORD")
        if (listOf(company, authUrl, apiUrl, email, password).any { it.isEmpty() }) return

        val http = UrlConnectionTransport()
        val auth = CloudAuth(authUrl, http)
        val session = auth.signIn(email, password)
        val api = HubDataApi(apiUrl, http) { auth.accessToken(session) }

        assertTrue("the owner's company is visible to the added member", api.companies().any { it.id == company })

        JdbcSqlDb().use { db ->
            val store = SyncStore(db)
            store.enable(company)
            val first = SyncEngine(store, api).runOnce()
            assertTrue("first pass complete", first.complete)
            assertEquals("E2E Acme", db.value("SELECT name FROM clients"))
            assertEquals("E2E Tower", db.value("SELECT name FROM projects"))
            assertEquals(2, db.count("project_tasks"))
            assertEquals("111", db.value("SELECT contactNo FROM clients"))
            val due = db.value("SELECT dueDate FROM project_tasks WHERE title = 'E2E desktop task'") as Long
            assertEquals("2026-03-01", SyncStore.day(due))

            db.exec("UPDATE project_tasks SET status = 'DONE' WHERE title = 'E2E to edit'")
            val projectId = db.value("SELECT id FROM projects") as Long
            db.task(projectId, "E2E phone task")
            db.exec("UPDATE clients SET contactNo = '777'")
            val second = SyncEngine(store, api).runOnce()
            assertTrue("second pass complete", second.complete)
            assertTrue("sent the three changes", second.pushed + second.merged >= 3)
            assertEquals(0, db.dirty())
            println("LIVE E2E OK: received 4 records, sent ${second.pushed + second.merged} changes")
        }
        auth.signOut(session)
    }
}
