package com.holagundi.archiman.sync

import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.net.InetAddress
import java.io.IOException
import java.util.concurrent.Executors

class FileResponseBoundTest {
 private fun <T> serve(size:Int,run:(String)->T):T {
  val server=ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));val worker=Executors.newSingleThreadExecutor()
  worker.submit {try{server.accept().use {socket->
   val input=socket.getInputStream().bufferedReader();while(true){val line=input.readLine() ?: break;if(line.isEmpty())break}
   val out=socket.getOutputStream();out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: $size\r\nConnection: close\r\n\r\n".toByteArray())
   val chunk=ByteArray(8192){65};var remaining=size;while(remaining>0){val count=minOf(remaining,chunk.size);out.write(chunk,0,count);remaining-=count};out.flush()
  }}catch(_:IOException){}}
  try{return run("http://127.0.0.1:${server.localPort}")}finally{server.close();worker.shutdownNow()}
 }
 @Test fun `default transport rejects responses larger than one MiB`() {
  serve(1_100_000){url->try{UrlConnectionTransport(allowDevelopmentLoopback=true).request("GET",url,emptyMap(),null);fail("Unbounded response accepted")}catch(error:IOException){assertTrue(error.message.orEmpty().contains("too large"))}}
 }
 @Test fun `file-enabled transport accepts bounded file envelopes without removing the response limit`() {
  serve(1_100_000){url->val response=UrlConnectionTransport(allowDevelopmentLoopback=true,maxResponseBytes=8*1024*1024).request("GET",url,emptyMap(),null);assertEquals(1_100_000,response.body.length)}
  try{UrlConnectionTransport(maxResponseBytes=9*1024*1024);fail()}catch(_:IllegalArgumentException){}
 }
}
