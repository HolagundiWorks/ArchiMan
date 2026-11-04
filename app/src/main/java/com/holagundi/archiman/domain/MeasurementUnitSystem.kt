package com.holagundi.archiman.domain

import kotlin.math.pow

enum class MeasurementUnitSystem { METRIC, IMPERIAL }

private const val FEET_PER_METRE = 3.280839895013123

object MeasurementUnitConverter {
    fun infer(unit: String): MeasurementUnitSystem {
        val normalized = unit.lowercase().replace("²", "2").replace("³", "3").replace("\\s+".toRegex(), "")
        return if (
            normalized.contains("ft") ||
            normalized.contains("feet") ||
            normalized.contains("foot") ||
            normalized.contains("sqft") ||
            normalized.contains("cuft")
        ) {
            MeasurementUnitSystem.IMPERIAL
        } else {
            MeasurementUnitSystem.METRIC
        }
    }

    fun powerFor(type: String): Int = when (type) {
        CalculationTypes.NOS -> 0
        CalculationTypes.RUNNING_LENGTH -> 1
        CalculationTypes.AREA, CalculationTypes.WALL_PLASTER -> 2
        CalculationTypes.VOLUME -> 3
        else -> 1
    }

    fun convert(value: Double, power: Int, from: MeasurementUnitSystem, to: MeasurementUnitSystem): Double {
        if (from == to || power == 0) return value
        val factor = FEET_PER_METRE.pow(power)
        return if (from == MeasurementUnitSystem.METRIC) value * factor else value / factor
    }

    fun quantity(value: Double, type: String, from: MeasurementUnitSystem, to: MeasurementUnitSystem): Double =
        convert(value, powerFor(type), from, to)

    fun unitFor(type: String, system: MeasurementUnitSystem, countUnit: String = "Nos"): String = when (type) {
        CalculationTypes.NOS -> countUnit
        CalculationTypes.RUNNING_LENGTH -> if (system == MeasurementUnitSystem.METRIC) "m" else "ft"
        CalculationTypes.AREA, CalculationTypes.WALL_PLASTER -> if (system == MeasurementUnitSystem.METRIC) "m²" else "ft²"
        CalculationTypes.VOLUME -> if (system == MeasurementUnitSystem.METRIC) "m³" else "ft³"
        else -> countUnit
    }
}
