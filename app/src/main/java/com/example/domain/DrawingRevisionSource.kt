package com.example.domain

/**
 * Why a drawing revision happened. Recorded alongside the existing severity
 * scale (NORMAL/ATTENTION/CRITICAL) so a practice can see, across every
 * project, whether its revision churn is client-driven or a sign of
 * internal or site-coordination trouble — without any commercial or AI
 * involvement (see docs/ARCHITECT_PRACTICE_SCOPE.md).
 */
object DrawingRevisionSource {
    const val CLIENT_REQUEST = "CLIENT_REQUEST"
    const val INTERNAL_CORRECTION = "INTERNAL_CORRECTION"
    const val SITE_QUERY = "SITE_QUERY"
    const val SCOPE_CHANGE = "SCOPE_CHANGE"

    val ALL = listOf(CLIENT_REQUEST, INTERNAL_CORRECTION, SITE_QUERY, SCOPE_CHANGE)
}

enum class RevisionRiskBand { LOW, MEDIUM, HIGH }

data class RevisionRiskSummary(
    val totalRevisions: Int,
    val criticalRevisions: Int,
    val internalCorrections: Int,
    val scopeChanges: Int,
    val healthScore: Int,
    val riskBand: RevisionRiskBand
)

/**
 * A revision recorded for reasons the practice doesn't control (client
 * request, a site query) is normal business. One recorded as an internal
 * correction, or at critical severity, is what should worry a principal —
 * weighted the same way AORMS scores its own revision/decision health.
 */
fun computeRevisionRisk(revisions: List<Pair<String, String>>): RevisionRiskSummary {
    val total = revisions.size
    val critical = revisions.count { (_, severity) -> severity == "CRITICAL" }
    val internalCorrections = revisions.count { (source, _) -> source == DrawingRevisionSource.INTERNAL_CORRECTION }
    val scopeChanges = revisions.count { (source, _) -> source == DrawingRevisionSource.SCOPE_CHANGE }
    val healthScore = if (total > 0) {
        (100 - ((critical * 2 + internalCorrections) * 50.0 / total)).toInt().coerceAtLeast(0)
    } else 100
    val band = when {
        healthScore >= 80 -> RevisionRiskBand.LOW
        healthScore >= 55 -> RevisionRiskBand.MEDIUM
        else -> RevisionRiskBand.HIGH
    }
    return RevisionRiskSummary(total, critical, internalCorrections, scopeChanges, healthScore, band)
}
