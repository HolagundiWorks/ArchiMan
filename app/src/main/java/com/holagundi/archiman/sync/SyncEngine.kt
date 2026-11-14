package com.holagundi.archiman.sync

/** What one sync pass did. */
data class SyncResult(
    val pushed: Int,
    val merged: Int,
    val rejectedOlder: Int,
    val forbidden: Int,
    val pulled: Int,
    val deferred: Int,
    val skippedLocalChanges: Int,
    val complete: Boolean,
)

/**
 * One company sync pass: push local changes, then pull the cloud's. Push first, so the cloud settles conflicts before a pull overwrites anything
 * (the same order as MynaHub's HubSyncEngine). The store decides what changed and applies what arrives; this class only moves records.
 */
class SyncEngine(private val store: SyncStore, private val api: HubApi) {
    fun runOnce(): SyncResult {
        val st = store.status()
        check(st.enabled && st.cloudCompany != null) { "Cloud sync is not set up on this phone." }
        val company = st.cloudCompany
        var pushed = 0
        var merged = 0
        var older = 0
        var forbidden = 0
        var pulled = 0
        var deferred = 0
        var skipped = 0
        var complete = true

        for (round in 0 until MAX_ROUNDS) {
            val batch = store.collectPush(BATCH)
            if (batch.isEmpty()) break
            val results = api.push(company, batch)
            val acks = ArrayList<Ack>()
            var progressed = false
            for (i in 0 until results.length()) {
                val res = results.getJSONObject(i)
                val sent = batch.firstOrNull { it.syncId.equals(res.getString("sync_id"), ignoreCase = true) } ?: continue
                val status = res.getString("status")
                when (status) {
                    "created", "updated" -> { pushed++; progressed = true }
                    "merged" -> { merged++; progressed = true }
                    "rejected_older" -> { older++; progressed = true }
                    else -> forbidden++
                }
                acks += Ack(sent.kind, sent.syncId, sent.v, status, res.optLong("rev"))
            }
            store.ack(acks)
            if (!progressed || batch.size < BATCH) {
                complete = progressed
                break
            }
        }

        for (round in 0 until MAX_ROUNDS) {
            val from = store.status().lastPulledSeq
            val rows = api.pull(company, from, PULL_LIMIT)
            if (rows.length() == 0) break
            val res = store.applyPull(from, SyncStore.remotesFrom(rows))
            pulled += res.applied
            deferred = res.deferred
            skipped += res.skippedLocalChanges
            if (rows.length() < PULL_LIMIT || res.nextSeq <= from) {
                if (deferred > 0) complete = false
                break
            }
        }
        return SyncResult(pushed, merged, older, forbidden, pulled, deferred, skipped, complete && forbidden == 0)
    }

    companion object {
        const val BATCH = 200
        const val PULL_LIMIT = 500
        const val MAX_ROUNDS = 50
    }
}
