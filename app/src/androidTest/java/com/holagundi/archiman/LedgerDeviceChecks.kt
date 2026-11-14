package com.holagundi.archiman

import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.os.Bundle
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.holagundi.archiman.data.DemoCompanyStore
import com.holagundi.archiman.data.LedgerChart
import com.holagundi.archiman.data.LedgerRepository
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.ArchiManMigrations
import com.holagundi.archiman.data.local.entity.LedgerPeriodEntity
import com.holagundi.archiman.domain.LedgerLine
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking

/**
 * Ledger checks on the built-in demo company in an isolated in-memory database.
 * They never touch the practice on the phone. Run with:
 * am instrument ... -e suite ledger
 */
object LedgerDeviceChecks {
    fun run(instrumentation: Instrumentation) {
        val results = Bundle()
        try {
            val context = instrumentation.targetContext
            val directory = File(context.filesDir, "ledger-check-${UUID.randomUUID()}").apply { mkdirs() }
            val isolated = object : ContextWrapper(context) {
                override fun getFilesDir(): File = directory
                override fun getNoBackupFilesDir(): File = File(directory, "no-backup").apply { mkdirs() }
                override fun getDatabasePath(name: String): File = File(directory, name)
            }
            val db = Room.inMemoryDatabaseBuilder(context, ArchiManDatabase::class.java).build()
            runBlocking {
                DemoCompanyStore(isolated, db).openBuiltIn()
                LedgerChart.ensure(db)
                val ledger = LedgerRepository(db)

                val accounts = ledger.accounts()
                check(accounts.size == 35) { "chart has ${accounts.size} accounts, expected 35" }
                val bank = accounts.first { it.code == "1010" }.id
                val fees = accounts.first { it.code == "4000" }.id
                val tds = accounts.first { it.code == "1210" }.id
                results.putString("chart", "passed: 35 standard accounts seeded, idempotent on reopen")

                val fee = ledger.post(null, "2026-11-10", "Device check fee", listOf(LedgerLine(bank, 1000_00, 0), LedgerLine(fees, 0, 1000_00)))
                val tb = ledger.trialBalance("2026-11-10")
                check(tb.balanced && tb.debitPaise == 1000_00L) { "trial balance not balanced after posting" }
                check(tb.rows.first { it.code == "4000" }.creditPaise == 1000_00L)
                results.putString("post", "passed: balanced journal posted and shown in the trial balance")

                // Partial TDS receipt: bank 900, TDS receivable 100, fee 1000 (the receipt posts both debits).
                ledger.post(null, "2026-11-11", "Receipt with TDS", listOf(LedgerLine(bank, 900_00, 0), LedgerLine(tds, 100_00, 0), LedgerLine(fees, 0, 1000_00)))
                val afterReceipt = ledger.trialBalance("2026-11-30")
                check(afterReceipt.balanced)
                check(afterReceipt.rows.first { it.code == "1210" }.debitPaise == 100_00L)
                results.putString("tds", "passed: receipt with TDS keeps the trial balance equal")

                val asOfBefore = ledger.trialBalance("2026-11-10")
                check(asOfBefore.rows.first { it.code == "4000" }.creditPaise == 1000_00L) { "as-of date included a later journal" }
                results.putString("asOf", "passed: trial balance by date excludes later journals")

                val countBefore = ledger.journals("2026-01-01", "2026-12-31").size
                rejects { ledger.post(null, "2026-11-10", "Bad", listOf(LedgerLine(bank, 1_00, 0), LedgerLine(fees, 0, 2_00))) }
                rejects { ledger.post(null, "2026-11-10", "Unknown account", listOf(LedgerLine(9999, 1_00, 0), LedgerLine(fees, 0, 1_00))) }
                rejects { ledger.post(null, "2026-11-10", "", listOf(LedgerLine(bank, 1_00, 0), LedgerLine(fees, 0, 1_00))) }
                rejects { ledger.post(null, "10/11/2026", "Bad date", listOf(LedgerLine(bank, 1_00, 0), LedgerLine(fees, 0, 1_00))) }
                check(ledger.journals("2026-01-01", "2026-12-31").size == countBefore) { "a rejected journal was written" }
                results.putString("rules", "passed: unbalanced, unknown account, blank narration and bad date are rejected and write nothing")

                ledger.post(null, "2026-11-12", "Once", listOf(LedgerLine(bank, 5_00, 0), LedgerLine(fees, 0, 5_00)), sourceType = "invoice", sourceId = 77)
                rejects { ledger.post(null, "2026-11-12", "Twice", listOf(LedgerLine(bank, 5_00, 0), LedgerLine(fees, 0, 5_00)), sourceType = "invoice", sourceId = 77) }
                results.putString("once", "passed: a business event posts at most once")

                db.ledgerDao().insertPeriodIgnore(LedgerPeriodEntity(fy = "2024-25", startDate = "2024-04-01", endDate = "2025-03-31", status = "closed"))
                rejects { ledger.post(null, "2024-10-01", "Closed period", listOf(LedgerLine(bank, 1_00, 0), LedgerLine(fees, 0, 1_00))) }
                results.putString("closed", "passed: a closed financial year rejects new journals")

                ledger.reverse(null, fee, "Entered in error")
                val afterReversal = ledger.trialBalance("2026-12-31")
                check(afterReversal.balanced)
                // Fees account: credits fee 1000 + receipt 1000 + event 5; the reversal debits the fee's 1000 again.
                val fees2 = afterReversal.rows.first { it.code == "4000" }
                check(fees2.creditPaise == 2005_00L && fees2.debitPaise == 1000_00L) { "reversal did not cancel the fee" }
                rejects { ledger.reverse(null, fee, "Again") }
                results.putString("reverse", "passed: reversal cancels the journal, a journal reverses once, totals stay equal")

                // Table definitions created by the migration must match the Room entities.
                val migrated = migratedPlainDatabase(isolated)
                for (table in listOf("ledger_accounts", "ledger_journals", "ledger_journal_lines", "ledger_periods")) {
                    val expected = schemaOf(db.openHelper.writableDatabase, table)
                    val actual = schemaOf(migrated, table)
                    check(expected == actual) { "migration schema for $table differs from the entity:\n$expected\n---\n$actual" }
                }
                // Triggers only exist after the migration; they enforce immutability and closed periods.
                migrated.execSQL("INSERT INTO ledger_journals (number, date, narration, sourceType, sourceId, createdBy, createdAt) VALUES ('JV-T-1', '2026-01-05', 't', 'manual', NULL, NULL, 0)")
                rejectsSql { migrated.execSQL("UPDATE ledger_journals SET narration = 'changed'") }
                rejectsSql { migrated.execSQL("DELETE FROM ledger_journals") }
                migrated.close()
                schema33Restore(isolated, results)
                results.putString("migration", "passed: migration tables match entities; immutability triggers reject change and delete")
            }
            results.putString("stream", "\nLedger checks PASSED\n" + listOf("chart", "post", "tds", "asOf", "rules", "once", "closed", "reverse", "migration", "schema33").joinToString("\n") { results.getString(it).orEmpty() } + "\n")
            instrumentation.finish(Activity.RESULT_OK, results)
        } catch (error: Throwable) {
            results.putString("stream", "\nLedger checks FAILED: ${error.message}\n")
            instrumentation.finish(Activity.RESULT_CANCELED, results)
        }
    }

