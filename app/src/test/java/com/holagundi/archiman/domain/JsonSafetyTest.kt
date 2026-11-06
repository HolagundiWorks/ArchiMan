package com.holagundi.archiman.domain

import org.junit.Assert.*
import org.junit.Test

class JsonSafetyTest {
    @Test fun rejectsDeepNestingBeforeRecursiveParsing() {
        assertTrue(runCatching { JsonSafety.check("[".repeat(10000) + "]".repeat(10000)) }.isFailure)
        val destination = java.io.ByteArrayOutputStream()
        assertTrue(runCatching { com.holagundi.archiman.data.BackupArchive.write(mapOf("database.json" to ("[".repeat(100)+"]".repeat(100)).toByteArray()),destination) }.isFailure)
        assertEquals(0,destination.size())
    }
    @Test fun bracketsAndEscapesInsideStringsDoNotCount() {
        val text = "{\"text\":\"" + "[".repeat(50000) + "\\\"\\\\\"}"
        JsonSafety.check(text)
    }
    @Test fun rejectsObjectAllocationBomb() {
        assertTrue(runCatching { JsonSafety.check("[" + List(100001) { "{}" }.joinToString(",") + "]") }.isFailure)
    }
    @Test fun rejectsIncompleteInput() {
        listOf("{", "}", "{\"text\":\"unterminated").forEach { assertTrue(runCatching { JsonSafety.check(it) }.isFailure) }
    }
}
