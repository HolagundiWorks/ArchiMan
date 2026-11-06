package com.holagundi.archiman.data

import com.holagundi.archiman.data.local.FeeDashboardTotals
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ReceiptEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FeeDashboardSummaryTest {
    private fun invoice(id: Long, status: String, amount: Double = 100.0) = InvoiceEntity(id, 1, null, "INV-$id", "Fee", amount, status, 0)
    private fun receipt(id: Long, invoiceId: Long, amount: Double) = ReceiptEntity(id, invoiceId, 1, amount, "Payment", 0)

    @Test fun excludesDraftsAndVoidsFromReceivables() {
        val result = FeeDashboardSummary.calculate(listOf(invoice(1, "DRAFT"), invoice(2, "VOID"), invoice(3, "ISSUED")), emptyList())
        assertEquals("100.00", result.outstanding.toPlainString())
        assertEquals(1, result.unpaidInvoices)
    }

    @Test fun partialPaymentsAndReversalsUseTheLedgerRatherThanStatusAlone() {
        val result = FeeDashboardSummary.calculate(listOf(invoice(1, "PAID"), invoice(2, "ISSUED")), listOf(receipt(1, 1, 40.0), receipt(2, 2, 100.0), receipt(3, 2, -100.0)))
        assertEquals("160.00", result.outstanding.toPlainString())
        assertEquals("40.00", result.received.toPlainString())
        assertEquals(2, result.unpaidInvoices)
    }

    @Test fun settledInvoiceHasNoOutstandingBalance() {
        val result = FeeDashboardSummary.calculate(listOf(invoice(1, "PAID", 0.3)), listOf(receipt(1, 1, 0.1), receipt(2, 1, 0.2)))
        assertEquals("0.00", result.outstanding.toPlainString())
        assertEquals("0.30", result.received.toPlainString())
        assertEquals(0, result.unpaidInvoices)
    }

    @Test fun sqlTotalsMapToScaledMoney() {
        val result = FeeDashboardSummary.fromTotals(FeeDashboardTotals(160.0, 40.0, 2, 5.5))
        assertEquals("160.00", result.outstanding.toPlainString())
        assertEquals("40.00", result.received.toPlainString())
        assertEquals(2, result.unpaidInvoices)
        assertEquals("5.50", result.tdsWithheld.toPlainString())
    }
}
