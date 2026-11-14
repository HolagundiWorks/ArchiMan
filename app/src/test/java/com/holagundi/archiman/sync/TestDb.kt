package com.holagundi.archiman.sync

import org.json.JSONObject
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** [SqlDb] over JDBC SQLite, so the real sync SQL (triggers included) runs on the JVM. Tables use the DDL Room exported for the app. */
class JdbcSqlDb : SqlDb, AutoCloseable {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    private var depth = 0

    init {
        conn.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        val schema = File("schemas/com.holagundi.archiman.data.local.ArchiManDatabase/34.json").readText()
        val entities = JSONObject(schema).getJSONObject("database").getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            if (e.getString("tableName") !in setOf("clients", "projects", "project_tasks")) continue
            conn.createStatement().use { it.execute(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName"))) }
            val idx = e.getJSONArray("indices")
            for (j in 0 until idx.length()) conn.createStatement().use { it.execute(idx.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName"))) }
        }
        SyncSchema.ensure(this)
    }

    private fun normal(v: Any?): Any? = when (v) {
        is Int -> v.toLong()
        is Short -> v.toLong()
        is Byte -> v.toLong()
        is Float -> v.toDouble()
        else -> v
    }

    override fun exec(sql: String, vararg args: Any?) {
        conn.prepareStatement(sql).use { st ->
            args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
            st.execute()
        }
    }

    override fun query(sql: String, vararg args: Any?): List<Map<String, Any?>> =
        conn.prepareStatement(sql).use { st ->
            args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
            st.executeQuery().use { rs ->
                val md = rs.metaData
                val rows = ArrayList<Map<String, Any?>>()
                while (rs.next()) rows += (1..md.columnCount).associate { md.getColumnLabel(it) to normal(rs.getObject(it)) }
                rows
            }
        }

    override fun lastInsertId(): Long = query("SELECT last_insert_rowid() AS id").first()["id"] as Long

    override fun <T> transaction(block: () -> T): T {
        if (depth == 0) conn.autoCommit = false
        depth++
        try {
            val r = block()
            depth--
            if (depth == 0) { conn.commit(); conn.autoCommit = true }
            return r
        } catch (e: Throwable) {
            depth--
            if (depth == 0) { conn.rollback(); conn.autoCommit = true }
            throw e
        }
    }

    override fun close() = conn.close()

    // ---- fixtures ----
    fun client(name: String, phone: String = ""): Long {
        exec(
            "INSERT INTO clients (name, clientType, contactPerson, address, correspondenceAddress, contactNo, email, preferredCommunication, notes, createdAt, detailsJson) VALUES (?, 'Client', '', 'Pune', '', ?, '', 'Email', 'private note', 1700000000000, '')",
            name, phone,
        )
        return lastInsertId()
    }

    fun project(client: Long, name: String, status: String = "ACTIVE"): Long {
        val clientName = query("SELECT name FROM clients WHERE id = ?", client).first()["name"]
        exec(
            "INSERT INTO projects (name, client, clientId, siteLocation, projectCode, projectType, status, description, architectInCharge, areaUnit, createdAt) VALUES (?, ?, ?, 'Baner', 'P-1', 'Residential', ?, 'Villa', 'Ar. X', 'm²', 1700000000000)",
            name, clientName, client, status,
        )
        return lastInsertId()
    }

    fun task(project: Long, title: String, status: String = "TO_DO", due: Long? = null): Long {
        exec("INSERT INTO project_tasks (projectId, title, description, dueDate, status, priority, createdAt) VALUES (?, ?, '', ?, ?, 'HIGH', 1700000000000)", project, title, due, status)
        return lastInsertId()
    }

    fun count(table: String) = (query("SELECT COUNT(*) AS n FROM $table").first()["n"] as Long).toInt()
    fun dirty() = count("sync_dirty")
    fun value(sql: String, vararg args: Any?): Any? = query(sql, *args).firstOrNull()?.values?.firstOrNull()
}
