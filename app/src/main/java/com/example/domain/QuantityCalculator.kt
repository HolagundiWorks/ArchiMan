package com.example.domain

import com.example.data.local.entity.CalculationType
import kotlin.math.round

data class MeasurementInput(
    val no: Double = 1.0,
    val length: Double = 0.0,
    val breadth: Double = 0.0,
    val height: Double = 0.0,
    val deduction: Double = 0.0
)

object QuantityCalculator {
    fun calculate(type: CalculationType, input: MeasurementInput): Double {
        val no = input.no.coerceAtLeast(0.0)
        val raw = when (type) {
            CalculationType.NOS -> no
            CalculationType.RUNNING_LENGTH -> no * input.length.coerceAtLeast(0.0)
            CalculationType.AREA -> no * input.length.coerceAtLeast(0.0) * input.breadth.coerceAtLeast(0.0)
            CalculationType.WALL_PLASTER ->
                (no * input.length.coerceAtLeast(0.0) * input.height.coerceAtLeast(0.0)) - input.deduction.coerceAtLeast(0.0)
            CalculationType.VOLUME ->
                no * input.length.coerceAtLeast(0.0) * input.breadth.coerceAtLeast(0.0) * input.height.coerceAtLeast(0.0)
        }
        return round(raw.coerceAtLeast(0.0) * 100.0) / 100.0
    }
}
