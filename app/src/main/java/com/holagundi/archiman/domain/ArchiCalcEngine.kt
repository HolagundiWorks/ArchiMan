package com.holagundi.archiman.domain

import java.util.Locale
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object ArchiCalcEngine {
    fun evaluate(expr: String): String = try {
        val normalized = normalize(expr)
        if (normalized.isEmpty()) "0.00" else format(evalSimple(normalized))
    } catch (_: Exception) {
        "Error"
    }

    fun eval(expr: String): Double = try {
        val normalized = normalize(expr)
        if (normalized.isEmpty()) 0.0 else evalSimple(normalized)
    } catch (_: Exception) {
        0.0
    }

    fun applyFn(value: String, fn: String): String {
        val parsed = value.toDoubleOrNull() ?: evaluate(value).toDoubleOrNull()
        if (parsed == null) return "Error"
        val result = when (fn) {
            "sin" -> sin(parsed * Math.PI / 180.0)
            "cos" -> cos(parsed * Math.PI / 180.0)
            "tan" -> tan(parsed * Math.PI / 180.0)
            "log" -> log10(parsed)
            "ln" -> ln(parsed)
            "sqrt" -> sqrt(parsed)
            "x2" -> parsed * parsed
            "1/x" -> 1.0 / parsed
            "+/-" -> -parsed
            else -> parsed
        }
        return format(result)
    }

    fun format(result: Double): String {
        if (result.isNaN() || result.isInfinite()) return "Error"
        return String.format(Locale.US, "%.2f", result)
    }

    fun normalize(expr: String): String = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace("−", "-")
        .replace(Regex("pi", RegexOption.IGNORE_CASE), Math.PI.toString())
        .replace("%", "/100")
        .trim()

    private fun evalSimple(expr: String): Double {
        val values = ArrayDeque<Double>()
        val ops = ArrayDeque<String>()

        fun precedence(op: String) = when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            "^" -> 3
            else -> 0
        }

        fun apply(op: String) {
            val b = values.removeLastOrNull() ?: 0.0
            val a = values.removeLastOrNull() ?: 0.0
            values.addLast(
                when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else 0.0
                    "^" -> a.pow(b)
                    else -> 0.0
                },
            )
        }

        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    values.addLast(expr.substring(start, i).toDouble())
                }
                c == '(' -> {
                    ops.addLast(c.toString())
                    i++
                }
                c == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != "(") apply(ops.removeLast())
                    if (ops.isNotEmpty() && ops.last() == "(") ops.removeLast()
                    i++
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == 'x' -> {
                    val op = if (c == 'x') "*" else c.toString()
                    val prev = if (i == 0) null else expr[i - 1]
                    val unary = c == '-' && (prev == null || prev in charArrayOf('+', '-', '*', '/', '(', '^', 'x'))
                    if (unary) {
                        i++
                        val start = i
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                        val number = expr.substring(start, i).toDoubleOrNull() ?: 0.0
                        values.addLast(-number)
                    } else {
                        while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(op)) apply(ops.removeLast())
                        ops.addLast(op)
                        i++
                    }
                }
                else -> i++
            }
        }
        while (ops.isNotEmpty()) apply(ops.removeLast())
        return values.lastOrNull() ?: 0.0
    }
}

data class MeasureReading(
    val meters: Double,
    val squareMeters: Double,
    val cubicMeters: Double,
    val kind: String,
    val imperial: Boolean,
)

object ArchiCalcMeasure {
    fun measure(expr: String): MeasureReading {
        val imperial = expr.contains("'") || expr.contains("\"")
        val meters = splitFactors(expr).map { parseLengthToMeters(it) }
        return when {
            meters.size >= 3 -> {
                val area = meters[0] * meters[1]
                MeasureReading(meters[0], area, area * meters[2], "VOLUME", imperial)
            }
            meters.size == 2 -> MeasureReading(meters[0], meters[0] * meters[1], 0.0, "AREA", imperial)
            else -> MeasureReading(meters.firstOrNull() ?: 0.0, 0.0, 0.0, "LENGTH", imperial)
        }
    }

    fun formatGrid(expr: String): Map<String, String> {
        val reading = measure(expr.trim().ifEmpty { "0" })
        fun n(value: Double) = if (value.isNaN()) "0.00" else String.format(Locale.US, "%.2f", value)
        return mapOf(
            "m" to n(reading.meters),
            "m2" to n(reading.squareMeters),
            "m3" to n(reading.cubicMeters),
            "ft" to n(reading.meters / 0.3048),
            "ft2" to n(reading.squareMeters / 0.09290304),
            "ft3" to n(reading.cubicMeters / 0.028316846592),
        )
    }

    fun splitFactors(expr: String): List<String> {
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var depth = 0
        for (c in expr) {
            when {
                c == '(' -> {
                    depth++
                    buf.append(c)
                }
                c == ')' -> {
                    depth--
                    buf.append(c)
                }
                depth == 0 && (c == 'x' || c == 'X' || c == '*') -> {
                    val token = buf.toString().trim()
                    if (token.isNotEmpty()) out += token
                    buf.clear()
                }
                else -> buf.append(c)
            }
        }
        val tail = buf.toString().trim()
        if (tail.isNotEmpty()) out += tail
        return out.ifEmpty { listOf(expr.trim().ifEmpty { "0" }) }
    }

    fun parseLengthToMeters(token: String): Double {
        var s = token.replace(Regex("x", RegexOption.IGNORE_CASE), "*")
        val ftIn = Regex("""(\d+(?:\.\d+)?)'\s*-?\s*(\d+(?:\.\d+)?)?""" + "\"?")
        s = ftIn.replace(s) { match ->
            val feet = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val inch = match.groupValues[2].toDoubleOrNull() ?: 0.0
            (feet * 0.3048 + inch * 0.0254).toString()
        }
        val inches = Regex("""(\d+(?:\.\d+)?)\"""")
        s = inches.replace(s) { match ->
            ((match.groupValues[1].toDoubleOrNull() ?: 0.0) * 0.0254).toString()
        }
        return parseFloatPrefix(s) ?: ArchiCalcEngine.eval(s.trim().ifEmpty { "0" })
    }

    private fun parseFloatPrefix(value: String): Double? {
        val match = Regex("""^[+-]?\d+(?:\.\d+)?""").find(value.trim()) ?: return null
        return match.value.toDoubleOrNull()
    }
}
