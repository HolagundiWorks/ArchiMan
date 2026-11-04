package com.holagundi.archiman.data

import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.QuantityCalculator
import com.holagundi.archiman.domain.SchemaContract
import org.json.JSONArray
import org.json.JSONObject

data class DemoIndexEntry(val key: String, val label: String, val count: Int)

data class DemoCompanyPackage(
    val practiceName: String,
    val schemaVersion: Int,
    val fictional: Boolean,
    val index: List<DemoIndexEntry>,
    val source: JSONObject,
) {
    fun requireOpenable() {
        require(source.optString("format") == FORMAT) { "This file is not an ArchiMan company package." }
        require(schemaVersion == SchemaContract.VERSION) {
            "Demo file is schema $schemaVersion. This app opens schema ${SchemaContract.VERSION}."
        }
        require(fictional) { "Built-in demo packages must be marked fictional." }
        val actual = counts(source)
        index.forEach { entry ->
            val found = actual[entry.key]
            require(found != null) { "Index key ${entry.key} is not part of this package." }
            require(found == entry.count) {
                "Index for ${entry.label} says ${entry.count}. The file contains $found."
            }
        }
        require(actual.keys.all { key -> index.any { it.key == key } }) { "The file index is missing a section." }
        checkQuantities(source)
    }

    companion object {
        const val FORMAT = "archiman-company"
        const val ASSET = "ArchiMan-Demo-Company.archimandb.json"

        fun parse(text: String): DemoCompanyPackage {
            val json = JSONObject(text)
            val indexJson = json.getJSONArray("index")
            val index = buildList {
                for (i in 0 until indexJson.length()) {
                    val row = indexJson.getJSONObject(i)
                    add(DemoIndexEntry(row.getString("key"), row.getString("label"), row.getInt("count")))
                }
            }
            return DemoCompanyPackage(
                practiceName = json.getString("practiceName"),
                schemaVersion = json.getInt("schemaVersion"),
                fictional = json.optBoolean("fictional", false),
                index = index,
                source = json,
            )
        }

        fun counts(json: JSONObject): Map<String, Int> {
            val projects = json.optJSONArray("projects") ?: JSONArray()
            var levels = 0
            var rooms = 0
            var contractors = 0
            var qualified = 0
            var sheets = 0
            var measurements = 0
            var tasks = 0
            var letters = 0
            var proposals = 0
            var contracts = 0
            var visits = 0
            var snags = 0
            var coordination = 0
            var drawings = 0
            var revisions = 0
            var transmittals = 0
            var logs = 0
            var orders = 0
            var invoices = 0
            for (i in 0 until projects.length()) {
                val project = projects.getJSONObject(i)
                val floors = project.optJSONArray("floors") ?: JSONArray()
                levels += floors.length()
                for (f in 0 until floors.length()) {
                    rooms += floors.getJSONObject(f).optJSONArray("rooms")?.length() ?: 0
                }
                val crews = project.optJSONArray("contractors") ?: JSONArray()
                contractors += crews.length()
                for (c in 0 until crews.length()) {
                    qualified += crews.getJSONObject(c).optJSONArray("qualified")?.length() ?: 0
                }
                val book = project.optJSONArray("sheets") ?: JSONArray()
                sheets += book.length()
                for (s in 0 until book.length()) {
                    measurements += book.getJSONObject(s).optJSONArray("rows")?.length() ?: 0
                }
                tasks += project.optJSONArray("tasks")?.length() ?: 0
                letters += project.optJSONArray("letters")?.length() ?: 0
                if (project.has("proposal")) proposals += 1
                if (project.has("contract")) contracts += 1
                if (project.has("visit")) visits += 1
                snags += project.optJSONArray("snags")?.length() ?: 0
                coordination += project.optJSONArray("coordination")?.length() ?: 0
                val drawingRows = project.optJSONArray("drawings") ?: JSONArray()
                drawings += drawingRows.length()
                for (d in 0 until drawingRows.length()) {
                    revisions += drawingRows.getJSONObject(d).optJSONArray("revisions")?.length() ?: 0
                }
                transmittals += project.optJSONArray("transmittals")?.length() ?: 0
                logs += project.optJSONArray("logs")?.length() ?: 0
                orders += project.optJSONArray("orders")?.length() ?: 0
                if (project.has("invoice")) invoices += 1
            }
            return mapOf(
                "clients" to (json.optJSONArray("clients")?.length() ?: 0),
                "projects" to projects.length(),
                "levels" to levels,
                "rooms" to rooms,
                "contractors" to contractors,
                "qualifiedItems" to qualified,
                "workLibrary" to (json.optJSONArray("workItems")?.length() ?: 0),
                "sheets" to sheets,
                "measurements" to measurements,
                "tasks" to tasks,
                "letters" to letters,
                "proposals" to proposals,
                "contracts" to contracts,
                "visits" to visits,
                "snags" to snags,
                "coordination" to coordination,
                "drawings" to drawings,
                "revisions" to revisions,
                "transmittals" to transmittals,
                "logs" to logs,
                "orders" to orders,
                "invoices" to invoices,
            )
        }

        private fun checkQuantities(json: JSONObject) {
            val items = json.getJSONArray("workItems")
            val types = buildMap {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    put(item.getString("ref"), item.getString("calculationType"))
                }
            }
            val projects = json.optJSONArray("projects") ?: return
            for (p in 0 until projects.length()) {
                val sheets = projects.getJSONObject(p).optJSONArray("sheets") ?: continue
                for (s in 0 until sheets.length()) {
                    val sheet = sheets.getJSONObject(s)
                    val type = types[sheet.getString("itemRef")]
                        ?: error("Sheet ${sheet.getString("code")} uses an unknown work item.")
                    val rows = sheet.optJSONArray("rows") ?: JSONArray()
                    for (r in 0 until rows.length()) {
                        val row = rows.getJSONObject(r)
                        val quantity = QuantityCalculator.calculate(
                            type,
                            MeasurementInput(
                                no = row.optDouble("nos", 1.0),
                                length = row.optDouble("length", 0.0),
                                breadth = row.optDouble("breadth", 0.0),
                                height = row.optDouble("height", 0.0),
                                deduction = row.optDouble("deduction", 0.0),
                            ),
                        )
                        require(quantity >= 0.0) { "Quantity for ${row.optString("description")} is invalid." }
                    }
                }
            }
        }
    }
}
