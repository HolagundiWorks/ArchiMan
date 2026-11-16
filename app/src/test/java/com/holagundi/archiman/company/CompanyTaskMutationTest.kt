package com.holagundi.archiman.company

import com.holagundi.archiman.sync.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CompanyTaskMutationTest {
    private val projectId="11111111-1111-4111-8111-111111111111"
    private val task=CompanyTask("22222222-2222-4222-8222-222222222222",projectId,"Review","Details","2026-11-20T14:30:00.000Z","open","normal",1)
    private val session=CompanySession(CompanyConnection("https://company.example","/api/auth"),"private-token",
        CompanyIdentity("u","demo@example.test","Demo","Company",listOf("Project Manager"),listOf("projects.read","tasks.write"),false,"org"))
    private class Http:HttpTransport {
        val requests=mutableListOf<Triple<String,String,String?>>()
        var result:HttpResponse?=null
        override fun request(method:String,url:String,headers:Map<String,String>,body:String?):HttpResponse {
            requests+=Triple(method,url,body)
            return result ?: throw IOException("offline")
        }
    }
    private fun row(edit:CompanyTaskEdit,version:Int)=edit.fields().put("id",edit.id).put("projectId",edit.projectId).put("version",version)
    private fun applied(edit:CompanyTaskEdit)=HttpResponse(200,emptyMap(),JSONObject().put("outcome","applied").put("operationId",edit.operationId)
        .put("task",row(edit,(edit.base?.version ?: 0)+1)).toString())

    @Test fun `create sends a stable task id and operation on retries`() {
        val http=Http();val api=CompanyApiClient(http);val edit=CompanyTaskEdit(projectId,title=" New task ",dueDate="2026-11-21")
        http.result=applied(edit)
        val first=api.saveTask(session,edit)
        assertEquals(first,api.saveTask(session,edit))
        assertEquals("POST",http.requests.first().first)
        assertTrue(http.requests.first().second.endsWith("/v1/projects/$projectId/tasks"))
        assertEquals(http.requests.first().third,http.requests.last().third)
        val body=JSONObject(http.requests.first().third!!)
        assertEquals(edit.id,body.getString("id"));assertFalse(body.has("expectedVersion"))
        assertEquals("New task",(first as CompanyTaskMutationResult.Applied).task.title)
    }
    @Test fun `edits preserve existing due times and send an expected revision`() {
        val http=Http();val api=CompanyApiClient(http);val edit=CompanyTaskEdit(projectId,task,status="completed")
        http.result=applied(edit)
        assertEquals(task.dueAt,edit.dueAt())
        assertTrue(api.saveTask(session,edit) is CompanyTaskMutationResult.Applied)
        assertEquals("PATCH",http.requests.single().first)
        assertEquals(1,JSONObject(http.requests.single().third!!).getInt("expectedVersion"))
        assertNull(edit.copy(dueDate="").dueAt())
    }
    @Test fun `conflicts require matching operation submitted values and task identity`() {
        val http=Http();val api=CompanyApiClient(http);val edit=CompanyTaskEdit(projectId,task,title="My draft")
        val current=row(edit,2).put("title","Other editor").put("status","in_progress")
        val conflict=JSONObject().put("outcome","conflict").put("operationId",edit.operationId).put("expectedVersion",1)
            .put("submitted",edit.fields()).put("current",current)
        fun response()=HttpResponse(409,emptyMap(),JSONObject().put("error",JSONObject().put("code","VERSION_CONFLICT")).put("conflict",conflict).toString())
        http.result=response()
        assertEquals("Other editor",(api.saveTask(session,edit) as CompanyTaskMutationResult.Conflict).current.title)
        current.put("projectId","33333333-3333-4333-8333-333333333333");http.result=response()
        try {api.saveTask(session,edit);fail()} catch(error:CompanyApiException) {assertEquals("INVALID_RESPONSE",error.errorCode)}
        current.put("projectId",projectId);conflict.put("operationId","44444444-4444-4444-8444-444444444444");http.result=response()
        try {api.saveTask(session,edit);fail()} catch(error:CompanyApiException) {assertEquals("INVALID_RESPONSE",error.errorCode)}
    }
    @Test fun `invalid dates fields and revisions never reach the network`() {
        val http=Http();val api=CompanyApiClient(http);val valid=CompanyTaskEdit(projectId,task)
        for(edit in listOf(valid.copy(title=" "),valid.copy(title="x".repeat(241)),valid.copy(description="x".repeat(10001)),
            valid.copy(dueDate="2026-02-30"),valid.copy(dueDate="0000-01-01"),valid.copy(status="unknown"),
            valid.copy(base=task.copy(version=Int.MAX_VALUE)))) {
            assertFalse(edit.valid)
            try {api.saveTask(session,edit);fail()} catch(error:CompanyApiException) {assertEquals("INVALID_TASK",error.errorCode)}
        }
        assertTrue(http.requests.isEmpty())
    }
    @Test fun `task writes persist no drafts records or retry operations and outages clear memory`() {
        val values=object:KeyValues {
            val data=mutableMapOf<String,String>()
            override fun get(key:String)=data[key]
            override fun put(key:String,value:String) {data[key]=value}
            override fun remove(key:String) {data.remove(key)}
        }
        val box=object:SecretBox {
            override fun seal(plain:ByteArray)=plain
            override fun open(sealed:ByteArray)=sealed
        }
        val store=CompanyAccountStore(values,box);store.connect(session.connection);store.save(session)
        val before=values.data.toMap();val http=Http();val account=CompanyAccount(store,CompanyApiClient(http))
        val edit=CompanyTaskEdit(projectId,title="Private company task")
        http.result=applied(edit);account.saveTask(edit)
        assertEquals(before,values.data)
        http.result=null
        try {account.saveTask(edit);fail()} catch(_:IOException) {}
        assertNull(account.session());assertTrue(account.hasSavedSession())
        assertEquals(before,values.data);assertNull(CompanyAccountStore(values,box).load())
    }
}
