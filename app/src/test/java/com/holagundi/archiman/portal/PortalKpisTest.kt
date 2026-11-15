package com.holagundi.archiman.portal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PortalKpisTest {
    @Test
    fun upcomingFilingsAreSortedAndCoverGstAndTds() {
        val dues = PortalKpis.upcomingFilings(LocalDate.of(2026, 11, 6))
        assertEquals(4, dues.size)
        assertTrue(dues.zipWithNext().all { (a, b) -> !a.dueOn.isAfter(b.dueOn) })
        assertTrue(dues.any { it.label == "GSTR-1" })
        assertTrue(dues.any { it.label == "GSTR-3B" })
        assertTrue(dues.any { it.label.contains("TDS") })
    }

    @Test
    fun stripAndTableRenderCarbonMarkup() {
        val strip = PortalKpis.strip(listOf(PortalKpis.Kpi("Active projects", "3", "building")))
        assertTrue(strip.contains("carbon-kpis"))
        assertTrue(strip.contains("Active projects"))
        val table = PortalKpis.dataTable("All projects", listOf("Project", "Status"), listOf(listOf("Tower A", "Active")))
        assertTrue(table.contains("cds--data-table"))
        assertTrue(table.contains("Tower A"))
        assertTrue(PortalKpis.filingStrip(dues = PortalKpis.upcomingFilings(LocalDate.of(2026, 11, 6))).contains("GSTR-1"))
    }
}
