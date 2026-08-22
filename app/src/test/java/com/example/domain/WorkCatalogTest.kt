package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkCatalogTest {
    @Test fun classifiesCommonTrades() {
        assertEquals("Masonry", WorkCatalog.classify("Civil", "BrickWork 230mm"))
        assertEquals("Concrete & Structure", WorkCatalog.classify("Civil", "RCC Slab Concrete"))
        assertEquals("Electrical Works", WorkCatalog.classify("Electrical", "Concealed Conduit"))
        assertEquals("Plumbing Works", WorkCatalog.classify("Plumbing", "Drainage Pipe"))
        assertEquals("Flooring & Cladding", WorkCatalog.classify("Flooring", "Granite Flooring"))
    }

    @Test fun normalizesSpacingAndCase() {
        assertEquals("brickwork 230mm", WorkCatalog.normalizeName("  BrickWork   230mm "))
    }

    @Test fun generatedCodesAreStableAndWorkTypeScoped() {
        assertEquals(WorkCatalog.codeFor("Masonry", "Brickwork 230mm"), WorkCatalog.codeFor("Masonry", "Brickwork 230mm"))
    }
}
