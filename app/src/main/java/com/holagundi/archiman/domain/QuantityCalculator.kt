package com.holagundi.archiman.domain

import kotlin.math.max
import kotlin.math.round

object CalculationTypes {
    const val NOS = "NOS"
    const val RUNNING_LENGTH = "RUNNING_LENGTH"
    const val AREA = "AREA"
    const val WALL_PLASTER = "WALL_PLASTER"
    const val VOLUME = "VOLUME"
}

data class MeasurementInput(
    val no: Double = 1.0,
    val length: Double = 0.0,
    val breadth: Double = 0.0,
    val height: Double = 0.0,
    val deduction: Double = 0.0,
)

object QuantityCalculator {
    fun calculate(type: String, input: MeasurementInput): Double {
        val no = max(0.0, input.no)
        val length = max(0.0, input.length)
        val breadth = max(0.0, input.breadth)
        val height = max(0.0, input.height)
        val deduction = max(0.0, input.deduction)
        val raw = when (type) {
            CalculationTypes.NOS -> no
            CalculationTypes.RUNNING_LENGTH -> no * length
            CalculationTypes.AREA -> no * length * breadth
            CalculationTypes.WALL_PLASTER -> no * length * height - deduction
            CalculationTypes.VOLUME -> no * length * breadth * height
            else -> no * length * breadth
        }
        return round(max(0.0, raw) * 100.0) / 100.0
    }
}
