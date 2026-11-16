package com.holagundi.archiman.company

import com.holagundi.archiman.sync.HttpResponse
import com.holagundi.archiman.sync.HttpTransport
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class CompanySiteRecordsTest {
 private val project="11111111-1111-4111-8111-111111111111"
 private val record=CompanySiteRecord("22222222-2222-4222-8222-222222222222",project,"snag","2026-11-16","Review finish","Fictional observation","North entry","Demo contractor","2026-11-20","open",null,1)
 private val session=CompanySession(CompanyConnection("https://company.example","/api/auth"),"private-test",CompanyIdentity("u","demo@example.test","Demo","Company",listOf("Project Manager"),listOf("sites.write"),false))
 private class Http:HttpTransport {
  var count=0;var body:String?=null;var method="";var response=HttpResponse(200,emptyMap(),"{}")
  override fun request(method:String,url:String,headers:Map<String,String>,body:String?):HttpResponse {count++;this.body=body;this.method=method;return response}
 }
 private fun row(value:CompanySiteRecord)=CompanySiteChange(project,value).fields().put("id",value.id).put("projectId",value.projectId).put("version",value.version)
 private fun invalid(run:()->Unit) {try{run();fail("Accepted invalid site operation")}catch(error:CompanyApiException){assertTrue(error.errorCode in listOf("INVALID_RESPONSE","INVALID_SITE_ACTION"))}}
 @Test fun `validates calendar dates resolution states and confirmation before network`() {
  val http=Http();val api=CompanyApiClient(http);val base=CompanySiteChange(project,record)
  for(value in listOf(record.copy(recordDate="2026-02-30"),record.copy(recordDate="0000-01-01"),record.copy(title=" "),record.copy(status="closed"),record.copy(resolvedDate="2026-11-17"),record.copy(status="resolved",resolvedDate="2026-11-15"),record.copy(details="x".repeat(10001)))) {
   val change=base.copy(value=value);assertFalse(change.valid);invalid{api.changeSiteRecord(session,change)}
  }
  invalid{api.changeSiteRecord(session,base.copy(deleting=true))};assertEquals(0,http.count)
  assertTrue(base.copy(value=record.copy(status="resolved",resolvedDate="2026-11-17")).valid)
 }
 @Test fun `edits preserve stable retries and verify returned fields and revision`() {
  val http=Http();val api=CompanyApiClient(http);val change=CompanySiteChange(project,record,value=record.copy(title=" Changed "))
  val result=JSONObject().put("outcome","applied").put("operationId",change.operationId).put("record",row(record.copy(title="Changed",version=2)))
  http.response=HttpResponse(200,emptyMap(),result.toString());assertEquals("Changed",(api.changeSiteRecord(session,change) as CompanySiteResult.Saved).record.title)
  val body=http.body;api.changeSiteRecord(session,change);assertEquals(body,http.body);assertEquals("PATCH",http.method)
  result.getJSONObject("record").put("location","Other");http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.changeSiteRecord(session,change)}
 }
 @Test fun `deletion checks identity timestamp revision and matching operation`() {
  val http=Http();val api=CompanyApiClient(http);val change=CompanySiteChange(project,record,deleting=true,confirmation="DELETE")
  val result=JSONObject().put("outcome","deleted").put("operationId",change.operationId).put("id",record.id).put("projectId",project).put("version",2).put("deletedAt","2026-11-16T10:00:00.000Z")
  http.response=HttpResponse(200,emptyMap(),result.toString());assertTrue(api.changeSiteRecord(session,change) is CompanySiteResult.Deleted)
  result.put("version",3);http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.changeSiteRecord(session,change)}
 }
 @Test fun `scoped pages reject wrong projects duplicates overflow and nonadvancing cursors`() {
  fun page(vararg records:JSONObject)=JSONObject().put("projectId",project).put("records",JSONArray(records.toList())).put("nextCursor",JSONObject.NULL)
  assertEquals(record,readSitePage(page(row(record)),project,null).records.single())
  invalid{readSitePage(page(row(record)).put("projectId",record.id),project,null)}
  invalid{readSitePage(page(row(record),row(record)),project,null)}
  invalid{readSitePage(page(row(record)).put("nextCursor",record.id),project,null)}
  val rows=(1..26).map {row(record.copy(id=java.util.UUID.randomUUID().toString()))}.toTypedArray();invalid{readSitePage(page(*rows),project,null)}
 }
}
