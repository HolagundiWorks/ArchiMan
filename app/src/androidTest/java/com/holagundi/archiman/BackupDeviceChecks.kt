package com.holagundi.archiman

import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.os.Bundle
import androidx.room.Room
import com.holagundi.archiman.data.BackupArchive
import com.holagundi.archiman.data.BackupRepository
import com.holagundi.archiman.data.DemoCompanyStore
import com.holagundi.archiman.data.SuiteRepository
import com.holagundi.archiman.data.MeasurementRepository
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.portal.PortalServer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/** Device checks use an isolated database and directory, never the user's practice. */
class BackupDeviceChecks : Instrumentation() {
    private var suite: String = "backup"

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        suite = arguments?.getString("suite").orEmpty().ifBlank { "backup" }
        start()
    }

    override fun onStart() {
        when (suite) {
            "screenWalk" -> { ScreenWalkRunner.run(this); return }
            "inputPrompts" -> { InputPromptSuite.run(this); return }
        }
        val results = Bundle()
        val directory = File(targetContext.filesDir, "restored-attachments/device-check-${UUID.randomUUID()}")
        directory.mkdirs()
        val isolated = object : ContextWrapper(targetContext) {
            override fun getFilesDir(): File = directory
            override fun getNoBackupFilesDir(): File = File(directory, "no-backup").apply { mkdirs() }
        }
        val db = Room.inMemoryDatabaseBuilder(targetContext, ArchiManDatabase::class.java).build()
        try {
            runBlocking {
                DemoCompanyStore(isolated, db).openBuiltIn()
                val demoStore = DemoCompanyStore(isolated, db)
                val demoProjects = db.projectDao().projects()
                check(demoProjects.size == 2)
                check(demoProjects.sumOf { db.catalogDao().observeContractors(it.id).first().size } == 8)
                val schedules = com.holagundi.archiman.data.ProjectScheduleRepository(db)
                val activeDemo = demoProjects.map { requireNotNull(schedules.active(it.id)) }
                check(activeDemo.all { it.label == "R001" } && activeDemo.sumOf { it.lines.size } == 14)
                check(activeDemo.flatMap { it.lines }.all { it.rate.toBigDecimal().signum() > 0 })
                check(activeDemo.flatMap { it.lines }.map { it.basis }.toSet() == setOf("LABOUR_ONLY", "WITH_MATERIAL"))
                demoStore.ensureDemoContractors(); demoStore.ensureDemoContractors()
                check(demoProjects.sumOf { schedules.revisions(it.id).size } == 2)
                check(demoProjects.sumOf { db.catalogDao().observeContractors(it.id).first().size } == 8)
                results.putString("demoContractors", "passed: 8 contractors, 14 rate-bearing assignments, active R001 per demo project, both contract bases, idempotent upgrade")
                val backups = BackupRepository(isolated, db, PortalServer(db, isolated))
                val sql = db.openHelper.writableDatabase
                val projectId = sql.query("SELECT id FROM projects LIMIT 1").use { check(it.moveToFirst()); it.getLong(0) }
                val attachment = File(directory, "evidence.txt").apply { writeText("offline evidence") }
                sql.execSQL("UPDATE company_profile SET logoUri = ?", arrayOf(attachment.toURI().toString()))
                val original = ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
                val candidate = backups.prepare(ByteArrayInputStream(original))
                check(candidate.preview.projects > 0 && candidate.preview.attachments >= 1)
                sql.execSQL("UPDATE projects SET name = 'Device check changed name' WHERE id = ?", arrayOf(projectId))
                backups.restore(candidate)
                val restoredName = sql.query("SELECT name FROM projects WHERE id = $projectId").use { it.moveToFirst(); it.getString(0) }
                check(restoredName != "Device check changed name")
                val photo = sql.query("SELECT logoUri FROM company_profile LIMIT 1").use { it.moveToFirst(); it.getString(0) }
                check(targetContext.contentResolver.openInputStream(android.net.Uri.parse(photo))!!.bufferedReader().use { it.readText() } == "offline evidence")
                results.putString("roundTrip", "passed: demo records, relationships and attachment bytes restored")

                val recoveryBytes = ByteArrayOutputStream().also { backups.exportRecovery(it) }.toByteArray()
                val recovery = backups.prepare(ByteArrayInputStream(recoveryBytes))
                backups.restore(recovery)
                check(sql.query("SELECT name FROM projects WHERE id = $projectId").use { it.moveToFirst(); it.getString(0) } == "Device check changed name")
                results.putString("recovery", "passed: pre-restore practice can be recovered")

                // Valid ZIP/checksums but broken foreign keys must roll back all writes.
                val entries = BackupArchive.read(ByteArrayInputStream(original)).toMutableMap()
                val document = JSONObject(String(entries.getValue("database.json"), Charsets.UTF_8))
                val table = document.getJSONObject("tables").getJSONObject("floors")
                val columns = table.getJSONArray("columns")
                val index = (0 until columns.length()).first { columns.getString(it) == "projectId" }
                table.getJSONArray("rows").getJSONArray(0).put(index, Long.MAX_VALUE)
                entries["database.json"] = document.toString().toByteArray(Charsets.UTF_8)
                val broken = ByteArrayOutputStream().also { BackupArchive.write(entries, it) }.toByteArray()
                val invalid = backups.prepare(ByteArrayInputStream(broken))
                var rejected = false
                try { backups.restore(invalid) } catch (_: Exception) { rejected = true }
                check(rejected)
                check(sql.query("SELECT name FROM projects WHERE id = $projectId").use { it.moveToFirst(); it.getString(0) } == "Device check changed name")
                sql.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) }
                results.putString("rollback", "passed: invalid relationships leave existing records intact")

                // Compare every row in every table after an unmodified round trip.
                backups.restore(candidate)
                val after = ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
                val beforeTables = JSONObject(String(BackupArchive.read(ByteArrayInputStream(original)).getValue("database.json"))).getJSONObject("tables")
                val afterTables = JSONObject(String(BackupArchive.read(ByteArrayInputStream(after)).getValue("database.json"))).getJSONObject("tables")
                beforeTables.getJSONObject("company_profile").getJSONArray("rows").getJSONArray(0).apply {
                    val cols = beforeTables.getJSONObject("company_profile").getJSONArray("columns")
                    val logo = (0 until cols.length()).first { cols.getString(it) == "logoUri" }
                    put(logo, afterTables.getJSONObject("company_profile").getJSONArray("rows").getJSONArray(0).get(logo))
                }
                check(beforeTables.toString() == afterTables.toString()) { "Restored rows differ from exported rows." }
                results.putString("allTables", "passed: every exported table round-trips")
                ConsultancyDeviceChecks.legacyBackup(isolated, db)
                results.putString("legacyBackup", "passed: schema-28 backup upgrades on restore")

                val suite = SuiteRepository(db, db.suiteDao(), db.practiceDao(), db.projectDao(), db.catalogDao(), db.measurementDao(), MeasurementRepository(db, db.measurementDao(), db.catalogDao(), db.projectDao()))
                UxDeviceChecks.run(isolated, db, suite, projectId)
                results.putString("ux", "passed: encrypted bounded durable drafts, tamper rejection, invalid-form rejection, committed-snapshot undo and stale-undo protection")
                val invoiceId = db.suiteDao().insertInvoice(InvoiceEntity(projectId = projectId, stageId = null, number = "DEVICE-CHECK", title = "Partial receipt check", amount = 100.0, status = "DRAFT", createdAt = System.currentTimeMillis()))
                val invoice = db.suiteDao().invoice(invoiceId)!!
                suite.issueInvoice(invoiceId)
                suite.recordReceipt(projectId, invoice, 40.0, "Partial payment")
                check(db.suiteDao().invoice(invoiceId)!!.status == "ISSUED")
                var overpaymentRejected = false
                try { suite.recordReceipt(projectId, invoice, 61.0, "Too much") } catch (_: IllegalArgumentException) { overpaymentRejected = true }
                check(overpaymentRejected && db.suiteDao().invoiceReceipts(invoiceId).size == 1)
                suite.recordReceipt(projectId, invoice, 60.0, "Balance")
                check(db.suiteDao().invoice(invoiceId)!!.status == "PAID")
                var duplicateRejected = false
                try { suite.recordReceipt(projectId, invoice, 60.0, "Stale repeat") } catch (_: IllegalArgumentException) { duplicateRejected = true }
                check(duplicateRejected && db.suiteDao().invoiceReceipts(invoiceId).size == 2)
                results.putString("billing", "passed: partial receipt, excess rejection, settlement and stale repeat")
                DashboardDeviceChecks.run(db, suite)
                results.putString("dashboard", "passed: pending counts, completed-task exclusion, due-task ordering and archived-project exclusion")
                RecordDeviceChecks.run(db, suite)
                results.putString("recordOperations", "passed: editor schema, project edit/archive/delete, client links, task CRUD, dependencies, draft protections, receipt reversal, measurement locks and cascading cleanup")
                ConsultancyDeviceChecks.run(isolated, db)
                ConsultancyDeviceChecks.migration(targetContext, context)
                FinanceDeviceChecks.run(isolated, db, suite)
                WorkspaceDeviceChecks.run(targetContext, db)
                AuditDeviceChecks.run(isolated, db, suite, results)
                results.putString("workspace", "passed: revision activation, stale rate rejection, contractor filtering, unit-converted import/deduplication, bill double-count prevention, letterhead PDF/XLSX/CSV exports")
                results.putString("finance", "passed: single admin, GST split, composition, TDS settlement/verification, bank duplicate rejection, reversal/rematch and backup")
                results.putString("consultancy", "passed: fee bases, proposal-to-agreement copy, review/placeholder checks, immutable issued snapshots, PDF pagination, backup round-trip and preserving migration")
            }
            results.putString("stream", "\nBackup, billing, dashboard, record operations, consultancy documents, PDF, GST, TDS, bank reconciliation, single admin, contractor schedule revisions, unit imports, running bills, letterhead exports, portal access, migration, failed-import rollback, concurrent mutations, malformed requests, connection limits and 10,000-row audit checks passed using isolated data.\n" + results.getString("auditMetrics").orEmpty() + "\n")
            finish(Activity.RESULT_OK, results)
        } catch (error: Throwable) {
            results.putString("stream", "\nBackup device checks failed: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, results)
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }
}
