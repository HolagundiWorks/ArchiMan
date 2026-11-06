package com.holagundi.archiman.ui

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardDateTest {
    @Test fun comparesCalendarDatesInTheDeviceTimeZone() {
        val zone = ZoneId.of("Asia/Kolkata")
        val today = LocalDate.of(2026, 11, 5)
        val dueLateToday = today.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        assertEquals("Due today", taskDueLabel(dueLateToday, today, zone))
        assertTrue(taskDueLabel(today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), today, zone).startsWith("Overdue"))
        assertEquals("No due date", taskDueLabel(null, today, zone))
    }
}
