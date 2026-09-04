package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RateBookCalculatorTest {
    @Test
    fun `amount is rounded to two currency decimals`() {
        assertEquals(333.33, RateBookCalculator.amount(quantity = 3.0, rate = 111.11), 0.0)
        assertEquals(10.01, RateBookCalculator.amount(quantity = 1.0, rate = 10.005), 0.0)
    }

    @Test
    fun `zero quantity produces zero amount`() {
        assertEquals(0.0, RateBookCalculator.amount(quantity = 0.0, rate = 999.0), 0.0)
    }

    @Test
    fun `negative and non-finite values are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { RateBookCalculator.amount(-1.0, 10.0) }
        assertThrows(IllegalArgumentException::class.java) { RateBookCalculator.amount(1.0, Double.NaN) }
    }
}
