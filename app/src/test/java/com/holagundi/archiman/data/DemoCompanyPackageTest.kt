package com.holagundi.archiman.data

import com.holagundi.archiman.domain.CalculationTypes
import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.QuantityCalculator
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoCompanyPackageTest {
    @Test
    fun builtInFileIndexMatchesItsRecords() {
        val file = File("../demo/company/ArchiMan-Demo-Company.archimandb.json")
        val pack = DemoCompanyPackage.parse(file.readText())
        pack.requireOpenable()
        assertEquals("Studio Holagundi", pack.practiceName)
        assertEquals(28, pack.schemaVersion)
        assertEquals(2, pack.index.first { it.key == "projects" }.count)
        assertEquals(2, pack.index.first { it.key == "sheets" }.count)
        assertEquals(6, pack.index.first { it.key == "rooms" }.count)
        assertEquals(2, pack.index.first { it.key == "snags" }.count)
        assertEquals(2, pack.index.first { it.key == "coordination" }.count)
        assertEquals(1, pack.index.first { it.key == "drawings" }.count)
        assertEquals(1, pack.index.first { it.key == "revisions" }.count)
        assertEquals(1, pack.index.first { it.key == "logs" }.count)
        assertEquals(1, pack.index.first { it.key == "orders" }.count)
    }

    @Test
    fun villaBeamQuantityIsCalculatedFromDimensions() {
        val quantity = QuantityCalculator.calculate(
            CalculationTypes.VOLUME,
            MeasurementInput(no = 1.0, length = 24.5, breadth = 0.3, height = 0.45),
        )
        assertEquals(3.31, quantity, 0.001)
    }
}
