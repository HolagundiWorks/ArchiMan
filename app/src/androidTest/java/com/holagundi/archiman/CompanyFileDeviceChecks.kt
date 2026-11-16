package com.holagundi.archiman

import android.app.Instrumentation
import android.content.ComponentName
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.holagundi.archiman.company.*
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID

object CompanyFileDeviceChecks {
 data class Fixture(val name:String,val type:String,val bytes:ByteArray) {override fun toString()="Fixture(redacted)"}
 fun fixtures():List<Fixture> {
  val text=Fixture("demo-verification.txt","text/plain","Fictional attachment for live company verification.".toByteArray())
  val bitmap=Bitmap.createBitmap(24,16,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.BLUE)}
  val image=try {ByteArrayOutputStream().use {out->check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));Fixture("demo-verification.png","image/png",out.toByteArray())}} finally {bitmap.recycle()}
  val pdf=PdfDocument()
  val document=try {
   for(index in 1..2) {val page=pdf.startPage(PdfDocument.PageInfo.Builder(200,120,index).create());page.canvas.drawColor(Color.WHITE);page.canvas.drawRect(0f,0f,50f,50f,Paint().apply {color=if(index==1)Color.BLACK else Color.BLUE});pdf.finishPage(page)}
   ByteArrayOutputStream().use {out->pdf.writeTo(out);Fixture("demo-verification.pdf","application/pdf",out.toByteArray())}
  } finally {pdf.close()}
  return listOf(text,image,document)
 }
 fun workflow(instrumentation:Instrumentation,account:CompanyAccount) {
  val context=instrumentation.targetContext
  val project=account.liveProjects(null,null).page.projects.single {it.code=="DEMO-01"}
  check(account.fileStatus().available)
  val before=context.filesDir.walkTopDown().filter {it.isFile}.map {it.relativeTo(context.filesDir).path}.toSet()
  for(fixture in fixtures()) {
   val upload=CompanyFileUpload(project.id,fixture.name,fixture.type,fixture.bytes)
   var active:CompanyFile?=null
   try {
    active=account.uploadFile(upload);check(account.uploadFile(upload)==active)
    val snapshot=account.liveProjects(null,null,fileProjectId=project.id)
    check(snapshot.filePage!!.files.contains(active))
    val content=account.fileContent(active)
    val preview=renderCompanyFile(context,content)
    if(fixture.type=="application/pdf")check(preview.pageCount==2 && preview.bitmap!=null)
    if(fixture.type=="image/png")check(preview.bitmap?.getPixel(5,5)==Color.BLUE)
    if(fixture.type=="text/plain")check(preview.text==fixture.bytes.toString(Charsets.UTF_8))
    preview.bitmap?.recycle()
    val remove=CompanyFileDelete(active,confirmation="DELETE")
    check(account.deleteFile(remove) is CompanyFileResult.Deleted)
    check(account.deleteFile(remove) is CompanyFileResult.Deleted)
    try{account.fileContent(active);error("Deleted file was downloaded")}catch(error:CompanyApiException){check(error.errorCode=="FILE_NOT_FOUND")}
    check(account.liveProjects(null,null,fileProjectId=project.id).filePage!!.files.none {it.id==active.id})
    active=null
   } finally {active?.let {runCatching {account.deleteFile(CompanyFileDelete(it,confirmation="DELETE"))}}}
  }
  val after=context.filesDir.walkTopDown().filter {it.isFile}.map {it.relativeTo(context.filesDir).path}.toSet()
  check(after==before)
 }
 fun verifyRenderer(instrumentation:Instrumentation) {
  val context=instrumentation.targetContext
  val info=context.packageManager.getServiceInfo(ComponentName(context,CompanyFileRendererService::class.java),0)
  check(!info.exported && info.flags and ServiceInfo.FLAG_ISOLATED_PROCESS !=0)
  val before=context.filesDir.walkTopDown().filter {it.isFile}.map {it.relativeTo(context.filesDir).path}.toSet()
  val project=UUID.randomUUID().toString()
  for(fixture in fixtures()) {
   val sha=MessageDigest.getInstance("SHA-256").digest(fixture.bytes).joinToString("") {"%02x".format(it)}
   val file=CompanyFile(UUID.randomUUID().toString(),project,fixture.name,fixture.type,fixture.bytes.size,sha,1)
   val content=CompanyFileContent(file,fixture.bytes)
   val preview=renderCompanyFile(context,content)
   check(preview.pageIndex==0)
   when(fixture.type){
    "text/plain"->check(preview.text==fixture.bytes.toString(Charsets.UTF_8) && preview.bitmap==null)
    "image/png"->check(preview.bitmap?.getPixel(5,5)==Color.BLUE && preview.pageCount==1)
    "application/pdf"->{
     check(preview.pageCount==2 && preview.bitmap!=null && preview.bitmap.getPixel(10,10)!=Color.WHITE)
     val next=renderCompanyFile(context,content,1);check(next.pageIndex==1 && next.pageCount==2 && next.bitmap!=null && next.bitmap.getPixel(10,10)!=Color.WHITE);next.bitmap.recycle()
    }
   }
   preview.bitmap?.recycle()
  }
  val after=context.filesDir.walkTopDown().filter {it.isFile}.map {it.relativeTo(context.filesDir).path}.toSet()
  check(after==before)
 }
}
