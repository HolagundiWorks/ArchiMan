package com.holagundi.archiman.company

import org.json.JSONObject

internal fun projectJson(project: CompanyProject): JSONObject = JSONObject()
    .put("id", project.id).put("name", project.name).put("code", project.code).put("status", project.status).put("version", project.version)
internal fun readProject(value: JSONObject) = CompanyProject(value.getString("id"), value.getString("name"),
    value.getString("code"), value.getString("status"), value.getInt("version"))
