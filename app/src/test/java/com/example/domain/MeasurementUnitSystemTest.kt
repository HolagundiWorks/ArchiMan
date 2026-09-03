package com.example.domain

import com.example.data.local.entity.CalculationType
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementUnitSystemTest {
    @Test
    fun `converts linear metric and imperial values both ways`() {
        val feet = MeasurementUnitConverter.linear(1.0, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL)
        assertEquals(3.280839895, feet, 0.000000001)
        assertEquals(1.0, MeasurementUnitConverter.linear(feet, MeasurementUnitSystem.IMPERIAL, MeasurementUnitSystem.METRIC), 0.000000001)
    }

    @Test
    fun `converts quantities using formula dimension`() {
        assertEquals(10.7639104, MeasurementUnitConverter.quantity(1.0, CalculationType.AREA, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL), 0.000001)
        assertEquals(35.3146667, MeasurementUnitConverter.quantity(1.0, CalculationType.VOLUME, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL), 0.000001)
        assertEquals(5.0, MeasurementUnitConverter.quantity(5.0, CalculationType.NOS, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL), 0.0)
    }

    @Test
    fun `wall deduction converts as area`() {
        assertEquals(10.7639104, MeasurementUnitConverter.deduction(1.0, CalculationType.WALL_PLASTER, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL), 0.000001)
    }

    @Test
    fun `formats editable values without floating point noise`() {
        assertEquals("3.2808", MeasurementUnitConverter.convertText("1", 1, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL))
        assertEquals("", MeasurementUnitConverter.convertText("", 1, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL))
        assertEquals("abc", MeasurementUnitConverter.convertText("abc", 1, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL))
    }

    @Test
    fun `maps formula to selected display unit`() {
        assertEquals("ft", MeasurementUnitConverter.unitFor(CalculationType.RUNNING_LENGTH, MeasurementUnitSystem.IMPERIAL))
        assertEquals("ft²", MeasurementUnitConverter.unitFor(CalculationType.WALL_PLASTER, MeasurementUnitSystem.IMPERIAL))
        assertEquals("m³", MeasurementUnitConverter.unitFor(CalculationType.VOLUME, MeasurementUnitSystem.METRIC))
        assertEquals("Nos", MeasurementUnitConverter.unitFor(CalculationType.NOS, MeasurementUnitSystem.IMPERIAL, "Nos"))
    }
}
