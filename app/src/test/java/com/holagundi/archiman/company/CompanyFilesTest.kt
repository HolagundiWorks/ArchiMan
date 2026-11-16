package com.holagundi.archiman.company

import com.holagundi.archiman.sync.HttpResponse
import com.holagundi.archiman.sync.HttpTransport
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class CompanyFilesTest {
 private val project="11111111-1111-4111-8111-111111111111"
 private val bytes="hello".toByteArray()
 private val upload=CompanyFileUpload(project,"demo.txt","text/plain",bytes)
 private val file=CompanyFile(upload.id,project,upload.name,upload.contentType,bytes.size,upload.sha256,1)
 private val session=CompanySession(CompanyConnection("https://company.example","/api/auth"),"private-test",CompanyIdentity("u","demo@example.test","Demo","Company",listOf("Project Manager"),listOf("files.write"),false))
 private class Http:HttpTransport {
  var count=0;var body:String?=null;var method="";var response=HttpResponse(200,emptyMap(),"{}")
  override fun request(method:String,url:String,headers:Map<String,String>,body:String?):HttpResponse {count++;this.body=body;this.method=method;return response}
 }
 private fun row(value:CompanyFile=file)=JSONObject().put("id",value.id).put("projectId",value.projectId).put("name",value.name).put("contentType",value.contentType).put("size",value.size).put("sha256",value.sha256).put("version",value.version)
 private fun invalid(run:()->Unit) {try{run();fail("Accepted invalid company file")}catch(error:CompanyApiException){assertTrue(error.errorCode in listOf("INVALID_RESPONSE","INVALID_FILE","INVALID_FILE_ACTION","FILE_CHANGED","FILE_INTEGRITY_ERROR"))}}
 @Test fun `uploads retain immutable byte copies identifiers and retry bodies`() {
  assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",upload.sha256)
  val original=byteArrayOf(65,66,67);val draft=CompanyFileUpload(project,"copy.txt","text/plain",original);original[0]=90;assertEquals("QUJD",draft.encoded())
  assertFalse(draft.toString().contains("QUJD"))
  val http=Http();val api=CompanyApiClient(http)
  http.response=HttpResponse(200,emptyMap(),JSONObject().put("outcome","uploaded").put("operationId",upload.operationId).put("file",row()).toString())
  assertEquals(file,api.uploadFile(session,upload));val body=http.body;assertEquals(file,api.uploadFile(session,upload));assertEquals(body,http.body)
  assertEquals(upload.operationId,JSONObject(body!!).getString("operationId"));assertEquals("POST",http.method)
 }
 @Test fun `downloaded bytes must match metadata size and sha256 before preview`() {
  val http=Http();val api=CompanyApiClient(http);val body=JSONObject().put("file",row()).put("contentBase64",upload.encoded())
  http.response=HttpResponse(200,emptyMap(),body.toString());assertEquals(file,api.fileContent(session,file).file)
  body.put("contentBase64",java.util.Base64.getEncoder().encodeToString("wrong".toByteArray()));http.response=HttpResponse(200,emptyMap(),body.toString());invalid{api.fileContent(session,file)}
  body.getJSONObject("file").put("name","changed.txt");http.response=HttpResponse(200,emptyMap(),body.toString());invalid{api.fileContent(session,file)}
 }
 @Test fun `unsafe names unsupported types oversized files and missing confirmation never reach network`() {
  val http=Http();val api=CompanyApiClient(http)
  for(draft in listOf(CompanyFileUpload(project,"../demo.txt","text/plain",bytes),CompanyFileUpload(project,"bad.pdf","text/plain",bytes),CompanyFileUpload(project,"demo.txt","text/plain",ByteArray(0)),CompanyFileUpload(project,"demo.txt","text/plain",ByteArray(COMPANY_MAX_FILE_BYTES+1)))) {
   assertFalse(draft.valid);invalid{api.uploadFile(session,draft)}
  }
  invalid{api.deleteFile(session,CompanyFileDelete(file))};assertEquals(0,http.count)
 }
 @Test fun `file pages reject wrong projects duplicates sizes hashes and stalled cursors`() {
  fun page(vararg files:JSONObject)=JSONObject().put("projectId",project).put("files",JSONArray(files.toList())).put("nextCursor",JSONObject.NULL)
  assertEquals(file,readCompanyFilePage(page(row()),project,null).files.single())
  invalid{readCompanyFilePage(page(row(),row()),project,null)}
  invalid{readCompanyFilePage(page(row().put("size",COMPANY_MAX_FILE_BYTES+1)),project,null)}
  invalid{readCompanyFilePage(page(row().put("sha256","bad")),project,null)}
  invalid{readCompanyFilePage(page(row()).put("nextCursor",file.id),project,null)}
 }
 @Test fun `file deletion validates the stable operation identity revision and timestamp`() {
  val http=Http();val api=CompanyApiClient(http);val action=CompanyFileDelete(file,confirmation="DELETE")
  val result=JSONObject().put("outcome","deleted").put("operationId",action.operationId).put("id",file.id).put("projectId",project).put("version",2).put("deletedAt","2026-11-17T00:00:00.000Z")
  http.response=HttpResponse(200,emptyMap(),result.toString());assertTrue(api.deleteFile(session,action) is CompanyFileResult.Deleted)
  result.put("version",3);http.response=HttpResponse(200,emptyMap(),result.toString());invalid{api.deleteFile(session,action)}
 }
}
