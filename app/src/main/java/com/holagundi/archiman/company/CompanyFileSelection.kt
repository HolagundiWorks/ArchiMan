package com.holagundi.archiman.company

/** A pending picker request holds only a public origin. Results live in RAM, never saved state. */
class CompanyFileSelection<T> {
    private var origin: String? = null
    private var pending: Pair<String, T>? = null

    @Synchronized fun begin(publicOrigin: String) {
        origin = publicOrigin
        pending = null
    }
    @Synchronized fun complete(value: T?) {
        val expected = origin
        origin = null
        pending = if (expected != null && value != null) expected to value else null
    }
    @Synchronized fun take(): Pair<String, T>? = pending.also { pending = null }
    @Synchronized fun hasPending(): Boolean = pending != null
}
