package com.holagundi.archiman.domain

data class MeasurementTextEdit(val text: String, val cursor: Int)

/** Selection-aware editing for the fixed measurement keyboard; never saves a measurement. */
object MeasurementTextEditing {
    fun insert(text: String, start: Int, end: Int, input: String, numeric: Boolean): MeasurementTextEdit {
        val from = minOf(start, end).coerceIn(0, text.length)
        val to = maxOf(start, end).coerceIn(from, text.length)
        val candidate = text.replaceRange(from, to, input)
        if (candidate.length > (if (numeric) 40 else 8000) || (numeric && !candidate.matches(Regex("[0-9]*(\\.[0-9]*)?")))) {
            return MeasurementTextEdit(text, end.coerceIn(0, text.length))
        }
        return MeasurementTextEdit(candidate, from + input.length)
    }

    fun backspace(text: String, start: Int, end: Int): MeasurementTextEdit {
        val from = minOf(start, end).coerceIn(0, text.length)
        val to = maxOf(start, end).coerceIn(from, text.length)
        if (from != to) return MeasurementTextEdit(text.removeRange(from, to), from)
        if (from == 0) return MeasurementTextEdit(text, 0)
        val previous = Character.offsetByCodePoints(text, from, -1)
        return MeasurementTextEdit(text.removeRange(previous, from), previous)
    }
}
