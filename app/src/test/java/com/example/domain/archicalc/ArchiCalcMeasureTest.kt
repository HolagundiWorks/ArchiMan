package com.example.domain.archicalc

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiCalcMeasureTest {
    @Test fun lengthFromMetricToken() {
        val result = ArchiCalcMeasure.measure("3.5")
        assertEquals(ArchiCalcMeasure.Kind.LENGTH, result.kind)
        assertEquals(3.5, result.meters, 0.0001)
    }

    @Test fun areaFromTwoFactors() {
        val result = ArchiCalcMeasure.measure("2 x 4")
        assertEquals(ArchiCalcMeasure.Kind.AREA, result.kind)
        assertEquals(8.0, result.squareMeters, 0.0001)
    }

    @Test fun volumeFromThreeFactors() {
        val result = ArchiCalcMeasure.measure("2 x 3 x 4")
        assertEquals(ArchiCalcMeasure.Kind.VOLUME, result.kind)
        assertEquals(24.0, result.cubicMeters, 0.0001)
    }

    @Test fun parsesFeetAndInches() {
        val result = ArchiCalcMeasure.measure("10'-6\"")
        assertEquals(ArchiCalcMeasure.Kind.LENGTH, result.kind)
        assertEquals(3.2004, result.meters, 0.0001)
    }

    @Test fun formatGridIncludesMetricAndImperial() {
        val grid = ArchiCalcMeasure.formatGrid("2 x 2")
        assertEquals("2.00", grid["m"])
        assertEquals("4.00", grid["m2"])
    }
}
