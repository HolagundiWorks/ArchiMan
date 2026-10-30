package com.example.domain.archicalc

import java.util.Locale

/**
 * Stateless length / area / volume measure helper for Archi Calc.
 * Adapted from AcharyaOS `UnitConverter` (Apache-2.0) without mutable global state.
 */
object ArchiCalcMeasure {
    data class Result(
        val meters: Double,
        val squareMeters: Double,
        val cubicMeters: Double,
        val kind: Kind,
        val imperial: Boolean
    )

    enum class Kind { LENGTH, AREA, VOLUME }

    private val ftIn = Regex("""(\d+(?:\.\d+)?)'\s*-?\s*(\d+(?:\.\d+)?)?"?""")
    private val inchOnly = Regex("""(\d+(?:\.\d+)?)\"""")

    fun measure(expr: String): Result {
        val imperial = expr.contains('\'') || expr.contains('"')
        val meters = splitFactors(expr).map { parseLengthToMeters(it) }
        return when {
            meters.size >= 3 -> {
                val area = meters[0] * meters[1]
                Result(meters[0], area, area * meters[2], Kind.VOLUME, imperial)
            }
            meters.size == 2 -> Result(meters[0], meters[0] * meters[1], 0.0, Kind.AREA, imperial)
            else -> Result(meters.firstOrNull() ?: 0.0, 0.0, 0.0, Kind.LENGTH, imperial)
        }
    }

    fun formatGrid(expr: String): Map<String, String> {
        val m = measure(expr.ifBlank { "0" })
        fun n(v: Double) = String.format(Locale.US, "%.2f", v)
        return mapOf(
            "m" to n(m.meters),
            "m2" to n(m.squareMeters),
            "m3" to n(m.cubicMeters),
            "ft" to n(m.meters / 0.3048),
            "ft2" to n(m.squareMeters / 0.09290304),
            "ft3" to n(m.cubicMeters / 0.028316846592)
        )
    }

    private fun splitFactors(expr: String): List<String> {
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var depth = 0
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c == '(' -> { depth++; buf.append(c); i++ }
                c == ')' -> { depth--; buf.append(c); i++ }
                depth == 0 && (c == 'x' || c == 'X' || c == '*') -> {
                    val t = buf.toString().trim()
                    if (t.isNotEmpty()) out += t
                    buf.clear(); i++
                }
                else -> { buf.append(c); i++ }
            }
        }
        val t = buf.toString().trim()
        if (t.isNotEmpty()) out += t
        return if (out.isEmpty()) listOf(expr.trim().ifBlank { "0" }) else out
    }

    private fun parseLengthToMeters(token: String): Double {
        var s = token.replace("x", "*", ignoreCase = true)
        s = ftIn.replace(s) { m ->
            val ft = m.groupValues[1].toDouble()
            val inch = m.groupValues[2].toDoubleOrNull() ?: 0.0
            (ft * 0.3048 + inch * 0.0254).toString()
        }
        s = inchOnly.replace(s) { m -> (m.groupValues[1].toDouble() * 0.0254).toString() }
        return s.toDoubleOrNull() ?: ArchiCalcEngine.eval(s.ifBlank { "0" })
    }
}
