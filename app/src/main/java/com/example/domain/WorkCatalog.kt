package com.example.domain

object WorkCatalog {
    fun codeFor(workType: String, itemName: String): String {
        val trade = workType.filter(Char::isLetterOrDigit).uppercase().take(4).padEnd(4, 'X')
        val item = itemName.filter(Char::isLetterOrDigit).uppercase().take(8).padEnd(3, 'X')
        val suffix = normalizeName(itemName).hashCode().toUInt().toString(16).uppercase().takeLast(4).padStart(4, '0')
        return "$trade-$item-$suffix"
    }

    fun classify(contractorType: String, itemName: String): String {
        val name = itemName.trim().lowercase()
        return when {
            name.containsAny("excavat", "earth", "soil") -> "Earthwork"
            name.containsAny("rcc", "pcc", "concrete", "reinforcement", "shuttering", "formwork") -> "Concrete & Structure"
            name.containsAny("brick", "block", "masonry") -> "Masonry"
            name.containsAny("plaster", "putty", "paint", "primer", "polish", "texture") -> "Finishes"
            name.containsAny("tile", "floor", "marble", "granite", "cladding", "skirting", "dado", "grout") -> "Flooring & Cladding"
            name.containsAny("pipe", "drain", "trap", "basin", "fitting", "tank", "wc ", "pressure test") -> "Plumbing Works"
            name.containsAny("wire", "cable", "point", "switch", "earthing", "distribution", "conduit") -> "Electrical Works"
            name.containsAny("door", "window", "wardrobe", "kitchen", "wood", "hardware") -> "Joinery & Carpentry"
            name.containsAny("waterproof", "joint treatment") -> "Waterproofing"
            name.containsAny("duct", "hvac", "indoor unit", "outdoor unit", "diffuser", "insulation") -> "HVAC Works"
            name.containsAny("fire", "sprinkler", "hydrant", "hose reel") -> "Fire Protection"
            name.containsAny("steel", "railing", "grill", "weld", "truss", "fabricat") -> "Fabrication"
            name.containsAny("lawn", "plant", "paver", "kerb", "irrigation") -> "Landscaping"
            else -> contractorType.trim().ifBlank { "General" }.let { if (it.endsWith("Works")) it else "$it Works" }
        }
    }

    fun normalizeName(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase()

    private fun String.containsAny(vararg needles: String): Boolean = needles.any(::contains)
}
