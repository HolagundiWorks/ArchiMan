package com.holagundi.archiman.domain

import kotlin.math.abs

object WorkCatalog {
    fun codeFor(workType: String, itemName: String): String {
        val trade = workType.filter { it.isLetterOrDigit() }.uppercase().take(4).padEnd(4, 'X')
        val item = itemName.filter { it.isLetterOrDigit() }.uppercase().take(8).padEnd(3, 'X')
        var hash = 0
        val norm = itemName.trim().lowercase()
        for (ch in norm) {
            hash = (hash shl 5) - hash + ch.code
        }
        val stable = if (hash == Int.MIN_VALUE) 0 else abs(hash)
        val suffix = stable.toString(16).uppercase().takeLast(4).padStart(4, '0')
        return "$trade-$item-$suffix"
    }

    fun classify(contractorType: String, itemName: String): String {
        val name = itemName.trim().lowercase()
        fun has(vararg needles: String) = needles.any { name.contains(it) }
        return when {
            has("excavat", "earth", "soil") -> "Earthwork"
            has("rcc", "pcc", "concrete", "reinforcement", "shuttering", "formwork") -> "Concrete & Structure"
            has("brick", "block", "masonry") -> "Masonry"
            has("plaster", "putty", "paint", "primer", "polish", "texture") -> "Finishes"
            has("tile", "floor", "marble", "granite", "cladding", "skirting", "dado", "grout") -> "Flooring & Cladding"
            has("pipe", "drain", "trap", "basin", "fitting", "tank", "wc ", "pressure test") -> "Plumbing Works"
            has("wire", "cable", "point", "switch", "earthing", "distribution", "conduit") -> "Electrical Works"
            has("door", "window", "wardrobe", "kitchen", "wood", "hardware") -> "Joinery & Carpentry"
            has("waterproof", "joint treatment") -> "Waterproofing"
            has("duct", "hvac", "indoor unit", "outdoor unit", "diffuser", "insulation") -> "HVAC Works"
            has("fire", "sprinkler", "hydrant", "hose reel") -> "Fire Protection"
            has("steel", "railing", "grill", "weld", "truss", "fabricat") -> "Fabrication"
            has("lawn", "plant", "paver", "kerb", "irrigation") -> "Landscaping"
            else -> {
                val cleanType = contractorType.trim().ifEmpty { "General" }
                if (cleanType.endsWith("Works")) cleanType else "$cleanType Works"
            }
        }
    }
}