    private suspend fun rejects(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        } catch (_: IllegalStateException) {
            return
        }
        throw IllegalStateException("expected the ledger to reject this entry")
    }

    private fun rejectsSql(block: () -> Unit) {
        try {
            block()
        } catch (_: android.database.sqlite.SQLiteException) {
            return
        }
        throw IllegalStateException("expected the trigger to reject this statement")
    }

    private fun migratedPlainDatabase(context: android.content.Context): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("ledger-migration-check.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(33) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val db = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
        ArchiManMigrations.MIGRATION_33_34.migrate(db)
        return db
    }

    private fun schemaOf(db: SupportSQLiteDatabase, table: String): String {
        val columns = db.query("PRAGMA table_info(`$table`)").use { c ->
            buildList { while (c.moveToNext()) add(listOf(c.getString(1), c.getString(2), c.getInt(3), c.getInt(5)).joinToString("|")) }
        }
        val indexes = db.query("PRAGMA index_list(`$table`)").use { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getString(1), c.getInt(2), c.getString(3))) }
        }.sortedBy { it.first }
        val indexLines = indexes.map { (name, unique, origin) ->
            val cols = db.query("PRAGMA index_info(`$name`)").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(2)) }
            }
            "index $name unique=$unique cols=${cols.joinToString(",")}"
        }
        return (columns + indexLines).joinToString("\n")
    }

    /** A schema 33 backup (no ledger tables) restores into schema 34 with the standard chart. */
    private suspend fun schema33Restore(context: android.content.Context, results: Bundle) {
        val db = Room.inMemoryDatabaseBuilder(context, ArchiManDatabase::class.java).build()
        DemoCompanyStore(context, db).openBuiltIn()
        val backups = com.holagundi.archiman.data.BackupRepository(context, db, com.holagundi.archiman.portal.PortalServer(db, context))
        val projectsBefore = db.projectDao().projects().size
        val exported = java.io.ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
        val entries = com.holagundi.archiman.data.BackupArchive.read(java.io.ByteArrayInputStream(exported)).toMutableMap()
        val data = org.json.JSONObject(String(entries.getValue("database.json"), Charsets.UTF_8))
        val tables = data.getJSONObject("tables")
        tables.keys().asSequence().filter { it.startsWith("ledger_") }.toList().forEach { tables.remove(it) }
        data.put("schemaVersion", 33).put("identity", "008241d3807f803f69fa85b294ff757b")
        entries["database.json"] = data.toString().toByteArray(Charsets.UTF_8)
        val tampered = java.io.ByteArrayOutputStream().also { com.holagundi.archiman.data.BackupArchive.write(entries, it) }.toByteArray()
        val prepared = backups.prepare(java.io.ByteArrayInputStream(tampered))
        check(prepared.preview.upgradedFrom == 33) { "upgradedFrom was ${prepared.preview.upgradedFrom}" }
        backups.restore(prepared)
        check(db.projectDao().projects().size == projectsBefore) { "projects changed on restore" }
        check(LedgerRepository(db).accounts().size == 35) { "restored chart has ${LedgerRepository(db).accounts().size} accounts" }
        check(LedgerRepository(db).trialBalance("2026-12-31").let { it.balanced && it.debitPaise == 0L }) { "restored ledger is not empty" }
        results.putString("schema33", "passed: schema 33 backup restores into schema 34 with the 35-account chart and no ledger journals")
    }
}
