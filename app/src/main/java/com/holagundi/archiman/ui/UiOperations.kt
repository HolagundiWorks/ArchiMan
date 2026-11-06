package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonNoticeKind
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiNotice(val message: String, val kind: CarbonNoticeKind)

class SaveAction(private val scope: CoroutineScope, private val result: Deferred<Boolean>) {
    /** Runs on the owning UI scope only after a committed save. Rejected repeat taps never clear a draft. */
    fun onSuccess(block: () -> Unit) { scope.launch { if (result.await()) block() } }
}

class UiOperations(private val scope: CoroutineScope) {
    private val active = mutableSetOf<String>()
    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    val pending = _pending.asStateFlow()
    private val _notice = MutableStateFlow<UiNotice?>(null)
    val notice = _notice.asStateFlow()
    fun dismiss() { _notice.value = null }
    fun report(message: String, kind: CarbonNoticeKind) { _notice.value = UiNotice(message, kind) }

    // Called from the UI thread; acquire before launching, not inside a queued coroutine.
    fun run(key: String, success: String, block: suspend () -> Unit): SaveAction {
        if (!active.add(key)) return SaveAction(scope, CompletableDeferred(false))
        _pending.value = active.toSet()
        _notice.value = null
        val result = scope.async {
            try {
                block()
                report(success, CarbonNoticeKind.SUCCESS)
                true
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                report(failure.message ?: "Could not save. Your entries are retained; please retry.", CarbonNoticeKind.ERROR)
                false
            } finally { active.remove(key); _pending.value = active.toSet() }
        }
        // Also release when the parent was cancelled before the async body could start.
        result.invokeOnCompletion { active.remove(key); _pending.value = active.toSet() }
        return SaveAction(scope, result)
    }
}
