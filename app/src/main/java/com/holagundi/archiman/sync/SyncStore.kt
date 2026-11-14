package com.holagundi.archiman.sync

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** One change to send to the cloud. [v] stays on the phone and comes back in the acknowledgement. */
data class PushRecord(val kind: String, val syncId: String, val baseRev: Long, val updatedMs: Long, val deleted: Boolean, val body: JSONObject, val v: Long) {
    fun toCloudJson(): JSONObject = JSONObject()
        .put("kind", kind).put("sync_id", syncId).put("base_rev", baseRev)
        .put("updated_ms", updatedMs).put("deleted", deleted).put("body", body)
}

data class Ack(val kind: String, val syncId: String, val v: Long, val status: String, val rev: Long)

data class Remote(val kind: String, val syncId: String, val rev: Long, val seq: Long, val deleted: Boolean, val body: JSONObject)

data class PullResult(val applied: Int, val deferred: Int, val skippedLocalChanges: Int, val unsupported: Int, val nextSeq: Long)

data class SyncStatus(val enabled: Boolean, val cloudCompany: String?, val lastPulledSeq: Long, val pending: Int)

/**
 * The phone's side of the company sync: what changed here, what to push, what the cloud acknowledged, and how pulled records are applied.
 * A port of MynaHub's `office-core/src/sync.rs` (same rules, same record bodies), over the phone's own tables. It never touches the network.
 *
 * Not synced: a project's stages (the phone models stages as fee items, the office as percentages), tasks without a project (the phone
 * cannot hold them), a task's priority and position, and everything outside wave 1.
 */
class SyncStore(private val db: SqlDb, private val nowMs: () -> Long = System::currentTimeMillis) {
    private fun rank(kind: String) = SyncSchema.KINDS.indexOf(kind).let { if (it < 0) 99 else it }

    private fun table(kind: String) = when (kind) {
        SyncSchema.CLIENT -> "clients"
        SyncSchema.PROJECT -> "projects"
        SyncSchema.TASK -> "project_tasks"
        else -> error("Unknown sync kind $kind")
    }

    private fun state(key: String): String? = db.query("SELECT value FROM sync_state WHERE key = ?", key).firstOrNull()?.get("value") as String?
    private fun setState(key: String, value: String) =
        db.exec("INSERT INTO sync_state (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = excluded.value", key, value)

    /** Turns change tracking on for a cloud company. The first time, every existing row is queued for upload. */
    fun enable(cloudCompany: String) = db.transaction {
        require(cloudCompany.isNotBlank()) { "A cloud company id is required." }
        state("cloud_company")?.let { check(it == cloudCompany) { "This phone is already linked to a different cloud company." } }
        val first = state("enabled") == null
        setState("cloud_company", cloudCompany)
        setState("enabled", "1")
        if (first) for (kind in SyncSchema.KINDS) {
            db.exec(
                "INSERT OR IGNORE INTO sync_dirty (kind, local_id, deleted, v, changed_ms) " +
                    "SELECT ?, id, 0, (SELECT IFNULL(MAX(v), 0) FROM sync_dirty) + ROW_NUMBER() OVER (ORDER BY id), ? FROM ${table(kind)}",
                kind, nowMs(),
            )
        }
    }

    fun disable() = setState("enabled", "0")

    fun status() = SyncStatus(
        enabled = state("enabled") == "1",
        cloudCompany = state("cloud_company"),
        lastPulledSeq = state("last_pulled_seq")?.toLongOrNull() ?: 0L,
        pending = (db.query("SELECT COUNT(*) AS n FROM sync_dirty").first()["n"] as Long).toInt(),
    )

    /** What linking would upload, for the confirmation shown before the first sync. */
    fun localCounts(): Map<String, Int> = SyncSchema.KINDS.associateWith { (db.query("SELECT COUNT(*) AS n FROM ${table(it)}").first()["n"] as Long).toInt() }

    // ---- mapping -------------------------------------------------------------------------------------------------

    private fun mapGet(kind: String, localId: Long): Pair<String, Long>? =
        db.query("SELECT sync_id, cloud_rev FROM sync_map WHERE kind = ? AND local_id = ?", kind, localId).firstOrNull()?.let { it["sync_id"] as String to it["cloud_rev"] as Long }

