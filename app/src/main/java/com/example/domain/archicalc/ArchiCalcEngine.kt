package com.example.domain.archicalc

import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Expression evaluator for the embedded Archi Calc surface.
 *
 * Logic adapted from Holagundi Consulting Works' AcharyaOS / Paperdesk calculator
 * (`CalculatorEngine`), Apache License 2.0 — see THIRD_PARTY_NOTICES.md.
 * HolagundiWorks/architect-calculator was not available as a cloneable module;
 * this is the in-app embed path that preserves quantity-only site maths.
 */
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
        val v = value.toDoubleOrNull() ?: evaluate(value).toDoubleOrNull() ?: return "Error"
        val r = when (fn) {
            "sin" -> sin(Math.toRadians(v))
            "cos" -> cos(Math.toRadians(v))
            "tan" -> tan(Math.toRadians(v))
            "log" -> log10(v)
            "ln" -> ln(v)
            "sqrt" -> sqrt(v)
            "x2" -> v * v
            "1/x" -> 1.0 / v
            "+/-" -> -v
            else -> v
        }
        return format(r)
    }

    fun format(result: Double): String {
        if (result.isNaN() || result.isInfinite()) return "Error"
        return String.format(Locale.US, "%.2f", result)
    }

    private fun normalize(expr: String): String = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace("−", "-")
        .replace("pi", PI.toString(), ignoreCase = true)
        .replace("%", "/100")
        .trim()

    private fun evalSimple(expr: String): Double {
        val values = ArrayDeque<Double>()
        val ops = ArrayDeque<Char>()
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
                    ops.addLast(c); i++
                }
                c == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != '(') apply(values, ops.removeLast())
                    if (ops.isNotEmpty() && ops.last() == '(') ops.removeLast()
                    i++
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == 'x' -> {
                    val op = if (c == 'x') '*' else c
                    if (c == '-' && (i == 0 || expr[i - 1] in "+-*/(^x")) {
                        i++
                        val start = i
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                        values.addLast(-expr.substring(start, i).toDouble())
                    } else {
                        while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(op)) {
                            apply(values, ops.removeLast())
                        }
                        ops.addLast(op)
                        i++
                    }
                }
                else -> throw IllegalArgumentException("bad char")
            }
        }
        while (ops.isNotEmpty()) apply(values, ops.removeLast())
        return values.last()
    }

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        '^' -> 3
        else -> 0
    }

    private fun apply(values: ArrayDeque<Double>, op: Char) {
        val b = values.removeLast()
        val a = values.removeLast()
        values.addLast(
            when (op) {
                '+' -> a + b
                '-' -> a - b
                '*' -> a * b
                '/' -> a / b
                '^' -> a.pow(b)
                else -> throw IllegalArgumentException()
            }
        )
    }
}
