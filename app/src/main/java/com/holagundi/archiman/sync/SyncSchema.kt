package com.holagundi.archiman.sync

/**
 * Change tracking for the company sync (the same design as MynaHub's office-core migration 0038): three plain tables and triggers on the
 * synced tables. Everything is IF NOT EXISTS, so [ensure] is safe on every open, on a fresh install and after a restore. No existing table changes.
 * Wave 1 on the phone: clients, projects and project tasks (docs/hub-sync-design.md in the MynaHub repository).
 */
object SyncSchema {
    const val CLIENT = "client"
    const val PROJECT = "project"
    const val TASK = "task"
    val KINDS = listOf(CLIENT, PROJECT, TASK)

    private const val ON =
        "(SELECT value FROM sync_state WHERE key = 'enabled') = '1' AND IFNULL((SELECT value FROM sync_state WHERE key = 'applying'), '0') = '0'"

    private fun upsertDirty(kind: String, idRef: String, deleted: Int) =
        "INSERT INTO sync_dirty (kind, local_id, deleted, v, changed_ms) VALUES ('$kind', $idRef, $deleted, " +
            "(SELECT IFNULL(MAX(v), 0) + 1 FROM sync_dirty), CAST(strftime('%s', 'now') AS INTEGER) * 1000) " +
            "ON CONFLICT (kind, local_id) DO UPDATE SET deleted = $deleted, v = excluded.v, changed_ms = excluded.changed_ms;"

    private fun triggers(kind: String, table: String, short: String) = listOf(
        "CREATE TRIGGER IF NOT EXISTS sync_${short}_ai AFTER INSERT ON `$table` WHEN $ON BEGIN ${upsertDirty(kind, "NEW.id", 0)} END",
        "CREATE TRIGGER IF NOT EXISTS sync_${short}_au AFTER UPDATE ON `$table` WHEN $ON BEGIN ${upsertDirty(kind, "NEW.id", 0)} END",
        "CREATE TRIGGER IF NOT EXISTS sync_${short}_ad AFTER DELETE ON `$table` WHEN $ON BEGIN ${upsertDirty(kind, "OLD.id", 1)} END",
    )

    val STATEMENTS: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS sync_map (kind TEXT NOT NULL, local_id INTEGER NOT NULL, sync_id TEXT NOT NULL UNIQUE, cloud_rev INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (kind, local_id))",
        "CREATE TABLE IF NOT EXISTS sync_dirty (kind TEXT NOT NULL, local_id INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, v INTEGER NOT NULL, changed_ms INTEGER NOT NULL, PRIMARY KEY (kind, local_id))",
        "CREATE TABLE IF NOT EXISTS sync_state (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)",
    ) + triggers(CLIENT, "clients", "clients") + triggers(PROJECT, "projects", "projects") + triggers(TASK, "project_tasks", "tasks")

    /** Creates the tracking tables and triggers when missing. Call after the synced tables exist. */
    fun ensure(db: SqlDb) = STATEMENTS.forEach { db.exec(it) }
}
