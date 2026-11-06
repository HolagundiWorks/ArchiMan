package com.holagundi.archiman.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InvoiceBalanceTest {
    @Test fun partialPaymentLeavesOutstandingBalance() {
        assertEquals("60.00", InvoiceBalance.afterPayment(100.0, emptyList(), 40.0).toPlainString())
        assertEquals("0.00", InvoiceBalance.afterPayment(100.0, listOf(40.0), 60.0).toPlainString())
    }

    @Test fun decimalPaymentsSettleExactly() {
        assertEquals("0.00", InvoiceBalance.afterPayment(0.3, listOf(0.1), 0.2).toPlainString())
        assertEquals("1.01", InvoiceBalance.money(1.005).toPlainString())
    }

    @Test fun rejectsInvalidAndExcessPayments() {
        listOf(0.0, -1.0, 0.001, 60.01, Double.NaN, Double.POSITIVE_INFINITY).forEach { amount ->
            assertThrows(IllegalArgumentException::class.java) {
                InvoiceBalance.afterPayment(100.0, listOf(40.0), amount)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            InvoiceBalance.afterPayment(100.0, listOf(100.0), 1.0)
        }
    }
}
