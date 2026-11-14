package com.holagundi.archiman.sync

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The cloud's push rules (hub.push_records in MynaHub cloud/sql/0001_hub_sync.sql), in memory. */
class FakeCloud(private val forbidden: Boolean = false) : HubApi {
    class Row(var rev: Long, var seq: Long, var deleted: Boolean, var updatedMs: Long, var body: JSONObject, val kind: String)

    val rows = LinkedHashMap<String, Row>()
    val conflicts = ArrayList<JSONObject>()
    var seq = 0L
    var pushCalls = 0

    override fun companies() = listOf(CloudCompany("co-1", "Studio"))

    override fun push(company: String, records: List<PushRecord>): JSONArray {
        pushCalls++
        val out = JSONArray()
        for (r in records) {
            if (forbidden) { out.put(answer(r, "forbidden", 0, 0)); continue }
            seq++
            val cur = rows[r.syncId]
            if (cur == null) {
                rows[r.syncId] = Row(1, seq, r.deleted, r.updatedMs, r.body, r.kind)
                out.put(answer(r, "created", 1, seq))
            } else if (r.baseRev == cur.rev || r.updatedMs > cur.updatedMs) {
                if (r.baseRev != cur.rev) conflicts += JSONObject().put("sync_id", r.syncId)
                val status = if (r.baseRev != cur.rev) "merged" else "updated"
                cur.rev++; cur.seq = seq; cur.deleted = r.deleted; cur.updatedMs = r.updatedMs; cur.body = r.body
                out.put(answer(r, status, cur.rev, seq))
            } else {
                conflicts += JSONObject().put("sync_id", r.syncId)
                out.put(answer(r, "rejected_older", cur.rev, cur.seq))
            }
        }
        return out
    }

    private fun answer(r: PushRecord, status: String, rev: Long, seq: Long) = JSONObject().put("sync_id", r.syncId).put("kind", r.kind).put("status", status).put("rev", rev).put("seq", seq)

    override fun pull(company: String, fromSeq: Long, limit: Int): JSONArray {
        val a = JSONArray()
        rows.entries.filter { it.value.seq > fromSeq }.sortedBy { it.value.seq }.take(limit).forEach { (id, r) ->
            a.put(JSONObject().put("kind", r.kind).put("sync_id", id).put("rev", r.rev).put("seq", r.seq).put("deleted", r.deleted).put("body", r.body))
        }
        return a
    }
}

class SyncEngineTest {
    private class Phone(clock: () -> Long) : AutoCloseable {
        val db = JdbcSqlDb()
        val store = SyncStore(db, clock)
        override fun close() = db.close()
    }

    @Test fun two_phones_converge_through_the_cloud() {
        var now = 1_700_000_000_000L
        Phone { now }.use { a -> Phone { now }.use { b ->
            val cloud = FakeCloud()
            val c = a.db.client("Acme", "111")
            val p = a.db.project(c, "Tower")
            val t = a.db.task(p, "Site visit")
            a.store.enable("co-1")
            val ra = SyncEngine(a.store, cloud).runOnce()
            assertEquals(3, ra.pushed)
            assertTrue(ra.complete)

            b.store.enable("co-1")
            val rb = SyncEngine(b.store, cloud).runOnce()
            assertEquals(3, rb.pulled)
            assertEquals("Acme", b.db.value("SELECT name FROM clients"))
            assertEquals("Tower", b.db.value("SELECT name FROM projects"))

            // an edit on B arrives on A
            now += 5_000
            b.db.exec("UPDATE project_tasks SET status = 'DOING'")
            SyncEngine(b.store, cloud).runOnce()
            SyncEngine(a.store, cloud).runOnce()
            assertEquals("DOING", a.db.value("SELECT status FROM project_tasks WHERE id = ?", t))

            // a delete on A reaches B
            now += 5_000
            a.db.exec("DELETE FROM project_tasks WHERE id = ?", t)
            SyncEngine(a.store, cloud).runOnce()
            SyncEngine(b.store, cloud).runOnce()
            assertEquals(0, b.db.count("project_tasks"))

            // both edit the same client before either syncs: the later edit wins on both
            now += 5_000
            a.db.exec("UPDATE clients SET contactNo = '111-a'")
            now += 5_000
            b.db.exec("UPDATE clients SET contactNo = '222-b'")
            SyncEngine(b.store, cloud).runOnce()
            SyncEngine(a.store, cloud).runOnce()
            SyncEngine(b.store, cloud).runOnce()
            assertEquals("222-b", a.db.value("SELECT contactNo FROM clients"))
            assertEquals("222-b", b.db.value("SELECT contactNo FROM clients"))
            assertTrue("the older edit is kept, not dropped", cloud.conflicts.isNotEmpty())

            val quiet = SyncEngine(a.store, cloud).runOnce()
            assertEquals(0, quiet.pushed)
            assertEquals(0, a.db.dirty())
        } }
    }

    @Test fun a_forbidden_push_stays_queued_and_the_pass_is_incomplete() {
        Phone { 1L }.use { a ->
            a.db.client("Acme")
            a.store.enable("co-1")
            val r = SyncEngine(a.store, FakeCloud(forbidden = true)).runOnce()
            assertEquals(1, r.forbidden)
            assertFalse(r.complete)
            assertEquals(1, a.db.dirty())
        }
    }

    @Test fun an_unlinked_phone_is_refused() {
        Phone { 1L }.use { a ->
            val e = org.junit.Assert.assertThrows(IllegalStateException::class.java) { SyncEngine(a.store, FakeCloud()).runOnce() }
            assertTrue(e.message!!.contains("not set up"))
        }
    }
}
