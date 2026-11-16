package com.holagundi.archiman.company

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/** No app permissions, credentials, app files or network access. Only one input FD and bounded raw pixels cross IPC. */
class CompanyFileRendererService:Service() {
 private val worker=Executors.newSingleThreadExecutor()
 private val binder=object:ICompanyFileRenderer.Stub() {
  override fun render(input:ParcelFileDescriptor,contentType:String,pageIndex:Int):ParcelFileDescriptor {
   if(Process.myUid()==applicationInfo.uid || Binder.getCallingUid()!=applicationInfo.uid)throw SecurityException("Isolated renderer required")
   val pipe=ParcelFileDescriptor.createPipe()
   worker.execute {
    try {ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use {stream->
     val output=DataOutputStream(stream)
     try {
      input.use {descriptor->
       val rendered=if(contentType=="application/pdf")renderPdf(descriptor,pageIndex) else renderImage(descriptor,contentType,pageIndex)
       val bitmap=rendered.second
       try {
        val size=bitmap.width*bitmap.height*4
        if(bitmap.width !in 1..1400 || bitmap.height !in 1..1400 || size>8*1024*1024 || bitmap.byteCount!=size)throw IllegalArgumentException()
        val pixels=ByteBuffer.allocate(size);bitmap.copyPixelsToBuffer(pixels)
        output.writeInt(rendered.first);output.writeInt(bitmap.width);output.writeInt(bitmap.height);output.writeInt(size);output.write(pixels.array());output.flush()
       } finally {bitmap.recycle()}
      }
     } catch(_:Exception){runCatching {output.writeInt(-1);output.flush()}}
    }} catch(_:Exception){runCatching {input.close()};runCatching {pipe[1].close()}}
   }
   return pipe[0]
  }
 }
 override fun onBind(intent:Intent?):IBinder=binder
 override fun onDestroy() {
  worker.shutdownNow();super.onDestroy()
  // End native decoding even if a malformed document outlives the caller's bounded request.
  if(Process.myUid()!=applicationInfo.uid)Process.killProcess(Process.myPid())
 }
 private fun renderPdf(input:ParcelFileDescriptor,pageIndex:Int):Pair<Int,Bitmap> {
  PdfRenderer(input).use {renderer->
   if(renderer.pageCount !in 1..500 || pageIndex !in 0 until renderer.pageCount)throw IllegalArgumentException()
   renderer.openPage(pageIndex).use {page->
    if(page.width<=0 || page.height<=0)throw IllegalArgumentException()
    val ratio=min(1400f/max(page.width,page.height),1f)
    val bitmap=Bitmap.createBitmap(max(1,(page.width*ratio).toInt()),max(1,(page.height*ratio).toInt()),Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.WHITE)
    try{page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)}catch(error:Exception){bitmap.recycle();throw error}
    return renderer.pageCount to bitmap
   }
  }
 }
 private fun renderImage(input:ParcelFileDescriptor,type:String,pageIndex:Int):Pair<Int,Bitmap> {
  if(type !in setOf("image/png","image/jpeg","image/webp") || pageIndex!=0)throw IllegalArgumentException()
  val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeFileDescriptor(input.fileDescriptor,null,bounds)
  if(bounds.outWidth<=0 || bounds.outHeight<=0)throw IllegalArgumentException()
  var sample=1;while(max(bounds.outWidth.toLong()/sample,bounds.outHeight.toLong()/sample)>1400)sample*=2
  android.system.Os.lseek(input.fileDescriptor,0,android.system.OsConstants.SEEK_SET)
  val decoded=BitmapFactory.decodeFileDescriptor(input.fileDescriptor,null,BitmapFactory.Options().apply {inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}) ?: throw IllegalArgumentException()
  try {
   val bitmap=Bitmap.createBitmap(decoded.width,decoded.height,Bitmap.Config.ARGB_8888)
   try{Canvas(bitmap).drawBitmap(decoded,0f,0f,null)}catch(error:Exception){bitmap.recycle();throw error}
   return 1 to bitmap
  } finally {decoded.recycle()}
 }
}