    private fun mapEnsure(kind: String, localId: Long): Pair<String, Long> = mapGet(kind, localId) ?: run {
        val id = UUID.randomUUID().toString()
        db.exec("INSERT INTO sync_map (kind, local_id, sync_id) VALUES (?, ?, ?)", kind, localId, id)
        id to 0L
    }

    private fun localOf(kind: String, syncId: String): Long? =
        db.query("SELECT local_id FROM sync_map WHERE kind = ? AND sync_id = ?", kind, syncId).firstOrNull()?.get("local_id") as Long?

    private fun putMap(kind: String, localId: Long, syncId: String, rev: Long) {
        db.exec("DELETE FROM sync_map WHERE kind = ? AND local_id = ? AND sync_id <> ?", kind, localId, syncId)
        db.exec(
            "INSERT INTO sync_map (kind, local_id, sync_id, cloud_rev) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (kind, local_id) DO UPDATE SET sync_id = excluded.sync_id, cloud_rev = excluded.cloud_rev",
            kind, localId, syncId, rev,
        )
    }

    private fun isDirty(kind: String, localId: Long?) =
        localId != null && db.query("SELECT 1 AS x FROM sync_dirty WHERE kind = ? AND local_id = ?", kind, localId).isNotEmpty()

    // ---- bodies --------------------------------------------------------------------------------------------------

    private fun bodyOf(kind: String, id: Long): JSONObject? {
        val row = db.query("SELECT * FROM ${table(kind)} WHERE id = ?", id).firstOrNull() ?: return null
        fun s(k: String) = (row[k] as String?) ?: ""
        val created = stamp(row["createdAt"] as Long? ?: nowMs())
        return when (kind) {
            SyncSchema.CLIENT -> JSONObject().put("name", s("name")).put("contact_person", s("contactPerson")).put("email", s("email"))
                .put("phone", s("contactNo")).put("address", s("address")).put("archived", 0).put("created_at", created).put("updated_at", stamp(nowMs()))
            SyncSchema.PROJECT -> {
                val status = s("status")
                JSONObject().put("client_sync_id", mapEnsure(SyncSchema.CLIENT, row["clientId"] as Long).first).put("code", s("projectCode")).put("name", s("name"))
                    .put("site_address", s("siteLocation")).put("status", officeStatus(status)).put("archived", if (status == "ARCHIVED") 1 else 0)
                    .put("summary", s("description")).put("created_at", created).put("updated_at", stamp(nowMs()))
            }
            SyncSchema.TASK -> JSONObject().put("project_sync_id", mapEnsure(SyncSchema.PROJECT, row["projectId"] as Long).first).put("title", s("title"))
                .put("notes", s("description")).put("status", officeTaskStatus(s("status"))).put("position", 0)
                .put("due_date", (row["dueDate"] as Long?)?.let { day(it) } ?: "").put("created_at", created).put("updated_at", stamp(nowMs()))
            else -> null
        }
    }

    /** The next changes to push, parents before children, at most [limit]. */
    fun collectPush(limit: Int = 200): List<PushRecord> {
        val dirty = db.query("SELECT kind, local_id, deleted, v, changed_ms FROM sync_dirty")
            .sortedWith(compareBy({ rank(it["kind"] as String) }, { it["v"] as Long }))
        return dirty.take(limit).map { d ->
            val kind = d["kind"] as String
            val id = d["local_id"] as Long
            val (syncId, cloudRev) = mapEnsure(kind, id)
            val body = if ((d["deleted"] as Long) != 0L) null else bodyOf(kind, id)
            PushRecord(kind, syncId, cloudRev, d["changed_ms"] as Long, body == null, body ?: JSONObject(), d["v"] as Long)
        }
    }

    /**
     * Records the cloud's answer. created / updated / merged: remember the revision. rejected_older: the cloud's newer version arrives on the next
     * pull (ours is kept in the cloud conflict list). forbidden: stays queued. A row edited again since [collectPush] stays queued.
     */
    fun ack(items: List<Ack>): Pair<Int, Int> = db.transaction {
        var cleared = 0
        var kept = 0
        for (a in items) {
            val local = localOf(a.kind, a.syncId) ?: continue
            when (a.status) {
                "created", "updated", "merged" -> db.exec("UPDATE sync_map SET cloud_rev = ? WHERE kind = ? AND local_id = ?", a.rev, a.kind, local)
                "rejected_older" -> Unit
                else -> { kept++; continue }
            }
            val before = db.query("SELECT 1 AS x FROM sync_dirty WHERE kind = ? AND local_id = ? AND v = ?", a.kind, local, a.v).size
            db.exec("DELETE FROM sync_dirty WHERE kind = ? AND local_id = ? AND v = ?", a.kind, local, a.v)
            if (before == 0) kept++ else cleared++
        }
        cleared to kept
    }

