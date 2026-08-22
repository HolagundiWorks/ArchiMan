package com.example.domain

import com.example.data.local.entity.ItemMasterEntity

data class WorkItemDuplicateCandidate(
    val first: ItemMasterEntity,
    val second: ItemMasterEntity,
    val similarity: Int
)

object WorkItemDuplicateDetector {
    fun find(items: List<ItemMasterEntity>, threshold: Double = 0.78): List<WorkItemDuplicateCandidate> =
        items.asSequence()
            .filter { it.isActive }
            .groupBy { WorkCatalog.normalizeName(it.workType) }
            .values
            .asSequence()
            .flatMap { group ->
                group.indices.asSequence().flatMap { left ->
                    (left + 1 until group.size).asSequence().mapNotNull { right ->
                        val first = group[left]
                        val second = group[right]
                        if (first.calculationType != second.calculationType ||
                            normalizeUnit(first.unit) != normalizeUnit(second.unit)
                        ) return@mapNotNull null
                        val score = similarity(first.name, second.name)
                        if (score >= threshold) WorkItemDuplicateCandidate(first, second, (score * 100).toInt()) else null
                    }
                }
            }
            .sortedByDescending { it.similarity }
            .toList()

    internal fun similarity(first: String, second: String): Double {
        val left = tokens(first)
        val right = tokens(second)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val tokenScore = left.intersect(right).size.toDouble() / left.union(right).size
        val a = left.sorted().joinToString(" ")
        val b = right.sorted().joinToString(" ")
        val editScore = 1.0 - levenshtein(a, b).toDouble() / maxOf(a.length, b.length)
        return (tokenScore * 0.6) + (editScore * 0.4)
    }

    private fun tokens(value: String): Set<String> = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.length > 1 && it !in setOf("of", "and", "for", "the") }
        .map { if (it.length > 3 && it.endsWith('s') && !it.endsWith("ss")) it.dropLast(1) else it }
        .toSet()

    private fun normalizeUnit(value: String): String = when (value.trim().lowercase()) {
        "m2", "m²", "sqm", "sq m" -> "sqm"
        "m3", "m³", "cum", "cu m" -> "cum"
        "m", "rmt", "rm" -> "rmt"
        "no", "nos", "number", "numbers" -> "nos"
        else -> value.trim().lowercase()
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        a.forEachIndexed { i, ca ->
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            b.forEachIndexed { j, cb ->
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (ca == cb) 0 else 1)
            }
            previous = current
        }
        return previous[b.length]
    }
}
