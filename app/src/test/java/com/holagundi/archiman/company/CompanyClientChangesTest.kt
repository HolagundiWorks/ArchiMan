package com.holagundi.archiman.company

import com.holagundi.archiman.sync.HttpResponse
import com.holagundi.archiman.sync.HttpTransport
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CompanyClientChangesTest {
 private val id="11111111-1111-4111-8111-111111111111"
 private val client=CompanyClient(id,"Client","individual","Person","demo@example.test","","","","","",1)
 private val session=CompanySession(CompanyConnection("https://company.example","/api/auth"),"private-test",CompanyIdentity("u","demo@example.test","Demo","Company",listOf("Owner"),listOf("clients.write"),true))
 private class Http:HttpTransport {
  var count=0;var body:String?=null;var method="";var response=HttpResponse(200,emptyMap(),"{}")
  override fun request(method:String,url:String,headers:Map<String,String>,body:String?):HttpResponse {count++;this.body=body;this.method=method;return response}
 }
 private fun row(value:CompanyClient)=CompanyClientChange(base=value).fields().put("id",value.id).put("version",value.version)
 private fun invalid(run:()->Unit) {try{run();fail("Accepted invalid client operation")}catch(error:CompanyApiException){assertTrue(error.errorCode in listOf("INVALID_RESPONSE","INVALID_CLIENT_ACTION"))}}
 @Test fun `validates fields and confirmation before writing`() {
  val http=Http();val api=CompanyApiClient(http)
  val change=CompanyClientChange(base=client)
  for(draft in listOf(change.copy(value=client.copy(name=" ")),change.copy(value=client.copy(notes="x".repeat(4001))),change.copy(value=client.copy(kind="invalid")),change.copy(base=client.copy(version=Int.MAX_VALUE)),change.copy(deleting=true),change.copy(operationId="bad"))) {
   assertFalse(draft.valid);invalid{api.changeClient(session,draft)}
  }
  assertEquals(0,http.count)
 }
 @Test fun `retries unchanged client edits and validates response identity and values`() {
  val http=Http();val api=CompanyApiClient(http);val change=CompanyClientChange(base=client,value=client.copy(name=" Changed "))
  val result=JSONObject().put("outcome","applied").put("operationId",change.operationId).put("client",row(client.copy(name="Changed",version=2)))
  http.response=HttpResponse(200,emptyMap(),result.toString())
  assertTrue(api.changeClient(session,change) is CompanyClientChangeResult.Saved);val body=http.body
  api.changeClient(session,change);assertEquals(body,http.body);assertEquals("PATCH",http.method)
  assertEquals(1,JSONObject(body!!).getInt("expectedVersion"))
  result.getJSONObject("client").put("email","wrong@example.test");http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.changeClient(session,change)}
 }
 @Test fun `deletion requires matching operation revision and valid timestamp`() {
  val http=Http();val api=CompanyApiClient(http);val change=CompanyClientChange(base=client,deleting=true,confirmation="DELETE")
  val result=JSONObject().put("outcome","deleted").put("operationId",change.operationId).put("id",id).put("version",2).put("deletedAt","2026-11-16T10:00:00.000Z")
  http.response=HttpResponse(200,emptyMap(),result.toString());assertTrue(api.changeClient(session,change) is CompanyClientChangeResult.Deleted)
  assertEquals("DELETE",http.method)
  result.put("operationId",java.util.UUID.randomUUID().toString());http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.changeClient(session,change)}
 }
 @Test fun `link responses must match project client and expected revision`() {
  val http=Http();val api=CompanyApiClient(http);val project="22222222-2222-4222-8222-222222222222"
  val link=CompanyClientLink(CompanyProjectClient(project,null,null,1),id)
  val value=JSONObject().put("projectId",project).put("clientId",id).put("clientName","Client").put("version",2)
  val result=JSONObject().put("outcome","linked").put("operationId",link.operationId).put("project",value)
  http.response=HttpResponse(200,emptyMap(),result.toString());assertTrue(api.linkClient(session,link) is CompanyClientLinkResult.Linked)
  assertEquals("PUT",http.method);assertEquals(id,JSONObject(http.body!!).getString("clientId"))
  value.put("clientId",JSONObject.NULL).put("clientName",JSONObject.NULL);http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.linkClient(session,link)}
 }
 @Test fun `conflicts expose the current record only for the matching operation`() {
  val http=Http();val api=CompanyApiClient(http);val change=CompanyClientChange(base=client)
  val result=JSONObject().put("outcome","conflict").put("operationId",change.operationId).put("expectedVersion",1).put("current",row(client.copy(version=2,name="Concurrent change")))
  fun response()=HttpResponse(409,emptyMap(),JSONObject().put("error",JSONObject().put("code","VERSION_CONFLICT")).put("conflict",result).toString())
  http.response=response();assertEquals("Concurrent change",(api.changeClient(session,change) as CompanyClientChangeResult.Conflict).current.name)
  result.put("expectedVersion",2);http.response=response();invalid{api.changeClient(session,change)}
 }
}