    // ---- applying pulled records -------------------------------------------------------------------------------------

    private enum class Outcome { APPLIED, DEFERRED, SKIPPED_DIRTY, UNSUPPORTED }

    /**
     * Applies records pulled from the cloud. A record whose parent has not arrived is deferred and the cursor stops before it; a record this phone
     * has changed and not yet pushed is left alone (push first, the cloud decides).
     */
    fun applyPull(fromSeq: Long, records: List<Remote>): PullResult = db.transaction {
        setState("applying", "1")
        var applied = 0
        var deferred = 0
        var skipped = 0
        var unsupported = 0
        var maxSeq = fromSeq
        var floor: Long? = null
        for (r in records.sortedWith(compareBy({ rank(it.kind) }, { it.seq }))) {
            maxSeq = maxOf(maxSeq, r.seq)
            val outcome = try {
                applyOne(r)
            } catch (e: Exception) {
                Outcome.DEFERRED
            }
            when (outcome) {
                Outcome.APPLIED -> applied++
                Outcome.SKIPPED_DIRTY -> skipped++
                Outcome.UNSUPPORTED -> unsupported++
                Outcome.DEFERRED -> { deferred++; floor = minOf(floor ?: Long.MAX_VALUE, r.seq - 1) }
            }
        }
        val next = floor?.let { maxOf(it, fromSeq) } ?: maxSeq
        setState("last_pulled_seq", next.toString())
        setState("applying", "0")
        PullResult(applied, deferred, skipped, unsupported, next)
    }

    private fun unmapped(kind: String, sql: String, vararg args: Any?): Long? =
        db.query(sql, *args).firstOrNull()?.get("id")?.let { it as Long }?.takeIf { mapGet(kind, it) == null }

