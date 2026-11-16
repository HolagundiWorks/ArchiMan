package com.holagundi.archiman.company

import com.holagundi.archiman.sync.HttpResponse
import com.holagundi.archiman.sync.HttpTransport
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CompanyTaskActionsTest {
 private val project="11111111-1111-4111-8111-111111111111"
 private val member="22222222-2222-4222-8222-222222222222"
 private val task=CompanyTask("33333333-3333-4333-8333-333333333333",project,"Review","",null,"open","normal",1)
 private val session=CompanySession(CompanyConnection("https://company.example","/api/auth"),"private-test",CompanyIdentity("u","demo@example.test","Demo","Company",listOf("Project Manager"),listOf("tasks.assign"),false))
 private class Http:HttpTransport {
  var count=0;var body:String?=null;var method="";var response=HttpResponse(200,emptyMap(),"{}")
  override fun request(method:String,url:String,headers:Map<String,String>,body:String?):HttpResponse {count++;this.body=body;this.method=method;return response}
 }
 private fun row(version:Int,assignees:JSONArray=JSONArray())=JSONObject().put("id",task.id).put("projectId",project).put("title",task.title).put("description","").put("dueAt",JSONObject.NULL).put("status","open").put("priority","normal").put("version",version).put("assignees",assignees)
 private fun invalid(run:()->Unit) {try{run();fail("Accepted invalid task action")}catch(error:CompanyApiException){assertTrue(error.errorCode in listOf("INVALID_RESPONSE","INVALID_TASK_ACTION"))}}
 @Test fun `invalid confirmation member identifiers and revisions do not reach network`() {
  val http=Http();val api=CompanyApiClient(http)
  for(action in listOf(CompanyTaskAction(task,true),CompanyTaskAction(task,membershipIds=setOf("bad")),CompanyTaskAction(task.copy(version=Int.MAX_VALUE)),CompanyTaskAction(task,membershipIds=(1..21).map {java.util.UUID.randomUUID().toString()}.toSet()))) {
   assertFalse(action.valid);invalid{api.taskAction(session,action)}
  }
  assertEquals(0,http.count)
 }
 @Test fun `assignment sends stable selected ids and validates returned members`() {
  val http=Http();val api=CompanyApiClient(http);val action=CompanyTaskAction(task,membershipIds=setOf(member))
  val people=JSONArray().put(JSONObject().put("membershipId",member).put("name","Demo Staff"))
  val result=JSONObject().put("outcome","assigned").put("operationId",action.operationId).put("task",row(2,people))
  http.response=HttpResponse(200,emptyMap(),result.toString())
  assertEquals(member,(api.taskAction(session,action) as CompanyTaskActionResult.Assigned).task.assignees.single().membershipId)
  val body=http.body;api.taskAction(session,action);assertEquals(body,http.body);assertEquals("PUT",http.method)
  result.getJSONObject("task").put("assignees",JSONArray());http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.taskAction(session,action)}
 }
 @Test fun `deletion validates task identity project revision timestamp and operation`() {
  val http=Http();val api=CompanyApiClient(http);val action=CompanyTaskAction(task,true,confirmation="DELETE")
  val result=JSONObject().put("outcome","deleted").put("operationId",action.operationId).put("id",task.id).put("projectId",project).put("version",2).put("deletedAt","2026-11-16T10:00:00.000Z")
  http.response=HttpResponse(200,emptyMap(),result.toString());assertTrue(api.taskAction(session,action) is CompanyTaskActionResult.Deleted)
  assertEquals("DELETE",http.method)
  result.put("projectId",member);http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.taskAction(session,action)}
 }
 @Test fun `candidate pages and task assignees reject duplicates overflow and stalled cursors`() {
  val person=JSONObject().put("membershipId",member).put("name","Staff")
  invalid{readTaskAssignees(JSONArray().put(person).put(person))}
  invalid{readTaskAssignees(JSONArray((1..21).map {JSONObject().put("membershipId",java.util.UUID.randomUUID().toString()).put("name","Person")}))}
  val rows=JSONArray((1..25).map {JSONObject().put("membershipId",java.util.UUID.randomUUID().toString()).put("name","Person")})
  val last=rows.getJSONObject(24).getString("membershipId")
  val page=JSONObject().put("members",rows).put("nextCursor",last)
  assertEquals(last,readTaskCandidates(page,null).nextCursor);invalid{readTaskCandidates(page,last)}
 }
}
