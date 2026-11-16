package com.holagundi.archiman.company

import java.util.UUID
import java.util.Base64
import java.security.MessageDigest
import org.json.JSONObject

const val COMPANY_MAX_FILE_BYTES=5*1024*1024
val companyFileTypes=setOf("application/pdf","image/png","image/jpeg","image/webp","text/plain","text/csv")
data class CompanyFile(val id:String,val projectId:String,val name:String,val contentType:String,val size:Int,val sha256:String,val version:Int)
data class CompanyFilePage(val projectId:String,val files:List<CompanyFile>,val nextCursor:String?)
data class CompanyFileStatus(val available:Boolean,val maxBytes:Int,val contentTypes:Set<String>)
class CompanySelectedFile(val name:String,val contentType:String,bytes:ByteArray) {
 private val data=bytes.copyOf()
 val size get()=data.size
 fun upload(projectId:String)=CompanyFileUpload(projectId,name,contentType,data)
 override fun toString()="CompanySelectedFile(redacted)"
}
class CompanyFileUpload(val projectId:String,val name:String,val contentType:String,bytes:ByteArray,
    val id:String=UUID.randomUUID().toString(),val operationId:String=UUID.randomUUID().toString()) {
 private val data=bytes.copyOf()
 val size get()=data.size
 val sha256:String by lazy {fileHash(data)}
 fun encoded()=Base64.getEncoder().encodeToString(data)
 val valid get()=validClientId(projectId) && validClientId(id) && validClientId(operationId) && validFileName(name,contentType) && size in 1..COMPANY_MAX_FILE_BYTES
 override fun toString()="CompanyFileUpload(redacted)"
}
data class CompanyFileDelete(val base:CompanyFile,val operationId:String=UUID.randomUUID().toString(),val confirmation:String="") {
 val valid get()=validClientId(base.id) && validClientId(base.projectId) && validClientId(operationId) && base.version in 1 until Int.MAX_VALUE && confirmation=="DELETE"
}
sealed interface CompanyFileResult {
 data class Uploaded(val file:CompanyFile):CompanyFileResult
 data class Deleted(val id:String):CompanyFileResult
 data class Conflict(val current:CompanyFile):CompanyFileResult
}
class CompanyFileContent(val file:CompanyFile,internal val bytes:ByteArray) {
 override fun toString()="CompanyFileContent(redacted)"
}
internal fun fileHash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
internal fun validFileName(name:String,type:String):Boolean {
 if(name!=name.trim {it.isWhitespace() || it=='\uFEFF'} || name.length !in 1..180 || name.any {it.code<32 || it.code==127 || it=='/' || it=='\\'})return false
 val ext=name.substringAfterLast('.',"").lowercase()
 return when(type){"application/pdf"->ext=="pdf";"image/png"->ext=="png";"image/jpeg"->ext in setOf("jpg","jpeg");"image/webp"->ext=="webp";"text/plain"->ext=="txt";"text/csv"->ext=="csv";else->false}
}
internal fun readCompanyFile(row:JSONObject,projectId:String):CompanyFile {
 val size=row.getLong("size");val version=row.getLong("version")
 if(size !in 1..COMPANY_MAX_FILE_BYTES.toLong() || version !in 1..Int.MAX_VALUE.toLong())throw CompanyApiException("INVALID_RESPONSE")
 val file=CompanyFile(row.getString("id"),row.getString("projectId"),row.getString("name"),row.getString("contentType"),size.toInt(),row.getString("sha256"),version.toInt())
 if(!validClientId(file.id) || file.projectId!=projectId || !validClientId(projectId) || !validFileName(file.name,file.contentType) || !file.sha256.matches(Regex("[a-f0-9]{64}")))throw CompanyApiException("INVALID_RESPONSE")
 return file
}
internal fun readCompanyFilePage(page:JSONObject,projectId:String,after:String?):CompanyFilePage {
 if(page.getString("projectId")!=projectId)throw CompanyApiException("INVALID_RESPONSE")
 val rows=page.getJSONArray("files");if(rows.length()>25)throw CompanyApiException("INVALID_RESPONSE")
 val files=(0 until rows.length()).map {readCompanyFile(rows.getJSONObject(it),projectId)}
 if(files.map {it.id}.distinct().size!=files.size)throw CompanyApiException("INVALID_RESPONSE")
 val next=if(page.isNull("nextCursor"))null else page.getString("nextCursor")
 if(next!=null && (!validClientId(next) || next==after || files.size!=25 || files.lastOrNull()?.id!=next))throw CompanyApiException("INVALID_RESPONSE")
 return CompanyFilePage(projectId,files,next)
}
