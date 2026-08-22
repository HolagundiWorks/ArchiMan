package com.example.domain

import android.content.Context
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.ItemMasterEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class WorkCatalogDocument(val schemaVersion: Int = 1, val contractorTypes: List<ContractorTypeCatalog>)

@JsonClass(generateAdapter = true)
data class ContractorTypeCatalog(val name: String, val items: List<CatalogWorkItem>)

@JsonClass(generateAdapter = true)
data class CatalogWorkItem(val name: String, val uom: String, val formula: String, val workType: String)

object CatalogDocumentParser {
    private val adapter = Moshi.Builder().build().adapter(WorkCatalogDocument::class.java)

    fun parse(json: String): WorkCatalogDocument {
        val document = requireNotNull(adapter.fromJson(json)) { "Catalog is empty" }
        require(document.schemaVersion == 1) { "Unsupported catalog schema version ${document.schemaVersion}" }
        require(document.contractorTypes.isNotEmpty()) { "Catalog must contain contractor types" }
        val types = mutableSetOf<String>()
        val names = mutableSetOf<String>()
        document.contractorTypes.forEach { type ->
            require(type.name.isNotBlank()) { "Contractor type cannot be blank" }
            require(types.add(WorkCatalog.normalizeName(type.name))) { "Duplicate contractor type: ${type.name}" }
            type.items.forEach { item ->
                require(item.name.isNotBlank() && item.uom.isNotBlank() && item.workType.isNotBlank()) { "Catalog item fields cannot be blank" }
                require(runCatching { CalculationType.valueOf(item.formula) }.isSuccess) { "Unknown formula ${item.formula} for ${item.name}" }
                require(names.add(WorkCatalog.normalizeName(item.name))) { "Duplicate work item: ${item.name}" }
            }
        }
        return document
    }

    fun loadBundled(context: Context): WorkCatalogDocument = context.assets.open("standard_work_catalog.json")
        .bufferedReader().use { parse(it.readText()) }

    fun toMasterItems(document: WorkCatalogDocument): List<ItemMasterEntity> = document.contractorTypes.flatMap { type ->
        type.items.map { item ->
            ItemMasterEntity(
                itemCode = WorkCatalog.codeFor(item.workType, item.name),
                workType = item.workType,
                name = item.name.trim(),
                unit = item.uom.trim(),
                calculationType = CalculationType.valueOf(item.formula),
                isPredefined = false
            )
        }
    }
}
