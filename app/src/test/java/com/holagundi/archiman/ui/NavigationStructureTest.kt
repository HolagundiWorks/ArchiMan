package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.shell.ProjectGroup
import com.holagundi.archiman.ui.shell.WorkspacePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStructureTest {
    @Test fun allExistingFeaturesRemainReachableFromASection() {
        val legacy = setOf("BRIEF", "TASKS", "LETTERS", "PROPOSAL", "CONTRACT", "VISITS", "SNAGS", "COORDINATION", "DRAWINGS", "SITEMAN", "LEVELS", "MEASURE", "BOOK", "INVOICE", "BILL")
        val reached = mutableSetOf<WorkspacePage>()
        fun visit(page: WorkspacePage) {
            check(reached.add(page)) { "Navigation contains a cycle" }
            WorkspacePage.entries.filter { it.parent == page }.forEach(::visit)
        }
        ProjectGroup.entries.forEach { group -> WorkspacePage.entries.filter { it.group == group && it.parent == null }.forEach(::visit) }
        assertTrue(reached.map { it.name }.containsAll(legacy))
        assertEquals(WorkspacePage.entries.toSet(), reached)
    }

    @Test fun childDestinationsKeepTheirProjectSection() {
        WorkspacePage.entries.forEach { page -> page.parent?.let { assertEquals(it.group, page.group) } }
        assertEquals(WorkspacePage.DOCUMENTS, WorkspacePage.DRAWINGS.parent)
        assertEquals(WorkspacePage.MEASUREMENTS, WorkspacePage.BOOK.parent)
        assertEquals(ProjectGroup.FEES, WorkspacePage.PROPOSAL.group)
    }
}
