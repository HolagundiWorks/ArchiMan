package com.holagundi.archiman.domain
import org.junit.Assert.*
import org.junit.Test
class FinanceTest {
    @Test fun gstModesAndStateSplits() {
        assertEquals("1180.00", FinanceMath.tax("1000", GstMode.REGULAR, "18", "29", "29").total.toPlainString())
        assertEquals("90.00", FinanceMath.tax("1000", GstMode.REGULAR, "18", "29", "29").cgst.toPlainString())
        assertEquals("180.00", FinanceMath.tax("1000", GstMode.REGULAR, "18", "29", "27").igst.toPlainString())
        GstMode.entries.filter { it != GstMode.REGULAR }.forEach { assertEquals("1000.00", FinanceMath.tax("1000", it, "18", "", "").total.toPlainString()) }
    }
    @Test fun splitRoundingPreservesTotal() {
        val tax = FinanceMath.tax("0.05", GstMode.REGULAR, "18", "29", "29")
        assertEquals("0.06", tax.total.toPlainString()); assertEquals("0.01", (tax.cgst + tax.sgst).toPlainString())
    }
    @Test fun csvQuotesAndDuplicateFingerprints() {
        val csv = "Date,Narration,Credit,Debit,Reference\n05/11/2026,\"Client, payment\",\"1,080.00\",,UTR1\n06/11/2026,Fee,,100,UTR2"
        val rows = BankCsv.parse(csv, "Business")
        assertEquals("2026-11-05", rows[0].date); assertEquals("1080.00", rows[0].amount.toPlainString()); assertEquals("-100.00", rows[1].amount.toPlainString())
        assertEquals(rows, BankCsv.parse(csv, " business ")); assertNotEquals(rows[0].key, BankCsv.parse(csv, "Personal")[0].key)
    }
    @Test fun explicitDateOrder() {
        val csv = "Date,Amount\n05/11/2026,100"
        assertEquals("2026-11-05", BankCsv.parse(csv,"Bank")[0].date)
        assertEquals("2026-05-11", BankCsv.parse(csv,"Bank",true)[0].date)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidDateRejected() { BankCsv.parse("Date,Amount\n31/02/2026,100", "Bank") }
    @Test fun tdsSettlesButIsNotCash() {
        assertEquals("0.00", InvoiceBalance.outstanding(1180.0, listOf(1080.0 + 100.0)).toPlainString())
    }
    @Test fun extremeScientificNotationRejectedBeforeExpansion() {
        listOf("1e999999999","1E-999999999","NaN","Infinity","9".repeat(10000)).forEach { assertTrue(runCatching{FinanceMath.decimal(it)}.isFailure) }
        assertTrue(runCatching{BankCsv.parse("Date,Amount\n05/11/2026,1e999999999","Business")}.isFailure)
    }
    @Test fun hexEncodingPreservesFingerprintFormat() {
        val bytes=byteArrayOf(0,1,15,16,127,-128,-1)
        assertEquals("00010f107f80ff",Hex.encode(bytes))
        val data="stable reference".toByteArray();val hash=java.security.MessageDigest.getInstance("SHA-256").digest(data)
        assertEquals(hash.joinToString(""){"%02x".format(it)},Hex.encode(hash))
    }
    @Test fun bankDatesSupportIsoSingleDigitsAndNamedMonths() {
        listOf("2026-11-05","5/11/2026","05-NOV-2026").forEach { date -> assertEquals("2026-11-05",BankCsv.parse("Date,Amount\n$date,100","Business").single().date) }
    }
}
