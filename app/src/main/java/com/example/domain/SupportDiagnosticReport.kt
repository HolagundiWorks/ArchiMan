package com.example.domain

data class SupportDiagnosticSnapshot(
    val appVersion: String,
    val versionCode: Long,
    val schemaVersion: Int,
    val androidSdk: Int,
    val generatedAtEpochMs: Long,
    val projectCount: Int,
    val contractorCount: Int,
    val workItemCount: Int,
    val measurementCount: Int,
    val sheetCount: Int,
    val archivedSheetCount: Int,
    val rowsWithoutKnownSheet: Int,
    val rowsWithoutKnownWorkItem: Int,
    val sheetStatusCounts: Map<String, Int>
)

object SupportDiagnosticReport {
    const val FORMAT_VERSION = 1

    /**
     * Produces support metadata only. Names, descriptions, dimensions, quantities,
     * comments, photos, filesystem paths, and stable database identifiers are excluded.
     */
    fun render(snapshot: SupportDiagnosticSnapshot): String {
        val statuses = snapshot.sheetStatusCounts
            .toSortedMap()
            .entries
            .joinToString(",\n") { (status, count) ->
                "    \"${escape(status)}\": $count"
            }
        val statusBody = if (statuses.isBlank()) "" else "\n$statuses\n  "

        return """
            {
              "formatVersion": $FORMAT_VERSION,
              "generatedAtEpochMs": ${snapshot.generatedAtEpochMs},
              "app": {
                "versionName": "${escape(snapshot.appVersion)}",
                "versionCode": ${snapshot.versionCode},
                "databaseSchema": ${snapshot.schemaVersion},
                "androidSdk": ${snapshot.androidSdk}
              },
              "counts": {
                "projects": ${snapshot.projectCount},
                "contractors": ${snapshot.contractorCount},
                "workItems": ${snapshot.workItemCount},
                "measurements": ${snapshot.measurementCount},
                "sheets": ${snapshot.sheetCount},
                "archivedSheets": ${snapshot.archivedSheetCount}
              },
              "integrity": {
                "rowsWithoutKnownSheet": ${snapshot.rowsWithoutKnownSheet},
                "rowsWithoutKnownWorkItem": ${snapshot.rowsWithoutKnownWorkItem}
              },
              "sheetStatuses": {$statusBody}
            }
        """.trimIndent()
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u%04x".format(character.code))
                } else {
                    append(character)
                }
            }
        }
    }
}
