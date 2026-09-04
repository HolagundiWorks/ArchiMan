package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode

object RateBookCalculator {
    fun amount(quantity: Double, rate: Double): Double {
        require(quantity.isFinite() && quantity >= 0.0) { "Quantity must be a finite, non-negative number" }
        require(rate.isFinite() && rate >= 0.0) { "Rate must be a finite, non-negative number" }
        return BigDecimal.valueOf(quantity)
            .multiply(BigDecimal.valueOf(rate))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
