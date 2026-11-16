package com.holagundi.archiman.company

import java.util.UUID
import java.time.Instant
import org.json.JSONObject

data class CompanyProjectCreate(
    val id:String=UUID.randomUUID().toString(),val operationId:String=UUID.randomUUID().toString(),
    val name:String="",val code:String="",val status:String="active",
) {
    val valid get()=canonicalId(id) && canonicalId(operationId) && name.trim().length in 1..240 && code.trim().length<=80 &&
        status in setOf("active","on_hold","completed","archived")
}
data class CompanyProjectDelete(val base:CompanyProject,val operationId:String=UUID.randomUUID().toString(),val confirmation:String="") {
    val valid get()=canonicalId(base.id) && canonicalId(operationId) && base.version in 1 until Int.MAX_VALUE && confirmation=="DELETE"
}
sealed interface CompanyProjectDeleteResult {
    data class Deleted(val id:String,val version:Int,val deletedAt:String):CompanyProjectDeleteResult
    data class Conflict(val current:CompanyProject):CompanyProjectDeleteResult
}
private fun canonicalId(value:String)=runCatching {UUID.fromString(value).toString()==value}.getOrDefault(false)

internal fun readLifecycleProject(row:JSONObject,id:String):CompanyProject {
    val project=readProject(row)
    if(project.id!=id || project.version<1 || project.name.isBlank() || project.name.length>240 || project.code.length>80 ||
        project.status !in setOf("active","on_hold","completed","archived"))throw CompanyApiException("INVALID_RESPONSE")
    return project
}
internal fun validDeletionTime(value:String)=runCatching {Instant.parse(value)}.isSuccess
