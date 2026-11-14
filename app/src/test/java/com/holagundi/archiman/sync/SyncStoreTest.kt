package com.holagundi.archiman.sync

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStoreTest {
    private val db = JdbcSqlDb()
    private val store = SyncStore(db) { 1_700_000_100_000L }

    @After fun close() = db.close()

    private fun remote(kind: String, id: String, seq: Long, body: JSONObject, rev: Long = 1, deleted: Boolean = false) = Remote(kind, id, rev, seq, deleted, body)
    private fun ackAll(sent: List<PushRecord>, status: String = "created", rev: Long = 1) = store.ack(sent.map { Ack(it.kind, it.syncId, it.v, status, rev) })

    @Test fun nothing_is_tracked_until_sync_is_enabled_then_existing_rows_are_queued() {
        val c = db.client("Acme")
        assertEquals(0, db.dirty())
        store.enable("co-1")
        assertEquals(1, db.dirty())
        db.project(c, "Tower")
        assertEquals(2, db.dirty())
        assertThrows(IllegalStateException::class.java) { store.enable("other") }
        store.enable("co-1")
        assertEquals(2, db.dirty())
    }

    @Test fun a_push_has_parents_first_global_ids_and_only_wave_one_fields() {
        val c = db.client("Acme", phone = "555")
        val p = db.project(c, "Tower")
        db.task(p, "Site visit", "DOING", due = 1_767_225_600_000L)
        store.enable("co-1")
        val out = store.collectPush()
        assertEquals(listOf("client", "project", "task"), out.map { it.kind })
        assertEquals("555", out[0].body.getString("phone"))
        assertEquals(out[0].syncId, out[1].body.getString("client_sync_id"))
        assertEquals(out[1].syncId, out[2].body.getString("project_sync_id"))
        assertEquals("doing", out[2].body.getString("status"))
        assertEquals("2026-01-01", out[2].body.getString("due_date"))
        assertEquals(36, out[0].syncId.length)
        val text = out.joinToString { it.toCloudJson().toString() }
        assertFalse("private client notes must not leave the phone", text.contains("private note"))
        assertFalse("priority stays on the phone", text.contains("HIGH"))
    }

    @Test fun an_acknowledgement_never_clears_a_newer_edit() {
        val c = db.client("Acme")
        store.enable("co-1")
        val first = store.collectPush()
        db.exec("UPDATE clients SET contactNo = '999' WHERE id = ?", c) // edited while the push was in flight
        val (cleared, kept) = ackAll(first)
        assertEquals(0, cleared)
        assertEquals(1, kept)
        val again = store.collectPush()
        assertEquals(1L, again[0].baseRev)
        assertEquals("999", again[0].body.getString("phone"))
    }

    @Test fun forbidden_stays_queued_and_rejected_older_is_cleared() {
        db.client("A"); db.client("B")
        store.enable("co-1")
        val out = store.collectPush()
        store.ack(listOf(Ack("client", out[0].syncId, out[0].v, "forbidden", 0), Ack("client", out[1].syncId, out[1].v, "rejected_older", 3)))
        assertEquals(1, db.dirty())
    }

    @Test fun pulling_builds_a_company_on_an_empty_phone_without_echoing_it_back() {
        store.enable("co-1")
        val recs = listOf(
            remote("task", "t1", 3, JSONObject("""{"project_sync_id":"p1","title":"Site visit","status":"doing","due_date":"2026-01-01","notes":"n","created_at":"2026-01-01 00:00:00"}""")),
            remote("project", "p1", 2, JSONObject("""{"client_sync_id":"c1","name":"Tower","code":"P-9","site_address":"Baner","status":"on_hold","archived":0,"summary":"S","created_at":"2026-01-01 00:00:00"}""")),
            remote("client", "c1", 1, JSONObject("""{"name":"Acme","phone":"555","email":"a@example.test","created_at":"2026-01-01 00:00:00"}""")),
        )
        val res = store.applyPull(0, recs)
        assertEquals(3, res.applied)
        assertEquals(3L, res.nextSeq)
        assertEquals("555", db.value("SELECT contactNo FROM clients"))
        assertEquals("ON_HOLD", db.value("SELECT status FROM projects"))
        assertEquals("Acme", db.value("SELECT client FROM projects"))
        assertEquals("DOING", db.value("SELECT status FROM project_tasks"))
        assertEquals(1_767_225_600_000L, db.value("SELECT dueDate FROM project_tasks"))
        assertEquals(0, db.dirty())
        assertEquals(3L, store.status().lastPulledSeq)
    }

    @Test fun a_remote_client_with_the_same_name_is_adopted_not_duplicated() {
        val local = db.client("Acme")
        store.enable("co-1")
        db.exec("DELETE FROM sync_dirty")
        store.applyPull(0, listOf(remote("client", "c9", 1, JSONObject("""{"name":"ACME","phone":"999"}"""))))
        assertEquals(1, db.count("clients"))
        assertEquals("999", db.value("SELECT contactNo FROM clients WHERE id = ?", local))
    }

    @Test fun a_child_before_its_parent_is_deferred_and_holds_the_cursor() {
        val res = store.applyPull(10, listOf(remote("task", "t1", 12, JSONObject("""{"project_sync_id":"missing","title":"x","status":"todo"}"""))))
        assertEquals(1, res.deferred)
        assertEquals(11L, res.nextSeq)
    }

    @Test fun a_local_unpushed_edit_is_not_overwritten_by_a_pull() {
        val c = db.client("Acme")
        store.enable("co-1")
        val sid = store.collectPush().first().syncId
        val res = store.applyPull(0, listOf(remote("client", sid, 5, JSONObject("""{"name":"Acme","phone":"from-cloud"}"""))))
        assertEquals(1, res.skippedLocalChanges)
        assertEquals("", db.value("SELECT contactNo FROM clients WHERE id = ?", c))
    }

    @Test fun a_copy_of_what_the_phone_sent_is_not_applied_again() {
        val c = db.client("Acme", "111")
        store.enable("co-1")
        val sent = store.collectPush()
        ackAll(sent, rev = 4)
        db.exec("UPDATE clients SET contactNo = '222' WHERE id = ?", c)
        store.ack(emptyList())
        db.exec("DELETE FROM sync_dirty") // pretend the 222 edit was pushed and is at rev 4 already
        val res = store.applyPull(0, listOf(remote("client", sent[0].syncId, 5, JSONObject("""{"name":"Acme","phone":"111"}"""), rev = 4)))
        assertEquals(1, res.applied)
        assertEquals("222", db.value("SELECT contactNo FROM clients WHERE id = ?", c))
    }

    @Test fun tombstones_flow_both_ways_and_a_project_delete_tombstones_its_tasks() {
        val c = db.client("Acme")
        val p = db.project(c, "Tower")
        val t = db.task(p, "T")
        store.enable("co-1")
        ackAll(store.collectPush())
        assertEquals(0, db.dirty())

        db.exec("DELETE FROM project_tasks WHERE id = ?", t)
        val del = store.collectPush()
        assertEquals(1, del.size)
        assertTrue(del[0].deleted)
        db.exec("DELETE FROM sync_dirty")

        val psid = store.collectPush().firstOrNull()?.syncId // none queued; read the mapping through a fresh edit instead
        assertNull(psid)
        db.exec("UPDATE projects SET description = 'x' WHERE id = ?", p)
        val projectSid = store.collectPush().first().syncId
        db.exec("DELETE FROM sync_dirty")
        store.applyPull(0, listOf(remote("project", projectSid, 9, JSONObject(), rev = 5, deleted = true)))
        assertEquals("ARCHIVED", db.value("SELECT status FROM projects WHERE id = ?", p))
    }

    @Test fun deleting_a_project_on_the_phone_queues_tombstones_for_its_tasks() {
        val c = db.client("Acme")
        val p = db.project(c, "Tower")
        db.task(p, "T")
        store.enable("co-1")
        ackAll(store.collectPush())
        db.exec("DELETE FROM projects WHERE id = ?", p)
        val out = store.collectPush()
        assertEquals(listOf("project", "task"), out.map { it.kind })
        assertTrue(out.all { it.deleted })
    }

    @Test fun a_task_without_a_project_is_not_held_and_does_not_hold_the_cursor() {
        val res = store.applyPull(0, listOf(remote("task", "t1", 3, JSONObject("""{"title":"Pay vendor","status":"todo"}"""))))
        assertEquals(1, res.unsupported)
        assertEquals(0, res.deferred)
        assertEquals(3L, res.nextSeq)
        assertEquals(0, db.count("project_tasks"))
    }

    @Test fun the_exact_due_time_is_kept_when_only_the_date_matters() {
        val c = db.client("Acme")
        val p = db.project(c, "Tower")
        val due = 1_767_225_600_000L + 9 * 3_600_000L // 09:00 on 2026-01-01
        val t = db.task(p, "T", due = due)
        store.enable("co-1")
        val sent = store.collectPush()
        ackAll(sent)
        val taskSid = sent.first { it.kind == "task" }.syncId
        store.applyPull(0, listOf(remote("task", taskSid, 7, JSONObject("""{"project_sync_id":"${sent.first { it.kind == "project" }.syncId}","title":"T2","status":"done","due_date":"2026-01-01"}"""), rev = 2)))
        assertEquals(due, db.value("SELECT dueDate FROM project_tasks WHERE id = ?", t))
        assertEquals("T2", db.value("SELECT title FROM project_tasks WHERE id = ?", t))
        assertEquals("HIGH", db.value("SELECT priority FROM project_tasks WHERE id = ?", t))
    }

    @Test fun status_is_none_when_tracking_was_never_enabled() {
        val st = store.status()
        assertFalse(st.enabled)
        assertNull(st.cloudCompany)
        assertNotNull(st)
    }
}
