package com.holagundi.archiman.company

import java.time.LocalDate
import java.util.UUID
import org.json.JSONObject

data class CompanySiteRecord(val id:String,val projectId:String,val kind:String,val recordDate:String,val title:String,val details:String,
    val location:String,val assignedTo:String,val dueDate:String?,val status:String,val resolvedDate:String?,val version:Int)
data class CompanySitePage(val projectId:String,val records:List<CompanySiteRecord>,val nextCursor:String?)
data class CompanySiteChange(val projectId:String,val base:CompanySiteRecord?=null,val deleting:Boolean=false,
    val value:CompanySiteRecord=base ?: CompanySiteRecord(UUID.randomUUID().toString(),projectId,"inspection",LocalDate.now().toString(),"","","","",null,"open",null,1),
    val operationId:String=UUID.randomUUID().toString(),val confirmation:String="") {
    fun fields()=JSONObject().put("kind",value.kind).put("recordDate",value.recordDate).put("title",value.title.trim()).put("details",value.details)
        .put("location",value.location.trim()).put("assignedTo",value.assignedTo.trim()).put("dueDate",value.dueDate ?: JSONObject.NULL)
        .put("status",value.status).put("resolvedDate",value.resolvedDate ?: JSONObject.NULL)
    val valid get()=validClientId(projectId) && validClientId(value.id) && validClientId(operationId) && value.projectId==projectId &&
        (base==null || base.id==value.id && base.projectId==projectId && base.version in 1 until Int.MAX_VALUE) &&
        if(deleting)base!=null && confirmation=="DELETE" else siteFieldsValid(value)
}
sealed interface CompanySiteResult {
 data class Saved(val record:CompanySiteRecord):CompanySiteResult
 data class Deleted(val id:String):CompanySiteResult
 data class Conflict(val current:CompanySiteRecord):CompanySiteResult
}
private fun siteDate(value:String)=value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) && runCatching {LocalDate.parse(value).year in 1..9999}.getOrDefault(false)
internal fun siteFieldsValid(value:CompanySiteRecord)=value.kind in setOf("instruction","inspection","snag") && siteDate(value.recordDate) && value.title.trim().length in 1..240 &&
    value.details.length<=10000 && value.location.trim().length<=240 && value.assignedTo.trim().length<=240 && (value.dueDate==null || siteDate(value.dueDate)) &&
    value.status in setOf("open","resolved","closed") && if(value.status=="open")value.resolvedDate==null else value.resolvedDate!=null && siteDate(value.resolvedDate) && value.resolvedDate>=value.recordDate
internal fun readSiteRecord(row:JSONObject,projectId:String):CompanySiteRecord {
 val version=row.getLong("version");if(version !in 1..Int.MAX_VALUE.toLong())throw CompanyApiException("INVALID_RESPONSE")
 val record=CompanySiteRecord(row.getString("id"),row.getString("projectId"),row.getString("kind"),row.getString("recordDate"),row.getString("title"),row.getString("details"),
    row.getString("location"),row.getString("assignedTo"),if(row.isNull("dueDate"))null else row.getString("dueDate"),row.getString("status"),if(row.isNull("resolvedDate"))null else row.getString("resolvedDate"),version.toInt())
 if(!validClientId(record.id) || !validClientId(projectId) || record.projectId!=projectId || !siteFieldsValid(record))throw CompanyApiException("INVALID_RESPONSE")
 return record
}
internal fun readSitePage(page:JSONObject,projectId:String,after:String?):CompanySitePage {
 if(page.getString("projectId")!=projectId)throw CompanyApiException("INVALID_RESPONSE")
 val rows=page.getJSONArray("records");if(rows.length()>25)throw CompanyApiException("INVALID_RESPONSE")
 val records=(0 until rows.length()).map {readSiteRecord(rows.getJSONObject(it),projectId)}
 if(records.map {it.id}.distinct().size!=records.size)throw CompanyApiException("INVALID_RESPONSE")
 val next=if(page.isNull("nextCursor"))null else page.getString("nextCursor")
 if(next!=null && (!validClientId(next) || next==after || records.size!=25 || records.lastOrNull()?.id!=next))throw CompanyApiException("INVALID_RESPONSE")
 return CompanySitePage(projectId,records,next)
}
