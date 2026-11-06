package com.holagundi.archiman.portal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalLayoutTest {
    @Test
    fun signInUsesTheCarbonShellAndFields() {
        val page = PortalPages.login("Studio Holagundi", emptyList(), "csrf-token", "That code does not match the phone.")
        assertTrue(page.contains("class=\"cds--header\""))
        assertTrue(page.contains("class=\"cds--tile portal-sign-in\""))
        assertTrue(page.contains("class=\"cds--text-input\""))
        assertTrue(page.contains("class=\"cds--btn cds--btn--primary\""))
        assertTrue(page.contains("role=\"alert\""))
        assertTrue(page.contains("class=\"carbon-icon"))
        assertTrue(page.contains("portal-sign-in-brand"))
        assertFalse(page.contains("max-width:720px"))
        assertFalse(page.contains("cds--side-nav"))
    }

    @Test
    fun projectsUseSideNavigationTilesIconsAndTransfer() {
        val page = PortalPages.home(null, emptyList(), "Admin", "csrf-token")
        assertTrue(page.contains("cds--side-nav--expanded"))
        assertTrue(page.contains("href=\"/transfer\""))
        assertTrue(page.contains("Practice transfer"))
        assertTrue(page.contains("cds--tile--clickable"))
        assertTrue(page.contains("portal-tile-icon"))
        assertTrue(page.contains("cds--side-nav__icon"))
        assertTrue(page.contains("action=\"/logout\""))
        assertTrue(page.contains("portal-masthead"))
    }
}
