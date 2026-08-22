package com.example.domain

import com.example.data.local.entity.CalculationType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityCalculatorTest {
    @Test fun countUsesNumberOnly() = assertQty(4.0, CalculationType.NOS, MeasurementInput(no = 4.0, length = 99.0))
    @Test fun runningLengthUsesNumberAndLength() = assertQty(7.5, CalculationType.RUNNING_LENGTH, MeasurementInput(no = 3.0, length = 2.5))
    @Test fun areaUsesBreadth() = assertQty(24.0, CalculationType.AREA, MeasurementInput(no = 2.0, length = 4.0, breadth = 3.0, height = 99.0))
    @Test fun wallAreaUsesHeightAndDeduction() = assertQty(21.5, CalculationType.WALL_PLASTER, MeasurementInput(no = 2.0, length = 4.0, height = 3.0, deduction = 2.5))
    @Test fun volumeUsesAllDimensions() = assertQty(12.0, CalculationType.VOLUME, MeasurementInput(no = 2.0, length = 3.0, breadth = 2.0, height = 1.0))
    @Test fun negativeResultIsClampedToZero() = assertQty(0.0, CalculationType.WALL_PLASTER, MeasurementInput(no = 1.0, length = 1.0, height = 1.0, deduction = 5.0))
    @Test fun resultRoundsToTwoDecimals() = assertQty(0.33, CalculationType.AREA, MeasurementInput(length = 1.0, breadth = 0.3333))

    private fun assertQty(expected: Double, type: CalculationType, input: MeasurementInput) {
        assertEquals(expected, QuantityCalculator.calculate(type, input), 0.0001)
    }
}
