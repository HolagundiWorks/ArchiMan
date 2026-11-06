package com.holagundi.archiman.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CarbonChartDataTest {
    @Test fun emptyAndZeroDistributionsHaveNoInventedShares() {
        assertEquals(emptyList<Float>(),CarbonChartData.fractions(emptyList()))
        assertEquals(listOf(0f,0f),CarbonChartData.fractions(listOf(CarbonChartDatum("Cash",BigDecimal.ZERO),CarbonChartDatum("TDS",BigDecimal.ZERO))))
    }
    @Test fun feeDistributionPreservesTdsAndExactDecimals() {
        val rows=listOf(CarbonChartDatum("Cash",BigDecimal("9000000.00")),CarbonChartDatum("TDS",BigDecimal("1000000.00")),CarbonChartDatum("Outstanding",BigDecimal.ZERO))
        val fractions=CarbonChartData.fractions(rows)
        assertEquals(.9f,fractions[0],.000001f);assertEquals(.1f,fractions[1],.000001f);assertEquals(0f,fractions[2],0f)
        assertEquals(BigDecimal("9000000.00"),rows[0].value)
    }
    @Test fun barsUseOneSharedZeroBasedScale() {
        assertEquals(listOf(.5f,1f,0f),CarbonChartData.barFractions(listOf(CarbonChartDatum("Active",2.toBigDecimal()),CarbonChartDatum("Complete",4.toBigDecimal()),CarbonChartDatum("Archived",BigDecimal.ZERO))))
    }
    @Test fun negativeValuesRequireAReviewInsteadOfAMisleadingChart() {
        assertTrue(runCatching{CarbonChartData.fractions(listOf(CarbonChartDatum("Invalid",BigDecimal("-1"))))}.isFailure)
    }
}
