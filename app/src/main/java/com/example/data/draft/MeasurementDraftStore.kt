package com.example.data.draft

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class MeasurementDraftRow(
    val id: String,
    val description: String,
    val lengthText: String,
    val heightText: String,
    val widthText: String,
    val nosText: String,
    val deductionText: String,
    val remarks: String,
    val photoUri: String?
)

@JsonClass(generateAdapter = true)
data class MeasurementDraft(val rows: List<MeasurementDraftRow>, val updatedAt: Long)

data class MeasurementDraftKey(val projectId: Long, val contractorId: Long, val itemId: Long, val floorId: Long) {
    val storageKey: String get() = "draft_${projectId}_${contractorId}_${itemId}_${floorId}"
}

class MeasurementDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("measurement_drafts", Context.MODE_PRIVATE)
    private val adapter = Moshi.Builder().build().adapter(MeasurementDraft::class.java)

    fun load(key: MeasurementDraftKey): MeasurementDraft? = preferences.getString(key.storageKey, null)
        ?.let { runCatching { adapter.fromJson(it) }.getOrNull() }

    fun save(key: MeasurementDraftKey, rows: List<MeasurementDraftRow>) {
        if (rows.none(::hasContent)) {
            clear(key)
            return
        }
        preferences.edit().putString(key.storageKey, adapter.toJson(MeasurementDraft(rows, System.currentTimeMillis()))).apply()
    }

    fun clear(key: MeasurementDraftKey) {
        preferences.edit().remove(key.storageKey).apply()
    }

    private fun hasContent(row: MeasurementDraftRow): Boolean = row.description.isNotBlank() ||
        row.lengthText.isNotBlank() || row.heightText.isNotBlank() || row.widthText.isNotBlank() ||
        row.nosText.toDoubleOrNull()?.let { it != 1.0 } == true ||
        row.deductionText.toDoubleOrNull()?.let { it != 0.0 } == true ||
        row.remarks.isNotBlank() || row.photoUri != null
}
