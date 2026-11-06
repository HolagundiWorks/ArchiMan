package com.holagundi.archiman.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Decimal arithmetic at the boundary of the legacy Double-backed money columns. */
object InvoiceBalance {
    fun money(value: Double): BigDecimal {
        require(value.isFinite()) { "Enter a finite amount." }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    }

    fun outstanding(total: Double, receipts: List<Double>): BigDecimal =
        money(total).subtract(receipts.fold(BigDecimal("0.00")) { sum, value -> sum.add(money(value)) })

    fun afterPayment(total: Double, receipts: List<Double>, payment: Double): BigDecimal {
        val amount = money(payment)
        require(amount.signum() > 0) { "Payment must be greater than zero." }
        val remaining = outstanding(total, receipts)
        require(amount <= remaining) { "Payment exceeds the outstanding balance." }
        return remaining.subtract(amount)
    }
}
