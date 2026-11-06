package com.holagundi.archiman.domain

/** Linear preflight before recursive JSON parsers allocate objects from imported data. */
object JsonSafety {
    fun check(text: String) {
        var quoted = false
        var escaped = false
        var depth = 0
        var containers = 0
        for (char in text) {
            if (quoted) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{', '[' -> {
                    depth++; containers++
                    require(depth <= 32 && containers <= 100_000) { "Imported JSON exceeds safe nesting or record-container limits." }
                }
                '}', ']' -> { depth--; require(depth >= 0) { "Invalid imported JSON structure." } }
            }
        }
        require(!quoted && depth == 0) { "Incomplete imported JSON structure." }
    }
}
