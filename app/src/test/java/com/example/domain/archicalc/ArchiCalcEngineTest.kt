package com.example.domain.archicalc

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiCalcEngineTest {
    @Test fun evaluatesBasicArithmetic() = assertEquals("7.00", ArchiCalcEngine.evaluate("3+4"))

    @Test fun evaluatesMultiplicationAndDivision() = assertEquals("6.00", ArchiCalcEngine.evaluate("12÷2"))

    @Test fun evaluatesParenthesesAndPower() = assertEquals("16.00", ArchiCalcEngine.evaluate("(2+2)^2"))

    @Test fun appliesSquareRoot() = assertEquals("3.00", ArchiCalcEngine.applyFn("9", "sqrt"))

    @Test fun returnsErrorOnBadInput() = assertEquals("Error", ArchiCalcEngine.evaluate("2++"))
}
