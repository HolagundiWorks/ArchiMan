package com.holagundi.archiman.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.holagundi.archiman.data.EditorDraftStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

private object DraftWrites {
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    init { scope.launch { for (work in queue) work() } }
    fun enqueue(work: suspend () -> Unit) { queue.trySend(work) }
    suspend fun flush() { val done = CompletableDeferred<Unit>(); enqueue { done.complete(Unit) }; done.await() }
}

class LocalEditorDraft internal constructor(private val store: EditorDraftStore) {
    var recovered by mutableStateOf<String?>(null); internal set
    var status by mutableStateOf("Checking local draft…"); internal set
    internal var edited = false
    private var generation = 0L
    private var latest: String? = null
    private var writing = false
    private fun write(text: String?) {
        edited = true
        generation++
        latest = text
        status = "Saving local draft…"
        if (writing) return
        writing = true
        DraftWrites.enqueue {
            while (true) {
                val version = generation
                val value = latest
                try {
                    withContext(Dispatchers.IO) { if (value == null) store.delete() else store.write(value) }
                    if (version == generation) status = if (value == null) "Local draft cleared" else "Local draft saved on this device"
                } catch (_: Exception) { status = "Local draft could not be saved. Keep this editor open and save your record." }
                if (version == generation) { writing = false; break }
            }
        }
    }
    fun save(text: String) = write(text)
    fun clear() { recovered = null; write(null) }
}

/** Ordered writes retain the latest edit. Callers choose when to resume; committed data is never auto-replaced. */
@Composable
fun rememberLocalEditorDraft(key: String): LocalEditorDraft {
    val context = LocalContext.current.applicationContext
    val store = remember(key) { EditorDraftStore(context, key) }
    val state = remember(key) { LocalEditorDraft(store) }
    LaunchedEffect(key) {
        DraftWrites.flush()
        try { val recovered = withContext(Dispatchers.IO) { store.read() }; if (!state.edited) { state.recovered = recovered; state.status = if (recovered != null) "Local draft available" else "No unsaved local draft" } }
        catch (_: Exception) { state.status = "Local draft could not be read. Saved records are unchanged." }
    }
    return state
}
