package com.holagundi.archiman.domain

import java.util.Locale

data class AbstractRow(
    val itemName: String,
    val unit: String,
    val quantity: Double,
    val approved: Boolean,
)

data class AbstractLine(
    val itemName: String,
    val unit: String,
    val totalQuantity: Double,
    val rowCount: Int,
)

object QuantityAbstract {
    fun summarize(rows: List<AbstractRow>): List<AbstractLine> {
        return rows
            .filter { it.approved }
            .groupBy { it.itemName to it.unit }
            .map { (key, group) ->
                AbstractLine(
                    itemName = key.first,
                    unit = key.second,
                    totalQuantity = group.sumOf { it.quantity },
                    rowCount = group.size,
                )
            }
            .sortedBy { it.itemName }
    }

    fun csv(projectName: String, projectCode: String, lines: List<AbstractLine>): String {
        val body = buildString {
            appendLine("QUANTITY ONLY")
            appendLine("Quantity abstract,$projectName,$projectCode")
            appendLine("Work item,Unit,Total quantity,Approved rows")
            lines.forEach { line ->
                appendLine("${csv(line.itemName)},${csv(line.unit)},${"%.2f".format(Locale.US, line.totalQuantity)},${line.rowCount}")
            }
        }
        return body
    }

    private fun csv(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
}
