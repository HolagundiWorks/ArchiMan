package com.holagundi.archiman.domain

import org.junit.Assert.*
import org.junit.Test

class MeasurementTextEditingTest {
    @Test fun typingReplacesSelectionAndInsertsAtCursor() {
        assertEquals(MeasurementTextEdit("4.50", 3), MeasurementTextEditing.insert("4.00", 2, 3, "5", true))
        assertEquals(MeasurementTextEdit("Concrete slab", 9), MeasurementTextEditing.insert("Concreteslab", 8, 8, " ", false))
        assertEquals(MeasurementTextEdit("12.5", 4), MeasurementTextEditing.insert("0", 1, 0, "12.5", true))
    }
    @Test fun numericKeyboardRejectsSecondDecimalLettersAndOversizedInput() {
        assertEquals("4.5", MeasurementTextEditing.insert("4.5", 3, 3, ".", true).text)
        assertEquals("4", MeasurementTextEditing.insert("4", 1, 1, "a", true).text)
        assertEquals("4", MeasurementTextEditing.insert("4", 1, 1, "-", true).text)
        assertEquals("1".repeat(40), MeasurementTextEditing.insert("1".repeat(40), 40, 40, "2", true).text)
    }
    @Test fun backspaceHonorsSelectionAndDoesNotSplitAnEmoji() {
        assertEquals(MeasurementTextEdit("14", 1), MeasurementTextEditing.backspace("1234", 3, 1))
        assertEquals(MeasurementTextEdit("AB", 1), MeasurementTextEditing.backspace("A😀B", 3, 3))
        assertEquals(MeasurementTextEdit("", 0), MeasurementTextEditing.backspace("", 0, 0))
    }
}
