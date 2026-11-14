package com.holagundi.archiman.domain

/** Shared priority levels for tasks and site issues (maps to tile alert stripes). */
object WorkPriority {
    const val NORMAL = "NORMAL"
    const val ATTENTION = "ATTENTION"
    const val CRITICAL = "CRITICAL"

    val all = listOf(NORMAL, ATTENTION, CRITICAL)

    fun normalize(value: String): String = when (value.uppercase()) {
        CRITICAL, "HIGH", "URGENT", "P1" -> CRITICAL
        ATTENTION, "MEDIUM", "WARNING", "P2" -> ATTENTION
        else -> NORMAL
    }

    fun label(value: String): String = when (normalize(value)) {
        CRITICAL -> "Critical"
        ATTENTION -> "Attention"
        else -> "Normal"
    }
}
