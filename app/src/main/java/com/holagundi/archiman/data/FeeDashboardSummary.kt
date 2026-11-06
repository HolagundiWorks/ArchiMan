package com.holagundi.archiman.data

import com.holagundi.archiman.data.local.FeeDashboardTotals
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ReceiptEntity
import com.holagundi.archiman.domain.InvoiceBalance
import java.math.BigDecimal

data class FeeDashboardSummary(val outstanding: BigDecimal, val received: BigDecimal, val unpaidInvoices: Int, val tdsWithheld: BigDecimal = BigDecimal("0.00")) {
    companion object {
        fun calculate(invoices: List<InvoiceEntity>, receipts: List<ReceiptEntity>): FeeDashboardSummary {
            val byInvoice = receipts.groupBy { it.invoiceId }
            var outstanding = BigDecimal("0.00")
            var received = BigDecimal("0.00")
            var unpaid = 0
            invoices.filter { it.status in listOf("ISSUED", "PAID") }.forEach { invoice ->
                val payments = byInvoice[invoice.id].orEmpty().map { it.amount + it.tdsAmount }
                val balance = InvoiceBalance.outstanding(invoice.amount, payments).max(BigDecimal.ZERO)
                outstanding = outstanding.add(balance)
                received = received.add(byInvoice[invoice.id].orEmpty().map { it.amount }.fold(BigDecimal("0.00")) { sum, amount -> sum.add(InvoiceBalance.money(amount)) })
                if (balance.signum() > 0) unpaid++
            }
            val countedIds=invoices.filter { it.status in listOf("ISSUED","PAID") }.map { it.id }.toSet()
            return FeeDashboardSummary(outstanding, received, unpaid, receipts.filter { r -> r.invoiceId in countedIds }.fold(BigDecimal("0.00")) { total, receipt -> total + InvoiceBalance.money(receipt.tdsAmount) })
        }

        fun fromTotals(totals: FeeDashboardTotals) = FeeDashboardSummary(
            outstanding = InvoiceBalance.money(totals.outstanding).max(BigDecimal.ZERO),
            received = InvoiceBalance.money(totals.received),
            unpaidInvoices = totals.unpaidInvoices,
            tdsWithheld = InvoiceBalance.money(totals.tdsWithheld),
        )
    }
}
