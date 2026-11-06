package com.holagundi.archiman.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.domain.SchemaContract
import com.holagundi.archiman.portal.PortalServer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val practice: String,
    val createdAt: Long,
    val projects: Int,
    val records: Int,
    val attachments: Int,
    val missingAttachments: Int,
    val upgradedFrom: Int? = null,
)

class PreparedBackup internal constructor(internal val entries: Map<String, ByteArray>, val preview: BackupPreview)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ArchiManDatabase,
    private val portal: PortalServer,
) {
    private class AttachmentTooLarge : IllegalArgumentException("Attachment exceeds the backup size limit.")
    private val mutex = Mutex()
    private val attachmentColumns = setOf("logoUri", "photoUri", "evidenceUri", "afterEvidenceUri", "fileUri")
    private val recoveryFile get() = File(context.noBackupFilesDir, "recovery/latest.archimanbackup")
    private val legacyRecoveryFile get() = File(context.filesDir, "recovery/latest.archimanbackup")

    suspend fun export(output: OutputStream) = mutex.withLock {
        val entries = db.withTransaction { snapshot(db.openHelper.writableDatabase) }
        BackupArchive.write(entries, output)
    }

    suspend fun exportRecovery(output: OutputStream) = mutex.withLock {
        require(recoveryFile.isFile) { "No recovery copy is available yet." }
        val bytes = recoveryFile.inputStream().use { com.holagundi.archiman.domain.PasswordEnvelope.readBounded(it,BackupArchive.safeByteLimit()+1024*1024) }
        val plain = if (RecoveryCipher.protected(bytes)) RecoveryCipher.decrypt(bytes) else bytes
        try { output.write(plain) } finally { plain.fill(0) }
    }

    suspend fun protectLegacyRecovery() = mutex.withLock {
        if (legacyRecoveryFile.isFile && !recoveryFile.isFile) {
            legacyRecoveryFile.parentFile?.mkdirs()
            recoveryFile.parentFile?.mkdirs()
            runCatching {
                java.nio.file.Files.move(
                    legacyRecoveryFile.toPath(),
                    recoveryFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                )
            }.onFailure {
                legacyRecoveryFile.copyTo(recoveryFile, overwrite = true)
                legacyRecoveryFile.delete()
            }
        }
        if (!recoveryFile.isFile) return@withLock
        val bytes = recoveryFile.inputStream().use { com.holagundi.archiman.domain.PasswordEnvelope.readBounded(it,BackupArchive.safeByteLimit()+1024*1024) }
        if (RecoveryCipher.protected(bytes)) return@withLock
        try { writeRecovery(bytes) } finally { bytes.fill(0) }
    }

    private fun writeRecovery(plain: ByteArray) {
        check(recoveryFile.parentFile!!.mkdirs() || recoveryFile.parentFile!!.isDirectory)
        val temporary = File(recoveryFile.parentFile,"pending-${UUID.randomUUID()}")
        try {
            val encrypted = RecoveryCipher.encrypt(plain)
            java.io.FileOutputStream(temporary).use { it.write(encrypted); it.fd.sync() }
            java.nio.file.Files.move(temporary.toPath(),recoveryFile.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } finally { temporary.delete() }
    }

    suspend fun prepare(input: InputStream): PreparedBackup = mutex.withLock {
        val entries = BackupArchive.read(input).toMutableMap()
        val data = document(entries)
        val originalVersion = data.getInt("schemaVersion")
        val upgradedFrom = when {
            originalVersion in 28..30 -> {
                val expected = mapOf(28 to "1993cd791fb24b7411375cb96c1f771f", 29 to "d2a0df4c049eb978bdb19e4f03ee393c", 30 to "98cf1de9066b8d872610427572ad64dc").getValue(originalVersion)
                require(data.getString("identity") == expected) { "Unknown legacy backup identity." }
                val tables = data.getJSONObject("tables")
                fun append(name: String, additions: Map<String, Any>) {
                    val table = tables.getJSONObject(name); val columns = table.getJSONArray("columns")
                    require(strings(columns).none { it in additions.keys }) { "Invalid legacy backup columns." }
                    additions.keys.forEach(columns::put)
                    val rows = table.getJSONArray("rows")
                    for (index in 0 until rows.length()) additions.values.forEach { rows.getJSONArray(index).put(it) }
                }
                if (originalVersion == 28) listOf("fee_proposals", "work_contracts").forEach { append(it, linkedMapOf("detailsJson" to "", "documentText" to "")) }
                if (originalVersion < 30) {
                    append("invoices", mapOf("taxJson" to ""))
                    append("receipts", linkedMapOf("tdsAmount" to 0.0, "tdsReference" to "", "tdsVerified" to 0, "bankKey" to ""))
                    require(!tables.has("finance_records")) { "Invalid legacy finance table." }
                    tables.put("finance_records", JSONObject().put("columns", JSONArray(listOf("key", "kind", "json"))).put("rows", JSONArray()))
                }
                listOf("clients","contractors").forEach { append(it,mapOf("detailsJson" to "")) }
                listOf("measurement_sheets","measurements").forEach { append(it,linkedMapOf("boqRevision" to "", "boqLineKey" to "", "rateSnapshot" to 0.0, "contractBasis" to "")) }
                data.put("schemaVersion", SchemaContract.VERSION).put("identity", identity(db.openHelper.writableDatabase))
                entries["database.json"] = data.toString().toByteArray(Charsets.UTF_8)
                originalVersion
            }
            // Schema 32 only adds indexes; row shapes match schema 31.
            originalVersion == 31 -> {
                require(data.getString("identity") == "f10be1fd4ecc259d1daebcf7cc86b9d8") { "Unknown schema 31 backup identity." }
                data.put("schemaVersion", SchemaContract.VERSION).put("identity", identity(db.openHelper.writableDatabase))
                entries["database.json"] = data.toString().toByteArray(Charsets.UTF_8)
                31
            }
            else -> null
        }
        validate(data, entries, db.openHelper.writableDatabase)
        val tables = data.getJSONObject("tables")
        val company = tables.optJSONObject("company_profile")?.getJSONArray("rows")
        val columns = tables.optJSONObject("company_profile")?.getJSONArray("columns")
        val practiceIndex = columns?.let { (0 until it.length()).firstOrNull { index -> it.getString(index) == "practiceName" } }
        PreparedBackup(entries, BackupPreview(
            practice = if (company != null && company.length() > 0 && practiceIndex != null) company.getJSONArray(0).getString(practiceIndex) else "Practice backup",
            createdAt = data.getLong("createdAt"),
            projects = tables.getJSONObject("projects").getJSONArray("rows").length(),
            records = tables.keys().asSequence().sumOf { tables.getJSONObject(it).getJSONArray("rows").length() },
            attachments = data.getJSONObject("attachments").length(),
            missingAttachments = data.getJSONArray("missingAttachments").length(),
            upgradedFrom = upgradedFrom,
        ))
    }

    suspend fun restore(prepared: PreparedBackup) = mutex.withLock {
        val data = document(prepared.entries)
        val sql = db.openHelper.writableDatabase
        validate(data, prepared.entries, sql)
        // Invalidate sessions before acquiring the replacement transaction. Queued portal
        // writes recheck this state inside their own transaction and cannot target restored IDs.
        portal.stop(notify = true)
        // New files use a unique directory. Existing files are never overwritten.
        val directory = File(context.filesDir, "restored-attachments/${UUID.randomUUID()}")
        val replacements = mutableMapOf<String, String>()
        try {
            val attachments = data.getJSONObject("attachments")
            attachments.keys().forEach { original ->
                val entry = attachments.getString(original)
                val file = File(directory, entry.substringAfterLast('/'))
                check(directory.mkdirs() || directory.isDirectory)
                file.writeBytes(prepared.entries.getValue(entry))
                replacements[original] = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
            }
            db.withTransaction {
                // Persist a complete recovery package before changing any row.
                val recovery = snapshot(sql)
                val recoveryBytes = java.io.ByteArrayOutputStream().also { BackupArchive.write(recovery,it) }.toByteArray()
                try { writeRecovery(recoveryBytes) } finally { recoveryBytes.fill(0) }
                sql.execSQL("PRAGMA defer_foreign_keys = ON")
                val tables = data.getJSONObject("tables")
                val names = tableNames(sql)
                names.forEach { sql.execSQL("DELETE FROM ${quote(it)}") }
                names.forEach { name ->
                    val table = tables.getJSONObject(name)
                    val columns = strings(table.getJSONArray("columns"))
                    val rows = table.getJSONArray("rows")
                    val insert = "INSERT INTO ${quote(name)} (${columns.joinToString(",", transform = ::quote)}) VALUES (${columns.joinToString(",") { "?" }})"
                    for (index in 0 until rows.length()) {
                        val row = rows.getJSONArray(index)
                        val values = columns.mapIndexed { columnIndex, column ->
                            val value = decode(row.get(columnIndex))
                            if (column in attachmentColumns && value is String) replacements[value] ?: value else value
                        }.toTypedArray()
                        sql.execSQL(insert, values)
                    }
                }
                sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Backup has broken record relationships." } }
                // Cascades must not silently remove restored rows.
                names.forEach { name ->
                    sql.query("SELECT COUNT(*) FROM ${quote(name)}").use {
                        it.moveToFirst()
                        require(it.getInt(0) == tables.getJSONObject(name).getJSONArray("rows").length()) { "Backup record count mismatch." }
                    }
                }
            }
        } catch (error: Throwable) {
            directory.deleteRecursively()
            throw error
        }
        // Close browser sessions that referred to the previous practice.
        portal.stop(notify = true)
        db.invalidationTracker.refreshVersionsAsync()
    }

    private fun snapshot(sql: SupportSQLiteDatabase): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        val attachments = JSONObject()
        val missing = linkedSetOf<String>()
        var attachmentBytes = 0L
        val tables = JSONObject()
        tableNames(sql).forEach { name ->
            sql.query("SELECT * FROM ${quote(name)}").use { cursor ->
                val columns = cursor.columnNames.toList()
                val rows = JSONArray()
                while (cursor.moveToNext()) {
                    val row = JSONArray()
                    columns.forEachIndexed { index, column ->
                        val value: Any = when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                            Cursor.FIELD_TYPE_BLOB -> JSONObject().put("blob", Base64.getEncoder().encodeToString(cursor.getBlob(index)))
                            else -> cursor.getString(index)
                        }
                        row.put(value)
                        if (column in attachmentColumns && value is String && value.isNotBlank() && !attachments.has(value) && value !in missing) {
                            val bytes = readAttachment(value)
                            if (bytes == null) missing.add(value) else {
                                val entry = "attachments/${BackupArchive.digest(bytes)}.bin"
                                if (entry !in entries) attachmentBytes += bytes.size
                                require(attachmentBytes <= BackupArchive.safeByteLimit()) { "Attachments exceed this device's safe backup size." }
                                entries[entry] = bytes
                                attachments.put(value, entry)
                            }
                        }
                    }
                    rows.put(row)
                }
                tables.put(name, JSONObject().put("columns", JSONArray(columns)).put("rows", rows))
            }
        }
        val data = JSONObject().put("schemaVersion", SchemaContract.VERSION).put("identity", identity(sql))
            .put("createdAt", System.currentTimeMillis()).put("tables", tables)
            .put("attachments", attachments).put("missingAttachments", JSONArray(missing.toList()))
        entries["database.json"] = data.toString().toByteArray(Charsets.UTF_8)
        return entries
    }

    private fun readAttachment(value: String): ByteArray? {
        val uri = Uri.parse(value)
        if (uri.scheme !in listOf("content", "file")) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                val bytes = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    if (bytes.size().toLong() + count > BackupArchive.MAX_BYTES) throw AttachmentTooLarge()
                    bytes.write(buffer, 0, count)
                }
                bytes.toByteArray()
            }
        } catch (error: AttachmentTooLarge) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    private fun validate(data: JSONObject, entries: Map<String, ByteArray>, sql: SupportSQLiteDatabase) {
        require(data.getInt("schemaVersion") == SchemaContract.VERSION && data.getString("identity") == identity(sql)) { "This backup requires a different app schema." }
        val tables = data.getJSONObject("tables")
        val names = tableNames(sql)
        require(tables.keys().asSequence().toSet() == names.toSet()) { "Backup table set is incomplete." }
        names.forEach { name ->
            val table = tables.getJSONObject(name)
            val columns = sql.query("SELECT * FROM ${quote(name)} LIMIT 0").use { it.columnNames.toList() }
            require(strings(table.getJSONArray("columns")) == columns) { "Backup columns do not match." }
            val rows = table.getJSONArray("rows")
            for (index in 0 until rows.length()) {
                val row = rows.getJSONArray(index)
                require(row.length() == columns.size) { "Backup row is incomplete." }
                for (cell in 0 until row.length()) decode(row.get(cell))
                if(name=="finance_records") {
                    fun value(key:String)=row.getString(columns.indexOf(key))
                    FinanceRecordValidation.validate(com.holagundi.archiman.data.local.entity.FinanceRecordEntity(value("key"),value("kind"),value("json")))
                }
                columns.forEachIndexed { cell,column -> if(column in listOf("detailsJson","taxJson") && !row.isNull(cell) && row.getString(cell).isNotBlank()) {
                    com.holagundi.archiman.domain.JsonSafety.check(row.getString(cell))
                    val json=JSONObject(row.getString(cell))
                    if(column=="taxJson") com.holagundi.archiman.domain.GstMode.valueOf(json.getString("mode"))
                } }
            }
        }
        val attachments = data.getJSONObject("attachments")
        attachments.keys().forEach { require(attachments.getString(it) in entries && attachments.getString(it).startsWith("attachments/")) { "Attachment is missing." } }
        data.getJSONArray("missingAttachments")
        data.getLong("createdAt")
    }

    private fun decode(value: Any): Any? = when (value) {
        JSONObject.NULL -> null
        is String, is Long, is Int, is Double -> value
        is JSONObject -> Base64.getDecoder().decode(value.getString("blob"))
        else -> throw IllegalArgumentException("Unsupported backup value.")
    }

    private fun document(entries: Map<String, ByteArray>): JSONObject {
        val text = String(entries.getValue("database.json"), Charsets.UTF_8)
        com.holagundi.archiman.domain.JsonSafety.check(text)
        return JSONObject(text)
    }
    private fun strings(array: JSONArray) = (0 until array.length()).map(array::getString)
    private fun quote(name: String) = "\"${name.replace("\"", "\"\"")}\""
    private fun identity(sql: SupportSQLiteDatabase) = sql.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use { it.moveToFirst(); it.getString(0) }
    private fun tableNames(sql: SupportSQLiteDatabase): List<String> = sql.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY name").use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
    }
}
