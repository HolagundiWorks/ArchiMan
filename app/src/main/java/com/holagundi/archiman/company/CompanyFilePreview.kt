package com.holagundi.archiman.company

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import kotlin.math.min

class CompanyFilePreview(val content:CompanyFileContent,val pageIndex:Int,val pageCount:Int,val bitmap:Bitmap?=null,val text:String?=null) {
 val file get()=content.file
 override fun toString()="CompanyFilePreview(redacted)"
}
/** User-selected input is read once into bounded RAM; no URI permission, copy or draft is persisted. */
fun readSelectedCompanyFile(context:Context,uri:Uri):CompanySelectedFile {
 try {
  var name:String?=null
  context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)?.use {cursor->
   if(cursor.moveToFirst()) {
    name=cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))?.trim {it.isWhitespace() || it=='\uFEFF'}
    val index=cursor.getColumnIndex(OpenableColumns.SIZE)
    if(index>=0 && !cursor.isNull(index) && cursor.getLong(index)>COMPANY_MAX_FILE_BYTES)throw CompanyApiException("INVALID_FILE")
   }
  }
  val fileName=name ?: throw CompanyApiException("INVALID_FILE")
  val type=when(fileName.substringAfterLast('.',"").lowercase()){"pdf"->"application/pdf";"png"->"image/png";"jpg","jpeg"->"image/jpeg";"webp"->"image/webp";"txt"->"text/plain";"csv"->"text/csv";else->throw CompanyApiException("INVALID_FILE")}
  if(!validFileName(fileName,type))throw CompanyApiException("INVALID_FILE")
  val bytes=context.contentResolver.openInputStream(uri)?.use {input->
   val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true){val count=input.read(buffer);if(count<0)break;if(output.size()+count>COMPANY_MAX_FILE_BYTES)throw CompanyApiException("INVALID_FILE");output.write(buffer,0,count)}
   output.toByteArray()
  } ?: throw CompanyApiException("INVALID_FILE")
  if(bytes.isEmpty())throw CompanyApiException("INVALID_FILE")
  return CompanySelectedFile(fileName,type,bytes)
 } catch(error:CompanyApiException){throw error} catch(_:Exception){throw CompanyApiException("INVALID_FILE")}
}
/** Binary parsing runs in an isolated service; the app receives only bounded raw pixels. */
fun renderCompanyFile(context:Context,content:CompanyFileContent,pageIndex:Int=0):CompanyFilePreview {
 try {
  if(content.bytes.size!=content.file.size || fileHash(content.bytes)!=content.file.sha256)throw CompanyApiException("FILE_INTEGRITY_ERROR")
  if(content.file.contentType in setOf("text/plain","text/csv")) {
   if(pageIndex!=0)throw CompanyApiException("INVALID_FILE_PREVIEW")
   val text=content.bytes.toString(Charsets.UTF_8)
   val shown=text.take(50000).lineSequence().take(1000).joinToString("\n")
   return CompanyFilePreview(content,0,1,text=shown+if(shown.length<text.length)"\nPreview limited to 50,000 characters and 1,000 lines." else "")
  }
  return renderIsolated(context,content,pageIndex)
 } catch(error:CompanyApiException){throw error} catch(_:Exception){throw CompanyApiException("INVALID_FILE_PREVIEW")}
}
private fun renderIsolated(context:Context,content:CompanyFileContent,pageIndex:Int):CompanyFilePreview {
 val callbackThread=HandlerThread("Company file input").apply {start()}
 val reader=java.util.concurrent.Executors.newSingleThreadExecutor()
 val connected=java.util.concurrent.CountDownLatch(1)
 val remote=java.util.concurrent.atomic.AtomicReference<ICompanyFileRenderer?>()
 val output=java.util.concurrent.atomic.AtomicReference<ParcelFileDescriptor?>()
 val connection=object:android.content.ServiceConnection {
  override fun onServiceConnected(name:android.content.ComponentName,binder:android.os.IBinder){remote.set(ICompanyFileRenderer.Stub.asInterface(binder));connected.countDown()}
  override fun onServiceDisconnected(name:android.content.ComponentName){remote.set(null);connected.countDown()}
  override fun onNullBinding(name:android.content.ComponentName){connected.countDown()}
  override fun onBindingDied(name:android.content.ComponentName){remote.set(null);connected.countDown()}
 }
 var bound=false
 try {
  bound=context.bindService(android.content.Intent(context,CompanyFileRendererService::class.java),connection,Context.BIND_AUTO_CREATE)
  if(!bound || !connected.await(10,java.util.concurrent.TimeUnit.SECONDS))throw CompanyApiException("INVALID_FILE_PREVIEW")
  val renderer=remote.get() ?: throw CompanyApiException("INVALID_FILE_PREVIEW")
  val callback=object:ProxyFileDescriptorCallback() {
   override fun onGetSize()=content.bytes.size.toLong()
   override fun onRead(offset:Long,size:Int,data:ByteArray):Int {
    if(offset<0)throw android.system.ErrnoException("read",android.system.OsConstants.EINVAL)
    if(offset>=content.bytes.size)return 0
    val count=minOf(size,data.size,content.bytes.size-offset.toInt());content.bytes.copyInto(data,0,offset.toInt(),offset.toInt()+count);return count
   }
   override fun onRelease() {}
  }
  context.getSystemService(StorageManager::class.java).openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,callback,Handler(callbackThread.looper)).use {input->
   val future=reader.submit<CompanyFilePreview> {
    val pipe=renderer.render(input,content.file.contentType,pageIndex);output.set(pipe)
    java.io.DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(pipe)).use {stream->
     val pages=stream.readInt();val width=stream.readInt();val height=stream.readInt();val size=stream.readInt()
     if(pages !in 1..500 || pageIndex !in 0 until pages || width !in 1..1400 || height !in 1..1400 || size!=width*height*4 || size>8*1024*1024)throw CompanyApiException("INVALID_FILE_PREVIEW")
     val bytes=ByteArray(size);stream.readFully(bytes)
     val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
     try{bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))}catch(error:Exception){bitmap.recycle();throw error}
     CompanyFilePreview(content,pageIndex,pages,bitmap=bitmap)
    }
   }
   try{return future.get(20,java.util.concurrent.TimeUnit.SECONDS)}catch(error:Exception){future.cancel(true);throw error}
  }
 } finally {
  runCatching {output.get()?.close()}
  if(bound)runCatching {context.unbindService(connection)}
  reader.shutdownNow();callbackThread.quitSafely()
 }
}
