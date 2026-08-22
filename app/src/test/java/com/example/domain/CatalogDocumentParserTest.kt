package com.example.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogDocumentParserTest {
    @Test fun bundledCatalogIsVersionedValidAndConvertible() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = CatalogDocumentParser.loadBundled(context)
        val items = CatalogDocumentParser.toMasterItems(document)

        assertEquals(1, document.schemaVersion)
        assertTrue(document.contractorTypes.map { it.name }.containsAll(listOf("Civil", "Electrical", "Plumbing", "Carpenter", "Painter", "Flooring & Cladding")))
        assertTrue(items.size >= 50)
        assertTrue(items.all { it.itemCode.isNotBlank() && it.workType.isNotBlank() })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownFormula() {
        CatalogDocumentParser.parse("""{"schemaVersion":1,"contractorTypes":[{"name":"Civil","items":[{"name":"Bad","uom":"m","formula":"PRICE","workType":"General"}]}]}""")
    }
}
