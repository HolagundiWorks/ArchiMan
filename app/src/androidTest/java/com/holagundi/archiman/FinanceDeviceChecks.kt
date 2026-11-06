package com.holagundi.archiman
import android.content.Context
import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.domain.*
import kotlinx.coroutines.flow.first
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.json.JSONObject
import com.holagundi.archiman.portal.PortalServer
object FinanceDeviceChecks {
    suspend fun run(context: Context, db: ArchiManDatabase, suite: SuiteRepository) {
        suite.ensureDefaults()
        check(db.practiceDao().activeUsers().single().role == "Admin")
        val project = ProjectRepository(db, db.projectDao(), db.practiceDao()).createProject("Finance test", "FIN", "Company client", "Residential", "Bengaluru", "Architect")
        val finance = FinanceRepository(db, suite)
        finance.saveSettings("REGULAR", "18", "29", "29ABCDE1234F1Z5")
        val id = finance.createInvoice(project, "Design fee", "1000", "29", "998322", "")
        val invoice = db.suiteDao().invoice(id)!!
        check(invoice.amount == 1180.0 && JSONObject(invoice.taxJson).getString("cgst") == "90.00")
        suite.issueInvoice(id)
        val bank = BankCsv.parse("Date,Amount,Description,Reference\n05/11/2026,1080,Company payment,UTR123", "Business")
        check(finance.import(bank, "Business") == 1); check(finance.import(bank, "Business") == 0)
        finance.match(bank.single().key, id, "100", "Client deduction advice")
        check(db.suiteDao().invoice(id)!!.status == "PAID")
        val receipt = db.suiteDao().invoiceReceipts(id).single()
        check(receipt.amount == 1080.0 && receipt.tdsAmount == 100.0 && !receipt.tdsVerified)
        val summary = FeeDashboardSummary.calculate(listOf(db.suiteDao().invoice(id)!!), listOf(receipt))
        check(summary.outstanding.signum() == 0 && summary.received.toPlainString() == "1080.00" && summary.tdsWithheld.toPlainString() == "100.00")
        check(runCatching { finance.match(bank.single().key, id, "100", "Repeat") }.isFailure)
        finance.verifyTds(receipt.id); check(db.suiteDao().invoiceReceipts(id).single().tdsVerified)
        RecordOperations(db).reverseReceipt(receipt.id, "Test correction")
        check(db.suiteDao().invoice(id)!!.status == "ISSUED")
        check(JSONObject(db.suiteDao().finance(bank.single().key)!!.json).getString("status") == "UNMATCHED")
        check(runCatching { RecordOperations(db).reverseReceipt(receipt.id, "Repeat") }.isFailure)
        check(db.suiteDao().invoiceReceipts(id).sumOf { it.tdsAmount } == 0.0)
        finance.match(bank.single().key, id, "100", "Corrected advice")
        finance.saveSettings("COMPOSITION", "18", "29", "29ABCDE1234F1Z5")
        val composition = finance.createInvoice(project, "Composition fee", "1000", "", "998322", "")
        check(db.suiteDao().invoice(composition)!!.amount == 1000.0)
        val backup = BackupRepository(context, db, PortalServer(db, context))
        val bytes = ByteArrayOutputStream().also { backup.export(it) }.toByteArray()
        backup.restore(backup.prepare(ByteArrayInputStream(bytes)))
        check(db.suiteDao().invoiceReceipts(id).sumOf { it.tdsAmount } == 100.0)
        check(JSONObject(db.suiteDao().finance(bank.single().key)!!.json).getString("status") == "MATCHED")
        finance.saveSettings("NONE", "18", "", "")
    }
}
