package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonNoticeKind
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class UiOperationsTest {
    @Test fun rapidRepeatSubmissionsCommitOnceAndClearOnlyAfterSuccess(): Unit = runBlocking {
        val operations = UiOperations(this)
        val gate = CompletableDeferred<Unit>()
        val cleared = CompletableDeferred<Unit>()
        var writes = 0
        var draft = "Site visit observations"
        operations.run("visit", "Visit saved.") { writes++; gate.await() }.onSuccess { draft = ""; cleared.complete(Unit) }
        operations.run("visit", "Visit saved.") { writes++ }.onSuccess { fail("Repeat submission must not clear a draft") }
        yield()
        assertEquals(1, writes)
        assertEquals("Site visit observations", draft)
        assertEquals(setOf("visit"), operations.pending.value)
        gate.complete(Unit); cleared.await()
        assertEquals("", draft)
        assertTrue(operations.pending.value.isEmpty())
        assertEquals(CarbonNoticeKind.SUCCESS, operations.notice.value?.kind)
    }

    @Test fun failedSaveRetainsDraftAndCanBeRetriedWithoutBlockingUnrelatedWork(): Unit = runBlocking {
        val operations = UiOperations(this)
        val draft = "Client requirements"
        operations.run("brief", "Brief saved.") { error("Storage unavailable") }.onSuccess { fail("A failed save cannot clear the form") }
        yield(); yield()
        assertEquals("Client requirements", draft)
        assertTrue(operations.pending.value.isEmpty())
        assertEquals(CarbonNoticeKind.ERROR, operations.notice.value?.kind)
        val gate = CompletableDeferred<Unit>()
        val otherDone = CompletableDeferred<Unit>()
        operations.run("brief", "Brief saved.") { gate.await() }
        operations.run("task", "Task saved.") { }.onSuccess { otherDone.complete(Unit) }
        otherDone.await()
        assertEquals(setOf("brief"), operations.pending.value)
        gate.complete(Unit)
    }

    @Test fun cancellationReleasesGuardWithoutReportingSuccess(): Unit = runBlocking {
        val parent = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + parent)
        val operations = UiOperations(scope)
        operations.run("save", "Saved.") { awaitCancellation() }
        yield(); parent.cancelAndJoin()
        assertTrue(operations.pending.value.isEmpty())
        assertNull(operations.notice.value)
    }
}
