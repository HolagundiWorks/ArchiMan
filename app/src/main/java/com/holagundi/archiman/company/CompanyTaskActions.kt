package com.holagundi.archiman.company

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class CompanyTaskAssignee(val membershipId:String,val name:String)
data class CompanyTaskCandidatePage(val members:List<CompanyTaskAssignee>,val nextCursor:String?)
data class CompanyTaskAction(val base:CompanyTask,val deleting:Boolean=false,val membershipIds:Set<String> = base.assignees.map {it.membershipId}.toSet(),
    val operationId:String=UUID.randomUUID().toString(),val confirmation:String="") {
 val valid get()=validClientId(base.id) && validClientId(base.projectId) && validClientId(operationId) && base.version in 1 until Int.MAX_VALUE &&
    if(deleting)confirmation=="DELETE" else membershipIds.size<=20 && membershipIds.all(::validClientId)
}
sealed interface CompanyTaskActionResult {
 data class Assigned(val task:CompanyTask):CompanyTaskActionResult
 data class Deleted(val id:String):CompanyTaskActionResult
 data class Conflict(val current:CompanyTask):CompanyTaskActionResult
}
internal fun readTaskAssignees(rows:JSONArray?,max:Int=20):List<CompanyTaskAssignee> {
 if(rows==null)return emptyList()
 if(rows.length()>max)throw CompanyApiException("INVALID_RESPONSE")
 val members=(0 until rows.length()).map {i->val row=rows.getJSONObject(i);val id=row.getString("membershipId");val name=row.getString("name")
    if(!validClientId(id) || name.isBlank() || name.length>240)throw CompanyApiException("INVALID_RESPONSE")
    CompanyTaskAssignee(id,name)}
 if(members.map {it.membershipId}.distinct().size!=members.size)throw CompanyApiException("INVALID_RESPONSE")
 return members
}
internal fun readTaskCandidates(page:JSONObject,after:String?):CompanyTaskCandidatePage {
 val members=readTaskAssignees(page.getJSONArray("members"),25)
 val next=if(page.isNull("nextCursor"))null else page.getString("nextCursor")
 if(next!=null && (!validClientId(next) || next==after || members.size!=25 || members.lastOrNull()?.membershipId!=next))throw CompanyApiException("INVALID_RESPONSE")
 return CompanyTaskCandidatePage(members,next)
}
