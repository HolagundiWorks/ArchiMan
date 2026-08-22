package com.example.domain

import com.example.data.local.entity.CalculationType

enum class MeasurementRowStatus { EMPTY, INCOMPLETE, READY }

data class MeasurementRowValidationInput(
    val description: String,
    val no: String,
    val length: String,
    val breadth: String,
    val height: String,
    val deduction: String = "0",
    val hasPhoto: Boolean = false,
    val remarks: String = ""
)

data class MeasurementRowValidation(val status: MeasurementRowStatus, val message: String? = null)

object MeasurementRowValidator {
    fun validate(type: CalculationType, input: MeasurementRowValidationInput): MeasurementRowValidation {
        if (isEmpty(input)) return MeasurementRowValidation(MeasurementRowStatus.EMPTY)
        if (input.description.isBlank()) return incomplete("Member description is required")
        val no = positive(input.no) ?: return incomplete("No. must be greater than zero")
        val length = if (type != CalculationType.NOS) positive(input.length) ?: return incomplete("Length is required") else 1.0
        val breadth = if (type == CalculationType.AREA || type == CalculationType.VOLUME) positive(input.breadth) ?: return incomplete("Breadth is required") else 1.0
        val height = if (type == CalculationType.WALL_PLASTER || type == CalculationType.VOLUME) positive(input.height) ?: return incomplete("Height is required") else 1.0
        val deduction = input.deduction.ifBlank { "0" }.toDoubleOrNull() ?: return incomplete("Deduction must be a valid number")
        if (deduction < 0) return incomplete("Deduction cannot be negative")
        val gross = no * length * breadth * height
        if (deduction >= gross) return incomplete("Deduction must be less than gross quantity")
        return MeasurementRowValidation(MeasurementRowStatus.READY)
    }

    private fun isEmpty(input: MeasurementRowValidationInput): Boolean = input.description.isBlank() &&
        input.length.isBlank() && input.breadth.isBlank() && input.height.isBlank() &&
        (input.no.isBlank() || input.no.toDoubleOrNull() == 1.0) &&
        (input.deduction.isBlank() || input.deduction.toDoubleOrNull() == 0.0) &&
        !input.hasPhoto && input.remarks.isBlank()

    private fun positive(value: String): Double? = value.toDoubleOrNull()?.takeIf { it > 0 }
    private fun incomplete(message: String) = MeasurementRowValidation(MeasurementRowStatus.INCOMPLETE, message)
}
