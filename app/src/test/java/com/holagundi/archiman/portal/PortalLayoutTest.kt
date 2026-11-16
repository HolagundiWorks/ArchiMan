package com.holagundi.archiman.portal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalLayoutTest {
    @Test
    fun signInUsesTheCarbonShellAndFields() {
        val brand = PortalBrand(practiceName = "Studio Holagundi", accent = "#0F62FE")
        val admin = com.holagundi.archiman.data.local.entity.LocalUserEntity(1,"admin","Admin","Admin",true,0,0)
        val page = PortalPages.login("Studio Holagundi", listOf(admin), "csrf-token", "That code does not match the phone.", brand)
        assertTrue(page.contains("class=\"cds--header\""))
        assertTrue(page.contains("class=\"cds--tile portal-sign-in\""))
        assertTrue(page.contains("class=\"cds--text-input\""))
        assertTrue(page.contains("class=\"cds--btn cds--btn--primary\""))
        assertTrue(page.contains("role=\"alert\""))
        assertTrue(page.contains("class=\"carbon-icon"))
        assertTrue(page.contains("portal-sign-in-brand"))
        assertTrue(page.contains("Studio Holagundi"))
        assertTrue(page.contains("portal-brand-product"))
        assertTrue(page.contains("--brand-accent:#0F62FE"))
        assertFalse(page.contains("max-width:720px"))
        assertFalse(page.contains("cds--side-nav"))
    }

    @Test
    fun anUnconfiguredPracticeDoesNotOfferBackupTransfer() {
        val page = PortalPages.login("Demo", emptyList(), "csrf", null)
        assertTrue(page.contains("Company connection required"))
        assertFalse(page.contains("action=\"/transfer"))
        assertFalse(page.contains("type=\"password\""))
    }

    @Test
    fun projectsUseSideNavigationWithoutLocalBackupTransfer() {
        val brand = PortalBrand(practiceName = "Studio Holagundi", logoDataUrl = "data:image/png;base64,abc")
        val page = PortalPages.home(null, emptyList(), "Admin", "csrf-token", brand)
        assertTrue(page.contains("portal-side-nav"))
        assertTrue(page.contains("data-state=\"rail\""))
        assertTrue(page.contains("portal-nav-group"))
        assertTrue(page.contains("portal-nav-menu"))
        assertFalse(page.contains("href=\"/transfer\""))
        assertFalse(page.contains("Practice transfer"))
        assertTrue(page.contains("carbon-kpis"))
        assertTrue(page.contains("cds--data-table"))
        assertTrue(page.contains("Active projects"))
        assertTrue(page.contains("cds--side-nav__icon"))
        assertTrue(page.contains("action=\"/logout\""))
        assertTrue(page.contains("portal-masthead"))
        assertTrue(page.contains("id=\"theme-toggle\""))
        assertTrue(page.contains("id=\"portal-search\""))
        assertTrue(page.contains("People"))
        assertTrue(page.contains("Catalogue"))
        assertTrue(page.contains("href=\"/workspace?section=dashboard\""))
        assertTrue(page.contains("portal-brand-logo"))
        assertTrue(page.contains("portal-side-brand"))
        assertTrue(page.contains("Studio Holagundi"))
    }
}
