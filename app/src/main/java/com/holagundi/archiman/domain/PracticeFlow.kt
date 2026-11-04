package com.holagundi.archiman.domain

data class EstimateLine(
    val itemName: String,
    val unit: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double,
)

object EstimateBuilder {
    fun lines(quantities: Map<String, Pair<Double, String>>, rates: Map<String, Double>): List<EstimateLine> {
        return quantities.map { (name, qtyUnit) ->
            val rate = rates[name.lowercase()] ?: 0.0
            EstimateLine(name, qtyUnit.second, qtyUnit.first, rate, kotlin.math.round(qtyUnit.first * rate * 100.0) / 100.0)
        }.sortedBy { it.itemName }
    }
}

object ProjectPath {
    const val ASSIGN_STAGES = "Assign the standard stages"
    const val BILL_STAGE = "Bill a stage"
    const val ISSUE_INVOICE = "Review and issue the draft invoice"
    const val RECORD_PAYMENT = "Record payment"
    const val WAIT = "Wait for the next stage"

    fun next(hasStages: Boolean, hasOpenStage: Boolean, hasDraftInvoice: Boolean, hasUnpaidInvoice: Boolean): String = when {
        !hasStages -> ASSIGN_STAGES
        hasDraftInvoice -> ISSUE_INVOICE
        hasUnpaidInvoice -> RECORD_PAYMENT
        hasOpenStage -> BILL_STAGE
        else -> WAIT
    }
}

object StandardStages {
    val TITLES = listOf(
        "Concept design",
        "Preliminary drawings",
        "Working drawings",
        "Construction",
        "Completion",
    )
}

object CoreOpCatalog {
    val RCC = listOf("Columns", "Beams", "Pedestals", "Lintels", "Slabs", "Footings", "Retaining walls", "Stairs")
    val CIVIL = listOf(
        "Masonry walls",
        "Plastering",
        "PCC bed",
        "Earthwork",
        "Size stone",
        "Shuttering",
        "Flooring",
        "Painting",
        "Waterproofing",
        "DPC",
        "Coping",
        "Screed",
        "VDF",
        "Skirting",
        "Parapet",
        "Plinth protection",
        "Doors",
        "Windows",
    )
}
