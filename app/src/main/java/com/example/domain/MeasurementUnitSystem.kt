package com.example.domain

import com.example.data.local.entity.CalculationType
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

enum class MeasurementUnitSystem(val label: String) {
    METRIC("Metric"),
    IMPERIAL("Imperial");

    companion object {
        fun fromStored(value: String?): MeasurementUnitSystem =
            entries.firstOrNull { it.name == value } ?: METRIC
    }
}

object MeasurementUnitConverter {
    private const val FEET_PER_METRE = 3.280839895013123

    fun infer(unit: String): MeasurementUnitSystem {
        val normalized = unit.lowercase()
            .replace("²", "2")
            .replace("³", "3")
            .replace(" ", "")
        return if (
            normalized.contains("ft") || normalized.contains("feet") ||
            normalized.contains("foot") || normalized.contains("sqft") ||
            normalized.contains("cuft")
        ) MeasurementUnitSystem.IMPERIAL else MeasurementUnitSystem.METRIC
    }

    fun linear(value: Double, from: MeasurementUnitSystem, to: MeasurementUnitSystem): Double =
        convert(value, power = 1, from = from, to = to)

    fun quantity(value: Double, type: CalculationType, from: MeasurementUnitSystem, to: MeasurementUnitSystem): Double =
        convert(value, powerFor(type), from, to)

    fun deduction(value: Double, type: CalculationType, from: MeasurementUnitSystem, to: MeasurementUnitSystem): Double =
        convert(value, if (type == CalculationType.WALL_PLASTER) 2 else powerFor(type), from, to)

    fun unitFor(type: CalculationType, system: MeasurementUnitSystem, countUnit: String = "Nos"): String = when (type) {
        CalculationType.NOS -> countUnit
        CalculationType.RUNNING_LENGTH -> if (system == MeasurementUnitSystem.METRIC) "m" else "ft"
        CalculationType.AREA, CalculationType.WALL_PLASTER -> if (system == MeasurementUnitSystem.METRIC) "m²" else "ft²"
        CalculationType.VOLUME -> if (system == MeasurementUnitSystem.METRIC) "m³" else "ft³"
    }

    fun convertText(
        text: String,
        power: Int,
        from: MeasurementUnitSystem,
        to: MeasurementUnitSystem
    ): String {
        if (text.isBlank() || from == to) return text
        val value = text.toDoubleOrNull() ?: return text
        return format(convert(value, power, from, to))
    }

    fun powerFor(type: CalculationType): Int = when (type) {
        CalculationType.NOS -> 0
        CalculationType.RUNNING_LENGTH -> 1
        CalculationType.AREA, CalculationType.WALL_PLASTER -> 2
        CalculationType.VOLUME -> 3
    }

    private fun convert(
        value: Double,
        power: Int,
        from: MeasurementUnitSystem,
        to: MeasurementUnitSystem
    ): Double {
        if (from == to || power == 0) return value
        val factor = FEET_PER_METRE.pow(power)
        return if (from == MeasurementUnitSystem.METRIC) value * factor else value / factor
    }

    private fun format(value: Double): String = BigDecimal.valueOf(value)
        .setScale(4, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
