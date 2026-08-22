package com.example.domain

import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.ItemMasterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkItemDuplicateDetectorTest {
    private fun item(id: Long, name: String, unit: String = "sqm", type: CalculationType = CalculationType.AREA) =
        ItemMasterEntity(id = id, workType = "Finishes", name = name, unit = unit, calculationType = type)

    @Test fun `finds reordered and lightly varied names`() {
        val result = WorkItemDuplicateDetector.find(listOf(
            item(1, "Internal wall cement plaster"),
            item(2, "Cement plaster - internal walls")
        ))
        assertEquals(1, result.size)
        assertTrue(result.single().similarity >= 78)
    }

    @Test fun `does not compare incompatible formula or unit`() {
        val source = item(1, "Internal wall cement plaster")
        val result = WorkItemDuplicateDetector.find(listOf(
            source,
            item(2, source.name, unit = "cum", type = CalculationType.VOLUME)
        ))
        assertTrue(result.isEmpty())
    }

    @Test fun `does not flag unrelated items in same work type`() {
        val result = WorkItemDuplicateDetector.find(listOf(
            item(1, "Internal wall cement plaster"),
            item(2, "Exterior acrylic paint")
        ))
        assertTrue(result.isEmpty())
    }
}
