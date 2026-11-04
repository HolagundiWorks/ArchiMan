package com.holagundi.archiman.domain

import java.time.Instant
import java.time.ZoneId

data class MeasurementDraft(
    val projectId: Long,
    val projectCode: String,
    val floorId: Long,
    val floorName: String,
    val roomId: Long?,
    val roomName: String?,
    val contractorId: Long,
    val contractorName: String,
    val itemId: Long,
    val itemName: String,
    val unit: String,
    val calculationType: String,
    val description: String,
    val location: String,
    val remarks: String,
    val length: Double,
    val width: Double,
    val height: Double,
    val nos: Double,
    val deduction: Double,
)

data class SheetExportRow(
    val description: String,
    val location: String,
    val length: Double,
    val width: Double,
    val height: Double,
    val nos: Double,
    val deduction: Double,
    val quantity: Double,
    val remarks: String,
    val dateMillis: Long,
)

data class SheetExport(
    val projectName: String,
    val projectCode: String,
    val sheetCode: String,
    val floorName: String,
    val contractorName: String,
    val itemName: String,
    val formulaCode: String,
    val status: String,
    val unit: String,
    val rows: List<SheetExportRow>,
)

object MeasurementSheetCsv {
    fun render(sheet: SheetExport): String {
        val total = sheet.rows.sumOf { it.quantity }
        return buildString {
            appendLine("MEASUREMENT BOOK (QUANTITY ONLY)")
            appendLine("Project:,${csv(sheet.projectName + " (" + sheet.projectCode + ")")}")
            appendLine("Sheet Code:,${csv(sheet.sheetCode)}")
            appendLine("Floor:,${csv(sheet.floorName)}")
            appendLine("Contractor:,${csv(sheet.contractorName)}")
            appendLine("Work Item:,${csv(sheet.itemName)}")
            appendLine("Formula:,${csv(sheet.formulaCode)}")
            appendLine("Status:,${csv(sheet.status)}")
            appendLine()
            appendLine("No.,Description,Location,Length,Width,Height,Nos,Deduction,Quantity (${sheet.unit}),Remarks,Date")
            sheet.rows.forEachIndexed { index, row ->
                append(index + 1)
                append(',')
                append(csv(row.description))
                append(',')
                append(csv(row.location))
                append(',')
                append(row.length)
                append(',')
                append(row.width)
                append(',')
                append(row.height)
                append(',')
                append(row.nos)
                append(',')
                append(row.deduction)
                append(',')
                append(String.format(java.util.Locale.US, "%.2f", row.quantity))
                append(',')
                append(csv(row.remarks))
                append(',')
                append(csv(formatDate(row.dateMillis)))
                appendLine()
            }
            appendLine()
            appendLine("Total Executed Quantity:,,,,,,,${String.format(java.util.Locale.US, "%.2f", total)} ${sheet.unit}")
        }
    }

    private fun csv(value: String) = "\"" + value.replace("\"", "\"\"") + "\""

    private fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}

sealed class RecordResult {
    data class Saved(val sheetId: Long) : RecordResult()
    data class Rejected(val reason: String) : RecordResult()
}

data class ProjectModuleCounts(
    val dailyLogs: Int = 0,
    val materialOrders: Int = 0,
    val drawings: Int = 0,
    val siteIssues: Int = 0,
    val inspections: Int = 0,
    val minutes: Int = 0,
    val coordination: Int = 0,
    val decisions: Int = 0,
    val tasks: Int = 0,
)
