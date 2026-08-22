package com.example.domain

import com.example.data.local.entity.CalculationType
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementRowValidatorTest {
    private fun input(description: String = "North wall", no: String = "1", length: String = "4", breadth: String = "3", height: String = "2.8", deduction: String = "0") =
        MeasurementRowValidationInput(description, no, length, breadth, height, deduction)

    @Test fun `requires only count and description for nos`() {
        assertEquals(MeasurementRowStatus.READY, MeasurementRowValidator.validate(CalculationType.NOS, input(length = "", breadth = "", height = "")).status)
    }

    @Test fun `area requires breadth but not height`() {
        assertEquals(MeasurementRowStatus.READY, MeasurementRowValidator.validate(CalculationType.AREA, input(height = "")).status)
        assertEquals("Breadth is required", MeasurementRowValidator.validate(CalculationType.AREA, input(breadth = "")).message)
    }

    @Test fun `volume requires all dimensions`() {
        assertEquals("Height is required", MeasurementRowValidator.validate(CalculationType.VOLUME, input(height = "")).message)
    }

    @Test fun `default blank row is empty`() {
        assertEquals(MeasurementRowStatus.EMPTY, MeasurementRowValidator.validate(CalculationType.AREA, input(description = "", length = "", breadth = "", height = "")).status)
    }

    @Test fun `deduction cannot consume gross quantity`() {
        assertEquals(MeasurementRowStatus.INCOMPLETE, MeasurementRowValidator.validate(CalculationType.AREA, input(length = "2", breadth = "2", deduction = "4")).status)
    }
}
