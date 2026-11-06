package com.holagundi.archiman.data

import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.*
import com.holagundi.archiman.domain.*
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class FinanceRepository @Inject constructor(private val db: ArchiManDatabase, private val suite: SuiteRepository) {
    private val dao get() = db.suiteDao()
    fun observe() = dao.observeFinanceKinds(listOf("SETTINGS","BANK"))
    fun observeSettings() = dao.observeFinanceSettings()
    fun invoices() = dao.observeAllInvoices()
    fun receipts() = dao.observeAllReceipts()
    suspend fun settings(): JSONObject = dao.finance("settings")?.let { JSONObject(it.json) } ?: JSONObject().put("mode", "NONE").put("rate", "18").put("state", "").put("gstin", "")
    suspend fun saveSettings(mode: String, rate: String, state: String, gstin: String) = db.withTransaction {
        GstMode.valueOf(mode)
        if (mode != "NONE") require(gstin.matches(Regex("[0-9]{2}[A-Z0-9]{13}")) && gstin.take(2) == state) { "Enter GSTIN and matching two-digit supplier state." }
        require(FinanceMath.decimal(rate) <= java.math.BigDecimal("100")) { "Invalid rate." }
        db.openHelper.writableDatabase.execSQL("UPDATE company_profile SET gstin = ? WHERE id = 1", arrayOf(if (mode == "NONE") "" else gstin))
        dao.saveFinance(FinanceRecordEntity("settings", "SETTINGS", JSONObject().put("mode", mode).put("rate", rate).put("state", state).put("gstin", gstin).toString()))
    }
    suspend fun createInvoice(projectId: Long, title: String, base: String, place: String, sac: String, clientGstin: String) = db.withTransaction {
        require(title.isNotBlank()) { "Enter an invoice title." }
        val config = settings(); val mode = GstMode.valueOf(config.getString("mode"))
        if (mode != GstMode.NONE) require(sac.matches(Regex("[0-9]{6}"))) { "Enter the six-digit SAC for this service." }
        if (clientGstin.isNotBlank()) require(clientGstin.matches(Regex("[0-9]{2}[A-Z0-9]{13}"))) { "Invalid client GSTIN format." }
        val tax = FinanceMath.tax(base, mode, config.getString("rate"), config.getString("state"), place)
        require(tax.total.signum() > 0) { "Invoice amount must be positive." }
        config.put("base", tax.base.toPlainString()).put("cgst", tax.cgst.toPlainString()).put("sgst", tax.sgst.toPlainString()).put("igst", tax.igst.toPlainString()).put("total", tax.total.toPlainString()).put("place", place).put("sac", sac).put("clientGstin", clientGstin)
        val now = System.currentTimeMillis()
        dao.insertInvoice(InvoiceEntity(projectId = projectId, stageId = null, number = "INV-${java.util.UUID.randomUUID()}", title = title.trim(), amount = tax.total.toDouble(), status = "DRAFT", createdAt = now, taxJson = config.toString()))
    }
    suspend fun import(rows:List<BankRow>,account:String):Int = db.withTransaction {
        require(account.isNotBlank() && account.length<=120) { "Enter an account alias up to 120 characters." }
        val unique=rows.distinctBy { it.key }
        val existing=unique.map { it.key }.chunked(500).flatMap { dao.existingFinanceKeys(it) }.toSet()
        val pending=unique.filter { it.key !in existing }.map { row ->
            val json=JSONObject().put("date",row.date).put("description",row.description).put("reference",row.reference).put("amount",row.amount.toPlainString()).put("account",account.trim()).put("status","UNMATCHED")
            FinanceRecordEntity(row.key,"BANK",json.toString())
        }
        if(pending.isEmpty()) 0 else dao.insertFinanceBatch(pending).count { it!=-1L }
    }
    suspend fun ignore(key: String) = db.withTransaction {
        val row = requireNotNull(dao.finance(key)); val json = JSONObject(row.json)
        require(json.getString("status") != "MATCHED") { "Reverse its receipt before changing a matched transaction." }
        json.put("status", if (json.getString("status") == "IGNORED") "UNMATCHED" else "IGNORED")
        dao.saveFinance(row.copy(json = json.toString()))
    }
    suspend fun match(key: String, invoiceId: Long, tds: String, reference: String) = db.withTransaction {
        val row = requireNotNull(dao.finance(key)); val json = JSONObject(row.json)
        require(json.getString("status") == "UNMATCHED") { "Transaction was already processed." }
        val amount = json.getString("amount").toDouble(); require(amount > 0) { "Only bank credits can settle invoices." }
        val invoice = requireNotNull(dao.invoice(invoiceId))
        suite.recordReceipt(invoice.projectId, invoice, amount, "Bank ${json.getString("date")} ${json.getString("reference")}", FinanceMath.decimal(tds.ifBlank { "0" }).toDouble(), reference, key, java.time.LocalDate.parse(json.getString("date")).atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli())
        dao.saveFinance(row.copy(json = json.put("status", "MATCHED").put("invoiceId", invoiceId).toString()))
    }
    suspend fun verifyTds(receiptId: Long) = db.withTransaction {
        val sql = db.openHelper.writableDatabase
        sql.query("SELECT COUNT(*) FROM receipts WHERE note LIKE ?", arrayOf("REVERSAL:$receiptId:%")).use { it.moveToFirst(); require(it.getInt(0) == 0) { "A reversed deduction cannot be verified." } }
        sql.execSQL("UPDATE receipts SET tdsVerified = 1 WHERE id = ? AND tdsAmount > 0", arrayOf(receiptId))
    }
}
