package com.holagundi.archiman.company

import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID

data class CompanyClientChange(val base:CompanyClient?=null,val deleting:Boolean=false,
    val value:CompanyClient=base ?: CompanyClient(UUID.randomUUID().toString(),"","individual","","","","","","","",1),
    val operationId:String=UUID.randomUUID().toString(),val confirmation:String="") {
    fun fields()=JSONObject().put("name",value.name.trim()).put("kind",value.kind).put("contactPerson",value.contactPerson.trim())
        .put("email",value.email.trim()).put("phone",value.phone.trim()).put("address",value.address.trim())
        .put("correspondenceAddress",value.correspondenceAddress.trim()).put("preferredCommunication",value.preferredCommunication.trim()).put("notes",value.notes.trim())
    val valid get()=validClientId(value.id) && validClientId(operationId) && (base==null || (base.id==value.id && base.version in 1 until Int.MAX_VALUE)) &&
        if(deleting) base!=null && confirmation=="DELETE" else value.name.trim().length in 1..240 && value.kind in setOf("individual","company","architect_firm") &&
        value.contactPerson.trim().length<=240 && value.email.trim().length<=254 && value.phone.trim().length<=80 && value.address.trim().length<=2000 &&
        value.correspondenceAddress.trim().length<=2000 && value.preferredCommunication.trim().length<=120 && value.notes.trim().length<=4000
}
sealed interface CompanyClientChangeResult {
 data class Saved(val client:CompanyClient):CompanyClientChangeResult
 data class Deleted(val id:String):CompanyClientChangeResult
 data class Conflict(val current:CompanyClient):CompanyClientChangeResult
}
data class CompanyProjectClient(val projectId:String,val clientId:String?,val clientName:String?,val version:Int)
data class CompanyClientLink(val base:CompanyProjectClient,val clientId:String?=base.clientId,val operationId:String=UUID.randomUUID().toString()) {
 val valid get()=validClientId(base.projectId) && validClientId(operationId) && (clientId==null || validClientId(clientId)) && base.version in 1 until Int.MAX_VALUE
}
sealed interface CompanyClientLinkResult {
 data class Linked(val project:CompanyProjectClient):CompanyClientLinkResult
 data class Conflict(val current:CompanyProjectClient):CompanyClientLinkResult
}
internal fun validClientId(value:String)=runCatching {UUID.fromString(value).toString()==value}.getOrDefault(false)
internal fun readMutationClient(row:JSONObject)=readCompanyClients(JSONObject().put("clients",JSONArray().put(row)).put("nextCursor",JSONObject.NULL),null).clients.single()
internal fun readProjectClient(row:JSONObject,id:String):CompanyProjectClient {
 val clientId=if(row.isNull("clientId"))null else row.getString("clientId")
 val name=if(row.isNull("clientName"))null else row.getString("clientName")
 val version=row.getLong("version")
 if(row.getString("projectId")!=id || !validClientId(id) || (clientId!=null && !validClientId(clientId)) ||
     (clientId==null)!=(name==null) || (name!=null && (name.isBlank() || name.length>240)) || version !in 1..Int.MAX_VALUE.toLong())throw CompanyApiException("INVALID_RESPONSE")
 return CompanyProjectClient(id,clientId,name,version.toInt())
}
