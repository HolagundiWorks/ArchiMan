package com.holagundi.archiman.company

import com.holagundi.archiman.sync.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CompanyProjectLifecycleTest {
    private val project=CompanyProject("11111111-1111-4111-8111-111111111111","Project","P-01","active",1)
    private val session=CompanySession(CompanyConnection("https://company.example","/api/auth"),"private-token",
        CompanyIdentity("u","demo@example.test","Owner","Company",listOf("Owner"),listOf("projects.read","projects.create","projects.delete"),true,"org"))
    private class Http:HttpTransport {
        var result:HttpResponse?=null
        val requests=mutableListOf<Triple<String,String,String?>>()
        override fun request(method:String,url:String,headers:Map<String,String>,body:String?):HttpResponse {
            requests+=Triple(method,url,body);return result ?: throw IOException("offline")
        }
    }
    private fun created(draft:CompanyProjectCreate)=HttpResponse(200,emptyMap(),JSONObject().put("outcome","created").put("operationId",draft.operationId)
        .put("project",projectJson(CompanyProject(draft.id,draft.name.trim(),draft.code.trim(),draft.status,1))).toString())
    private fun deleted(action:CompanyProjectDelete)=HttpResponse(200,emptyMap(),JSONObject().put("outcome","deleted").put("operationId",action.operationId)
        .put("id",action.base.id).put("version",action.base.version+1).put("deletedAt","2026-11-16T00:00:00.000Z").toString())

    @Test fun `project creation uses a stable id and operation for identical retries`() {
        val http=Http();val api=CompanyApiClient(http);val draft=CompanyProjectCreate(name=" New project ",code=" P-02 ")
        http.result=created(draft)
        val first=api.createProject(session,draft)
        assertEquals(first,api.createProject(session,draft))
        assertEquals("New project",first.name);assertEquals("P-02",first.code)
        assertEquals("POST",http.requests.first().first)
        assertEquals(http.requests.first().third,http.requests.last().third)
    }
    @Test fun `project deletion needs explicit confirmation and validates id revision and time`() {
        val http=Http();val api=CompanyApiClient(http);val action=CompanyProjectDelete(project,confirmation="DELETE")
        http.result=deleted(action)
        assertEquals(api.deleteProject(session,action),api.deleteProject(session,action))
        assertEquals("DELETE",http.requests.first().first)
        assertEquals(1,JSONObject(http.requests.first().third!!).getInt("expectedVersion"))
        val bad=JSONObject(deleted(action).body).put("id","22222222-2222-4222-8222-222222222222")
        http.result=HttpResponse(200,emptyMap(),bad.toString())
        try {api.deleteProject(session,action);fail()}catch(error:CompanyApiException){assertEquals("INVALID_RESPONSE",error.errorCode)}
    }
    @Test fun `deletion conflicts retain current company values for explicit review`() {
        val http=Http();val api=CompanyApiClient(http);val action=CompanyProjectDelete(project,confirmation="DELETE")
        val conflict=JSONObject().put("outcome","conflict").put("operationId",action.operationId).put("expectedVersion",1)
            .put("current",projectJson(project.copy(name="New company name",version=2)))
        fun response()=HttpResponse(409,emptyMap(),JSONObject().put("error",JSONObject().put("code","VERSION_CONFLICT")).put("conflict",conflict).toString())
        http.result=response()
        assertEquals(2,(api.deleteProject(session,action) as CompanyProjectDeleteResult.Conflict).current.version)
        conflict.put("operationId","22222222-2222-4222-8222-222222222222");http.result=response()
        try {api.deleteProject(session,action);fail()}catch(error:CompanyApiException){assertEquals("INVALID_RESPONSE",error.errorCode)}
    }
    @Test fun `invalid lifecycle details and missing confirmation never make requests`() {
        val http=Http();val api=CompanyApiClient(http)
        for(draft in listOf(CompanyProjectCreate(name=""),CompanyProjectCreate(name="x".repeat(241)),CompanyProjectCreate(name="Valid",code="x".repeat(81)))) {
            assertFalse(draft.valid)
            try {api.createProject(session,draft);fail()}catch(error:CompanyApiException){assertEquals("INVALID_PROJECT_ACTION",error.errorCode)}
        }
        for(action in listOf(CompanyProjectDelete(project),CompanyProjectDelete(project,confirmation="delete"),CompanyProjectDelete(project.copy(version=Int.MAX_VALUE),confirmation="DELETE"))) {
            assertFalse(action.valid)
            try {api.deleteProject(session,action);fail()}catch(error:CompanyApiException){assertEquals("INVALID_PROJECT_ACTION",error.errorCode)}
        }
        assertTrue(http.requests.isEmpty())
    }
    @Test fun `lifecycle actions retain no project details drafts confirmations or operations on disk`() {
        val values=object:KeyValues {
            val data=mutableMapOf<String,String>()
            override fun get(key:String)=data[key]
            override fun put(key:String,value:String){data[key]=value}
            override fun remove(key:String){data.remove(key)}
        }
        val box=object:SecretBox {override fun seal(plain:ByteArray)=plain;override fun open(sealed:ByteArray)=sealed}
        val store=CompanyAccountStore(values,box);store.connect(session.connection);store.save(session)
        val before=values.data.toMap();val http=Http();val account=CompanyAccount(store,CompanyApiClient(http))
        val draft=CompanyProjectCreate(name="Company project")
        http.result=created(draft);account.createProject(draft)
        http.result=deleted(CompanyProjectDelete(project,operationId="33333333-3333-4333-8333-333333333333",confirmation="DELETE"))
        account.deleteProject(CompanyProjectDelete(project,operationId="33333333-3333-4333-8333-333333333333",confirmation="DELETE"))
        assertEquals(before,values.data)
        http.result=null
        try {account.createProject(draft);fail()}catch(_:IOException){}
        assertNull(account.session());assertTrue(account.hasSavedSession());assertEquals(before,values.data)
    }
}
