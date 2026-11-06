package com.holagundi.archiman

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.room.Room
import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.ArchiManMigrations
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.portal.PortalServer
import com.holagundi.archiman.ui.ConsultancyPdf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object ConsultancyDeviceChecks {
    suspend fun run(context: Context, db: ArchiManDatabase) {
        val projectId = ProjectRepository(db, db.projectDao(), db.practiceDao()).createProject("Consultancy test", "COA-DEVICE", "Consultancy client", "Residential", "Test site", "Architect")
        val repository = ConsultancyRepository(db)
        val prepared = repository.newInput(projectId, false, ConsultancyService.LANDSCAPE).copy(
            rate = "35.5", quantity = "2000", feeBasis = ConsultancyFeeBasis.PER_SQ_FT,
            areaDefinition = "Landscape treatment area; excludes building footprint",
        )
        val id = repository.save(projectId, false, prepared)
        val original = repository.read(projectId, false, id)
        check(original.amount == 71000.0)
        check(original.input == prepared)
        val agreementId = repository.copy(original, true)
        val agreement = repository.read(projectId, true, agreementId)
        check(agreement.input!!.scope == prepared.scope && agreement.input!!.terms == prepared.terms && agreement.amount == original.amount)
        check(agreement.input!!.proposalReference.contains("#$id"))
        suspend fun rejects(block: suspend () -> Unit) {
            var rejected = false
            try { block() } catch (_: IllegalArgumentException) { rejected = true }
            check(rejected)
        }
        rejects { repository.issue(agreement, false) }
        rejects { repository.issue(agreement, true) }
        val resolved = agreement.input!!.let { it.copy(
            parties = it.parties.replace(Regex("\\[[^]]*]"), "Agreed party details"),
            scope = it.scope.replace(Regex("\\[[^]]*]"), "Assigned consultants recorded"),
            terms = it.terms.replace(Regex("\\[[^]]*]"), "Agreed project-specific particulars"),
        ) }
        repository.save(projectId, true, resolved, agreement)
        rejects { repository.save(projectId, true, resolved.copy(title = "Stale overwrite"), agreement) }
        val reviewed = repository.read(projectId, true, agreementId)
        repository.issue(reviewed, true)
        val issued = repository.read(projectId, true, agreementId)
        check(issued.status == "ISSUED" && issued.documentText.contains("71000.00"))
        rejects { repository.save(projectId, true, resolved.copy(rate = "50"), issued) }
        rejects { repository.delete(issued) }
        val copyId = repository.copy(issued, true)
        check(repository.read(projectId, true, copyId).status == "DRAFT")
        val pdf = File(context.filesDir, "consultancy-check.pdf")
        pdf.outputStream().use { ConsultancyPdf.write(it, issued.title, issued.documentText) }
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> check(renderer.pageCount >= 2) { "Full agreement was truncated during PDF export." } }
        }
        pdf.delete()
        val backups = BackupRepository(context, db, PortalServer(db))
        val export = ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
        val candidate = backups.prepare(ByteArrayInputStream(export))
        backups.restore(candidate)
        check(repository.read(projectId, true, agreementId).documentText == issued.documentText)
        check(repository.read(projectId, true, agreementId).detailsJson == issued.detailsJson)
    }

    suspend fun migration(target: Context, test: Context) {
        val name = "consultancy-migration-${UUID.randomUUID()}.db"
        val file = target.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = JSONObject(test.assets.open("com.holagundi.archiman.data.local.ArchiManDatabase/28.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { raw ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.getJSONArray("indices")
                for (i in 0 until indexes.length()) raw.execSQL(indexes.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))
            raw.execSQL("INSERT INTO projects (id, name, client, clientId, siteLocation, projectCode, projectType, status, description, architectInCharge, areaUnit, createdAt) VALUES (1, 'Migration project', 'Client', 0, 'Site', 'MIG', 'Residential', 'ACTIVE', '', '', 'm²', 1)")
            raw.execSQL("INSERT INTO fee_proposals (projectId, title, amount, status, createdAt) VALUES (1, 'Preserved fee', 12345, 'DRAFT', 1)")
            raw.execSQL("INSERT INTO work_contracts (projectId, title, party, kind, amount, status, createdAt) VALUES (1, 'Preserved contract', 'Client', 'Consultancy', 54321, 'DRAFT', 1)")
            raw.version = 28
        }
        val migrated = Room.databaseBuilder(target, ArchiManDatabase::class.java, name).addMigrations(*ArchiManMigrations.ALL).build()
        try {
            val sql = migrated.openHelper.writableDatabase
            check(sql.version == 31)
            sql.query("SELECT title, amount, detailsJson, documentText FROM fee_proposals").use { check(it.moveToFirst()); check(it.getString(0) == "Preserved fee" && it.getDouble(1) == 12345.0 && it.getString(2) == "" && it.getString(3) == "") }
            sql.query("SELECT amount, detailsJson FROM work_contracts").use { check(it.moveToFirst()); check(it.getDouble(0) == 54321.0 && it.getString(1) == "") }
        } finally { migrated.close(); target.deleteDatabase(name) }
    }

    suspend fun legacyBackup(context: Context, db: ArchiManDatabase) {
        val backups = BackupRepository(context, db, PortalServer(db))
        val current = ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
        val entries = BackupArchive.read(ByteArrayInputStream(current)).toMutableMap()
        val data = JSONObject(String(entries.getValue("database.json"), Charsets.UTF_8))
        listOf("fee_proposals", "work_contracts").forEach { name ->
            val table = data.getJSONObject("tables").getJSONObject(name)
            val columns = table.getJSONArray("columns")
            val keep = (0 until columns.length()).filter { columns.getString(it) !in listOf("detailsJson", "documentText") }
            val oldRows = table.getJSONArray("rows")
            val rows = JSONArray()
            for (row in 0 until oldRows.length()) rows.put(JSONArray().apply { keep.forEach { put(oldRows.getJSONArray(row).get(it)) } })
            table.put("columns", JSONArray(keep.map(columns::getString))).put("rows", rows)
        }
        data.getJSONObject("tables").remove("finance_records")
        mapOf("clients" to listOf("detailsJson"), "contractors" to listOf("detailsJson"), "measurements" to listOf("boqRevision","boqLineKey","rateSnapshot","contractBasis"), "measurement_sheets" to listOf("boqRevision","boqLineKey","rateSnapshot","contractBasis"), "invoices" to listOf("taxJson"), "receipts" to listOf("tdsAmount", "tdsReference", "tdsVerified", "bankKey")).forEach { (name, remove) ->
            val table = data.getJSONObject("tables").getJSONObject(name); val columns = table.getJSONArray("columns")
            val keep = (0 until columns.length()).filter { columns.getString(it) !in remove }; val original = table.getJSONArray("rows")
            val rows = JSONArray(); for (r in 0 until original.length()) rows.put(JSONArray().apply { keep.forEach { put(original.getJSONArray(r).get(it)) } })
            table.put("columns", JSONArray(keep.map(columns::getString))).put("rows", rows)
        }
        data.put("schemaVersion", 28).put("identity", "1993cd791fb24b7411375cb96c1f771f")
        entries["database.json"] = data.toString().toByteArray(Charsets.UTF_8)
        val legacy = ByteArrayOutputStream().also { BackupArchive.write(entries, it) }.toByteArray()
        val prepared = backups.prepare(ByteArrayInputStream(legacy))
        check(prepared.preview.upgradedFrom == 28)
        backups.restore(prepared)
        check(db.openHelper.writableDatabase.version == 31)
    }
}
