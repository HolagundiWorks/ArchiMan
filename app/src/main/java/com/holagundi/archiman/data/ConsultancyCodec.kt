package com.holagundi.archiman.data

import com.holagundi.archiman.domain.*
import org.json.JSONArray
import org.json.JSONObject

object ConsultancyCodec {
    fun encode(input: ConsultancyInput): String = JSONObject().apply {
        put("version", 1); put("title", input.title); put("service", input.service.name); put("feeBasis", input.feeBasis.name)
        put("gstMode", input.gstMode.name); put("gstRate", input.gstRate); put("gstin", input.gstin); put("supplierState", input.supplierState); put("placeState", input.placeState)
        put("rate", input.rate); put("quantity", input.quantity); put("areaDefinition", input.areaDefinition)
        put("parties", input.parties); put("scope", input.scope); put("exclusions", input.exclusions); put("terms", input.terms)
        put("templateVersion", input.templateVersion); put("sourceUrl", input.sourceUrl); put("proposalReference", input.proposalReference)
        put("milestones", JSONArray().apply { input.milestones.forEach { put(JSONObject().put("label", it.label).put("cumulativePercent", it.cumulativePercent)) } })
    }.toString()

    fun decode(text: String): ConsultancyInput {
        val json = JSONObject(text)
        require(json.getInt("version") == 1) { "Unsupported consultancy document version." }
        val stages = json.getJSONArray("milestones")
        return ConsultancyInput(
            title = json.getString("title"), service = ConsultancyService.valueOf(json.getString("service")), feeBasis = ConsultancyFeeBasis.valueOf(json.getString("feeBasis")),
            gstMode = GstMode.valueOf(json.optString("gstMode", "NONE")), gstRate = json.optString("gstRate", "18"), gstin = json.optString("gstin"), supplierState = json.optString("supplierState"), placeState = json.optString("placeState"),
            rate = json.getString("rate"), quantity = json.getString("quantity"), areaDefinition = json.getString("areaDefinition"), parties = json.getString("parties"),
            scope = json.getString("scope"), exclusions = json.getString("exclusions"), terms = json.getString("terms"),
            milestones = (0 until stages.length()).map { stages.getJSONObject(it).let { stage -> ConsultancyMilestone(stage.getString("label"), stage.getString("cumulativePercent")) } },
            templateVersion = json.getString("templateVersion"), sourceUrl = json.getString("sourceUrl"), proposalReference = json.optString("proposalReference"),
        )
    }
}
