package com.holagundi.archiman.domain

object WorkItemLibrary {
    val formulas = linkedMapOf(CalculationTypes.VOLUME to "Nos × Length × Breadth × Height", CalculationTypes.WALL_PLASTER to "Nos × Length × Height − Deduction", CalculationTypes.AREA to "Nos × Length × Breadth", CalculationTypes.RUNNING_LENGTH to "Nos × Length", CalculationTypes.NOS to "Nos")
    val defaults = listOf(Triple("Concrete", "Concrete slab", CalculationTypes.VOLUME), Triple("Concrete", "Concrete footing", CalculationTypes.VOLUME), Triple("Earthwork", "Excavation", CalculationTypes.VOLUME), Triple("Masonry", "Brick masonry", CalculationTypes.VOLUME), Triple("Finishes", "Wall plaster", CalculationTypes.WALL_PLASTER), Triple("Finishes", "Wall painting", CalculationTypes.WALL_PLASTER), Triple("Flooring", "Floor tiles", CalculationTypes.AREA), Triple("Flooring", "Floor screed", CalculationTypes.AREA), Triple("Roofing", "Waterproofing", CalculationTypes.AREA), Triple("Finishes", "Skirting", CalculationTypes.RUNNING_LENGTH), Triple("Services", "Fixture count", CalculationTypes.NOS))
    fun validate(type: String, unit: String) {
        require(type in formulas) { "Choose a supported formula." }
        val accepted = when (type) { CalculationTypes.VOLUME -> setOf("m³", "ft³"); CalculationTypes.AREA, CalculationTypes.WALL_PLASTER -> setOf("m²", "ft²"); CalculationTypes.RUNNING_LENGTH -> setOf("m", "ft"); else -> setOf("Nos") }
        require(unit in accepted) { "Unit does not match formula dimensions." }
    }
}
