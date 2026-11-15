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
                postings(context, results)
                expensesAndIncome(context, results)
                schema33Restore(isolated, results)
                results.putString("migration", "passed: migration tables match entities; immutability triggers reject change and delete")
            }
            results.putString("stream", "\nLedger checks PASSED\n" + listOf("chart", "post", "tds", "asOf", "rules", "once", "closed", "reverse", "migration", "issue", "receipt", "gstNone", "gstRegular", "rejections", "schema33").joinToString("\n") { results.getString(it).orEmpty() } + "\n")
            instrumentation.finish(Activity.RESULT_OK, results)
        } catch (error: Throwable) {
            results.putString("stream", "\nLedger checks FAILED: $error\n" + error.stackTrace.take(8).joinToString("\n") + "\n")
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

    /** Issuing an invoice and recording a receipt post journals in the same transaction; the trial balance stays equal. */
    private fun row(tb: com.holagundi.archiman.data.TrialBalance, code: String) =
        tb.rows.firstOrNull { it.code == code } ?: error("no ledger row $code; rows=${tb.rows.map { it.code }}")

    private suspend fun postings(context: android.content.Context, results: Bundle) {
        val db = Room.inMemoryDatabaseBuilder(context, ArchiManDatabase::class.java).build()
        DemoCompanyStore(context, db).openBuiltIn()
        val suite = com.holagundi.archiman.data.SuiteRepository(
            db, db.suiteDao(), db.practiceDao(), db.projectDao(), db.catalogDao(), db.measurementDao(),
            com.holagundi.archiman.data.MeasurementRepository(db, db.measurementDao(), db.catalogDao(), db.projectDao()),
        )
        val ledger = LedgerRepository(db)
        val (invoiceId, projectId) = db.openHelper.writableDatabase.query("SELECT id, projectId FROM invoices WHERE status = 'DRAFT' ORDER BY id LIMIT 1").use {
            check(it.moveToFirst()) { "demo company has no draft invoice" }
            it.getLong(0) to it.getLong(1)
        }
        val amount = db.suiteDao().invoice(invoiceId)!!.amount
        suite.issueInvoice(invoiceId)
        val issued = ledger.trialBalance(java.time.LocalDate.now().toString())
        check(ledger.accounts().isNotEmpty() && ledger.journals("2000-01-01", "2100-12-31").isNotEmpty()) { "issue posted nothing: status=${db.suiteDao().invoice(invoiceId)!!.status} accounts=${ledger.accounts().size} journals=${ledger.journals("2000-01-01", "2100-12-31").size} invoiceId=$invoiceId" }
        check(issued.balanced) { "trial balance unequal after issue" }
        val receivable = row(issued, "1200")
        check(receivable.debitPaise == com.holagundi.archiman.domain.LedgerRules.parsePaise(String.format(java.util.Locale.US, "%.2f", amount))!!) {
            "receivable ${receivable.debitPaise} does not equal invoice amount $amount"
        }
        check(row(issued, "4000").creditPaise == receivable.debitPaise) { "fee income does not match the invoice" }
        results.putString("issue", "passed: issuing the demo invoice books the receivable and fee income, trial balance equal")

        val invoice = db.suiteDao().invoice(invoiceId)!!
        suite.recordReceipt(projectId, invoice, 1000.0, "Part payment", 50.0, "26AS-DEVICE-CHECK", "", System.currentTimeMillis())
        val paid = ledger.trialBalance(java.time.LocalDate.now().toString())
        check(paid.balanced) { "trial balance unequal after receipt" }
        check(row(paid, "1210").debitPaise == 50_00L) { "TDS receivable not booked" }
        check(row(paid, "1010").debitPaise == 1000_00L) { "bank not debited with cash receipt" }
        check(row(paid, "1200").creditPaise == 1050_00L) { "receivable not cleared by cash plus TDS" }
        results.putString("receipt", "passed: a receipt with TDS books bank, TDS receivable and clears the receivable, trial balance equal")
        db.close()
    }

    /** Expenses and income follow MynaHub's GST and TDS rules; GST is claimed only under a regular registration. */
    private suspend fun expensesAndIncome(context: android.content.Context, results: Bundle) {
        val db = Room.inMemoryDatabaseBuilder(context, ArchiManDatabase::class.java).build()
        DemoCompanyStore(context, db).openBuiltIn()
        LedgerChart.ensure(db)
        val ledger = LedgerRepository(db)
        suspend fun scheme(mode: String) = db.suiteDao().saveFinance(
            com.holagundi.archiman.data.local.entity.FinanceRecordEntity("settings", "SETTINGS",
                org.json.JSONObject().put("mode", mode).put("rate", "18").put("state", "").put("gstin", "").toString()),
        )
        val date = "2026-11-10"
        scheme("NONE")
        rejects { ledger.postIncome(date, "GST without registration", "4000", 1000_00, 180_00, false, 0, "bank") }
        ledger.postExpense(date, "Site travel", "5300", 1000_00, 180_00, 0, "bank")
        val noRegistration = ledger.trialBalance("2026-12-31")
        check(noRegistration.balanced) { "not balanced after unregistered expense" }
        check(row(noRegistration, "5300").debitPaise == 1180_00L) { "GST not part of cost without registration" }
        check(row(noRegistration, "1220").debitPaise == 0L) { "input credit claimed without registration" }
        results.putString("gstNone", "passed: without GST registration the GST is part of the cost and no input credit is claimed")

        scheme("REGULAR")
        ledger.postExpense(date, "Printing", "5400", 500_00, 90_00, 0, "cash")
        ledger.postIncome(date, "Consultancy", "4000", 1000_00, 180_00, false, 50_00, "bank")
        ledger.postIncome(date, "Bank interest", "4200", 100_00, 18_00, true, 0, "bank")
        val tb = ledger.trialBalance("2026-12-31")
        check(tb.balanced) { "trial balance unequal after expenses and income" }
        check(row(tb, "5400").debitPaise == 500_00L && row(tb, "1220").debitPaise == 90_00L) { "input credit not claimed under registration" }
        check(row(tb, "1000").creditPaise == 590_00L) { "cash not credited for printing" }
        check(row(tb, "1210").debitPaise == 50_00L) { "TDS deducted by client not booked" }
        check(row(tb, "2100").creditPaise == 90_00L && row(tb, "2110").creditPaise == 90_00L) { "CGST and SGST not split" }
        check(row(tb, "2120").creditPaise == 18_00L) { "IGST not booked for inter-state income" }
        check(row(tb, "1010").debitPaise == 1248_00L && row(tb, "1010").creditPaise == 1180_00L) { "bank lines do not match the entries" }
        results.putString("gstRegular", "passed: with a regular registration input credit is claimed, GST output split CGST/SGST or IGST, TDS booked, trial balance equal")

        rejects { ledger.postExpense(date, "Wrong account", "4000", 10_00, 0, 0, "bank") }
        rejects { ledger.postExpense(date, "TDS too high", "5300", 10_00, 0, 20_00, "bank") }
        rejects { ledger.postExpense(date, "Unknown payment", "5300", 10_00, 0, 0, "barter") }
        rejects { ledger.postIncome(date, "Wrong income account", "5300", 10_00, 0, false, 0, "bank") }
        results.putString("rejections", "passed: wrong account type, TDS above amount and unknown payment method are rejected")
    }
}
