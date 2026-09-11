package com.example.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppHierarchyTest {
    @Test
    fun projectNavigationContainsOnlyMeasurementAndSupportingProjectDestinations() {
        assertEquals(
            listOf(
                "OVERVIEW",
                "BRIEF_SCOPE",
                "PLANNING",
                "MORE",
                "DRAWINGS",
                "REPORTS",
                "CONTROLS",
                "CONTRACTORS"
            ),
            ProjectSection.entries.map { it.name }
        )
    }

    @Test
    fun userFacingDestinationsContainNoCommercialTerms() {
        val labels = buildList {
            addAll(AppScreen.entries.map { it.title })
            addAll(HomeTab.entries.map { it.label })
            addAll(DirectorySection.entries.map { it.label })
            addAll(ProjectSection.entries.map { it.name })
        }
        val commercialTerms = Regex("rate|amount|bill|invoice|payment|valuation", RegexOption.IGNORE_CASE)
        assertFalse(labels.any(commercialTerms::containsMatchIn))
    }
}