    private fun applyOne(r: Remote): Outcome {
        if (r.kind !in SyncSchema.KINDS) return Outcome.UNSUPPORTED
        val local = localOf(r.kind, r.syncId)
        if (isDirty(r.kind, local)) return Outcome.SKIPPED_DIRTY
        // A copy of what this phone itself sent, or one it already applied: nothing to do.
        if (local != null && (mapGet(r.kind, local)?.second ?: 0L) >= r.rev) return Outcome.APPLIED
        val b = r.body
        fun s(k: String) = b.optString(k, "")
        if (r.deleted) {
            if (local != null) {
                when (r.kind) {
                    SyncSchema.TASK -> db.exec("DELETE FROM project_tasks WHERE id = ?", local)
                    SyncSchema.PROJECT -> db.exec("UPDATE projects SET status = 'ARCHIVED' WHERE id = ?", local)
                    else -> Unit // a client archived or removed in the office stays here, the phone has no archive for clients
                }
                db.exec("UPDATE sync_map SET cloud_rev = ? WHERE kind = ? AND local_id = ?", r.rev, r.kind, local)
            }
            return Outcome.APPLIED
        }
        val id: Long = when (r.kind) {
            SyncSchema.CLIENT -> {
                val id = local ?: unmapped(SyncSchema.CLIENT, "SELECT id FROM clients WHERE name = ? COLLATE NOCASE", s("name"))
                if (id != null) {
                    db.exec(
                        "UPDATE clients SET name = ?, contactPerson = ?, contactNo = ?, email = ?, address = ? WHERE id = ?",
                        s("name"), s("contact_person"), s("phone"), s("email"), s("address"), id,
                    )
                    id
                } else {
                    db.exec(
                        "INSERT INTO clients (name, clientType, contactPerson, address, correspondenceAddress, contactNo, email, preferredCommunication, notes, createdAt, detailsJson) " +
                            "VALUES (?, 'Client', ?, ?, '', ?, ?, 'Email', '', ?, '')",
                        s("name"), s("contact_person"), s("address"), s("phone"), s("email"), parse(s("created_at")),
                    )
                    db.lastInsertId()
                }
            }
            SyncSchema.PROJECT -> {
                val client = localOf(SyncSchema.CLIENT, s("client_sync_id")) ?: return Outcome.DEFERRED
                val clientName = db.query("SELECT name FROM clients WHERE id = ?", client).first()["name"] as String
                val status = if (b.optInt("archived") == 1) "ARCHIVED" else phoneStatus(s("status"))
                val id = local ?: unmapped(SyncSchema.PROJECT, "SELECT id FROM projects WHERE clientId = ? AND name = ? COLLATE NOCASE", client, s("name"))
                if (id != null) {
                    db.exec(
                        "UPDATE projects SET client = ?, clientId = ?, projectCode = ?, name = ?, siteLocation = ?, status = ?, description = ? WHERE id = ?",
                        clientName, client, s("code"), s("name"), s("site_address"), status, s("summary"), id,
                    )
                    id
                } else {
                    db.exec(
                        "INSERT INTO projects (name, client, clientId, siteLocation, projectCode, projectType, status, description, architectInCharge, areaUnit, createdAt) " +
                            "VALUES (?, ?, ?, ?, ?, 'General', ?, ?, '', 'm²', ?)",
                        s("name"), clientName, client, s("site_address"), s("code"), status, s("summary"), parse(s("created_at")),
                    )
                    db.lastInsertId()
                }
            }
            else -> {
                // Tasks
                if (b.isNull("project_sync_id")) return Outcome.UNSUPPORTED // the phone cannot hold a task without a project
                val project = localOf(SyncSchema.PROJECT, b.getString("project_sync_id")) ?: return Outcome.DEFERRED
                val due = s("due_date")
                if (local != null) {
                    val current = db.query("SELECT dueDate FROM project_tasks WHERE id = ?", local).first()["dueDate"] as Long?
                    val keepDue = (current?.let { day(it) } ?: "") == due // keep the exact time of day when the date is the same
                    db.exec(
                        "UPDATE project_tasks SET projectId = ?, title = ?, description = ?, status = ?" + (if (keepDue) "" else ", dueDate = ?") + " WHERE id = ?",
                        *(listOf<Any?>(project, s("title"), s("notes"), phoneTaskStatus(s("status"))) + (if (keepDue) emptyList() else listOf(dayMs(due))) + local).toTypedArray(),
                    )
                    local
                } else {
                    db.exec(
                        "INSERT INTO project_tasks (projectId, title, description, dueDate, status, priority, createdAt) VALUES (?, ?, ?, ?, ?, 'NORMAL', ?)",
                        project, s("title"), s("notes"), dayMs(due), phoneTaskStatus(s("status")), parse(s("created_at")),
                    )
                    db.lastInsertId()
                }
            }
        }
        putMap(r.kind, id, r.syncId, r.rev)
        return Outcome.APPLIED
    }

    // ---- value mapping -------------------------------------------------------------------------------------------------

    private fun officeStatus(phone: String) = when (phone) { "ON_HOLD" -> "on_hold"; "COMPLETED" -> "completed"; else -> "active" }
    private fun phoneStatus(office: String) = when (office) { "on_hold" -> "ON_HOLD"; "completed" -> "COMPLETED"; else -> "ACTIVE" }
    private fun officeTaskStatus(phone: String) = when (phone) { "DOING" -> "doing"; "DONE" -> "done"; else -> "todo" }
    private fun phoneTaskStatus(office: String) = when (office) { "doing" -> "DOING"; "done" -> "DONE"; else -> "TO_DO" }

    companion object {
        private fun fmt(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        /** "yyyy-MM-dd HH:mm:ss" in UTC, the form the office stores (SQLite datetime). */
        fun stamp(ms: Long): String = fmt("yyyy-MM-dd HH:mm:ss").format(Date(ms))
        fun day(ms: Long): String = fmt("yyyy-MM-dd").format(Date(ms))
        fun dayMs(day: String): Long? = if (day.length == 10) runCatching { fmt("yyyy-MM-dd").parse(day)!!.time }.getOrNull() else null
        fun parse(text: String): Long = runCatching { fmt("yyyy-MM-dd HH:mm:ss").parse(text.replace('T', ' ').take(19))!!.time }.getOrElse { System.currentTimeMillis() }

        fun remotesFrom(rows: JSONArray): List<Remote> = (0 until rows.length()).map { i ->
            val o = rows.getJSONObject(i)
            Remote(o.getString("kind"), o.getString("sync_id"), o.getLong("rev"), o.getLong("seq"), o.optBoolean("deleted"), o.optJSONObject("body") ?: JSONObject())
        }
    }
}
