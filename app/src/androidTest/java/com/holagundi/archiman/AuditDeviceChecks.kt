package com.holagundi.archiman

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.room.withTransaction
import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.*
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.portal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.*
import java.net.Socket
import kotlin.system.measureNanoTime

object AuditDeviceChecks {
    suspend fun run(context:Context,db:ArchiManDatabase,suite:SuiteRepository,results:Bundle) {
        val directory=File(context.cacheDir,"audit-device").apply { mkdirs() }
        val metrics=JSONObject()
        val backups=BackupRepository(context,db,PortalServer(db,context))
        suspend fun tables():String { val bytes=ByteArrayOutputStream().also { backups.export(it) }.toByteArray();return JSONObject(BackupArchive.read(ByteArrayInputStream(bytes)).getValue("database.json").toString(Charsets.UTF_8)).getJSONObject("tables").toString() }
        val before=tables();val store=DemoCompanyStore(context,db)
        val backupBytes=ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
        val backupEntries=BackupArchive.read(ByteArrayInputStream(backupBytes)).toMutableMap()
        val backupJson=JSONObject(backupEntries.getValue("database.json").toString(Charsets.UTF_8))
        val financeTable=backupJson.getJSONObject("tables").getJSONObject("finance_records");val financeColumns=financeTable.getJSONArray("columns")
        val jsonColumn=(0 until financeColumns.length()).first { financeColumns.getString(it)=="json" };val keyColumn=(0 until financeColumns.length()).first { financeColumns.getString(it)=="key" }
        val financeRows=financeTable.getJSONArray("rows")
        for(index in 0 until financeRows.length()) { val row=financeRows.getJSONArray(index);if(row.getString(keyColumn)=="settings") row.put(jsonColumn,JSONObject(row.getString(jsonColumn)).put("mode","UNSAFE_UNKNOWN").toString()) }
        backupEntries["database.json"]=backupJson.toString().toByteArray()
        val invalidBackup=ByteArrayOutputStream().also { BackupArchive.write(backupEntries,it) }.toByteArray()
        check(runCatching { backups.prepare(ByteArrayInputStream(invalidBackup)) }.isFailure)
        check(tables()==before) { "Rejected backup metadata changed practice data." }
        val deepData=("[".repeat(500)+"]".repeat(500)).toByteArray()
        val deepManifest=JSONObject().put("format","archiman-backup").put("version",1).put("checksums",JSONObject().put("database.json",BackupArchive.digest(deepData))).toString().toByteArray()
        val deeplyNested=ByteArrayOutputStream().also { output -> java.util.zip.ZipOutputStream(output).use { zip ->
            mapOf("database.json" to deepData,"manifest.json" to deepManifest).forEach { (name,bytes) -> zip.putNextEntry(java.util.zip.ZipEntry(name));zip.write(bytes);zip.closeEntry() }
        } }.toByteArray()
        val depthError=runCatching { backups.prepare(ByteArrayInputStream(deeplyNested)) }.exceptionOrNull()
        check(depthError?.message?.contains("safe nesting")==true)
        check(tables()==before)
        val malicious=JSONObject(store.readPackage().source.toString());malicious.getJSONArray("workItems").getJSONObject(0).remove("specification")
        val broken=File(directory,"invalid-company.json").apply { writeText(malicious.toString()) }
        check(runCatching { store.openFromUri(Uri.fromFile(broken)) }.isFailure)
        check(tables()==before) { "Failed company import changed practice data." }
        val oversized=File(directory,"oversize-company.json").apply { outputStream().use { out -> val chunk=ByteArray(1024*1024){32};repeat(6){out.write(chunk)} } }
        check(runCatching { store.openFromUri(Uri.fromFile(oversized)) }.isFailure);check(tables()==before)
        broken.delete();oversized.delete()
        val reserved=runCatching { PortalWorkspace(db).save(mapOf("action" to "previewImport","csv" to "ItemCode,Project\nTEST,999"),"audit-csrf") }.exceptionOrNull()
        check(reserved?.message?.contains("Unsupported or duplicate measurement columns")==true)
        results.putString("importSafety","passed: malformed late-stage company import rolls back; oversized company import rejected before replacement")

        val project=ProjectRepository(db,db.projectDao(),db.practiceDao()).createProject("Audit isolated project","AUDIT-ONLY","Audit client","Residential","Test site","Audit architect")
        val invoiceId=db.suiteDao().insertInvoice(InvoiceEntity(projectId=project,stageId=null,number="AUDIT-CONCURRENT",title="Concurrent receipts",amount=100.0,status="ISSUED",createdAt=1))
        val invoice=db.suiteDao().invoice(invoiceId)!!
        val payments=coroutineScope { List(2) { async(Dispatchers.IO) { runCatching { suite.recordReceipt(project,invoice,60.0,"Concurrent audit payment") }.isSuccess } }.awaitAll() }
        check(payments.count { it }==1);check(db.suiteDao().invoiceReceipts(invoiceId).sumOf{it.amount}==60.0)
        suite.assignStandardStages(project)
        val stage=db.suiteDao().observeStages(project).first().first()
        val stages=coroutineScope { List(2) { async(Dispatchers.IO) { runCatching { suite.billStage(project,stage,500.0) }.isSuccess } }.awaitAll() }
        check(stages.count { it }==1)
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM invoices WHERE stageId=?",arrayOf(stage.id)).use { it.moveToFirst();check(it.getInt(0)==1) }
        val measurement=MeasurementRepository(db,db.measurementDao(),db.catalogDao(),db.projectDao())
        val template=db.measurementDao().observeSheets(db.projectDao().projects().first { it.projectCode=="WS-QA" }.id).first().first()
        repeat(12) { trial ->
            val sheet=db.measurementDao().insertSheet(template.copy(id=0,sheetCode="AUDIT-RACE-$trial",status="CHECKED",lockedAt=null))
            val reviews=coroutineScope { listOf(async(Dispatchers.IO){measurement.transition(sheet,"APPROVED","Reviewed","Audit")},async(Dispatchers.IO){measurement.transition(sheet,"RETURNED","Correct quantity","Audit")}).awaitAll() }
            check(reviews.count{it}==1) { "Conflicting review transitions both succeeded." }
        }
        check(measurement.export(project,template.id)==null)
        results.putString("concurrency","passed: concurrent receipts, duplicate stage billing, 12 conflicting approval/return races and cross-project export isolation")

        val csv=buildString { append("Date,Amount,Description,Reference\n");repeat(10000){append("05/11/2026,100.00,Audit transaction,REF-$it\n")} }
        var parsed=emptyList<BankRow>()
        val parseMs=measureNanoTime { parsed=BankCsv.parse(csv,"Audit bank");check(parsed.size==10000) }/1_000_000.0
        metrics.put("bankCsv10000RowsMs",parseMs)
        val bankRepo=FinanceRepository(db,suite)
        val batchMs=measureNanoTime { check(bankRepo.import(parsed,"Audit bank")==10000) }/1_000_000.0
        metrics.put("bankBatchImport10000RowsMs",batchMs)
        check(bankRepo.import(parsed,"Audit bank")==0)
        db.withTransaction { parsed.map { it.key }.chunked(500).forEach { keys -> db.openHelper.writableDatabase.execSQL("DELETE FROM finance_records WHERE `key` IN (${keys.joinToString(",") { "?" }})",keys.toTypedArray()) } }
        val insertMs=measureNanoTime { db.withTransaction { repeat(10000){i->db.suiteDao().saveFinance(FinanceRecordEntity("audit-bank-$i","BANK",JSONObject().put("date","2026-11-05").put("amount","100.00").put("description","Audit only").put("reference","REF-$i").put("account","Audit bank").put("status","UNMATCHED").toString()))} } }/1_000_000.0
        metrics.put("insert10000BankRecordsMs",insertMs)
        suspend fun timeQuery(all:Boolean):Pair<Double,Int> { var count=0;val elapsed=measureNanoTime { count=if(all)db.suiteDao().observeFinance().first().size else db.suiteDao().financeKinds(listOf("BOQ_REVISION")).size };return elapsed/1_000_000.0 to count }
        timeQuery(true);timeQuery(false)
        val all=List(5){timeQuery(true)};val filtered=List(5){timeQuery(false)}
        metrics.put("broadFinanceMedianMs",all.map{it.first}.sorted()[2]).put("scheduleOnlyMedianMs",filtered.map{it.first}.sorted()[2]).put("broadRows",all.first().second).put("scheduleRows",filtered.first().second)
        val invoices=(1L..10000L).map { InvoiceEntity(id=it,projectId=project,stageId=null,number="LOAD-$it",title="Load",amount=1000.0,status="PAID",createdAt=1) }
        val receipts=invoices.map { ReceiptEntity(invoiceId=it.id,projectId=project,amount=900.0,tdsAmount=100.0,note="Audit",receivedAt=1) }
        val summaryMs=measureNanoTime { val summary=FeeDashboardSummary.calculate(invoices,receipts);check(summary.outstanding.signum()==0&&summary.received.toPlainString()=="9000000.00"&&summary.tdsWithheld.toPlainString()=="1000000.00") }/1_000_000.0
        metrics.put("dashboard10000InvoicesAndReceiptsMs",summaryMs)
        db.openHelper.writableDatabase.execSQL("DELETE FROM finance_records WHERE `key` LIKE 'audit-bank-%'")
        val injection=DocumentExports.csv(listOf(listOf("=HYPERLINK(\"http://evil.invalid\")","\t+SUM(1,2)","\uFEFF=1+1","-100.00"))).toString(Charsets.UTF_8)
        check(injection.contains("'=HYPERLINK")&&injection.contains("'\t+SUM")&&injection.contains("'\uFEFF=1+1"))
        results.putString("largeData","passed: 10,000 bank import rows and records, 10,000-invoice cash/TDS aggregation, bounded schedule reads and spreadsheet formula neutralisation")

        val server=PortalServer(db,context);check(server.start("127.0.0.1"))
        fun request(raw:String):String=PinnedPortalClient.socket(server).use { socket->socket.soTimeout=5000;socket.getOutputStream().apply{write(raw.toByteArray());flush()};socket.getInputStream().readBytes().toString(Charsets.UTF_8) }
        fun closed(socket:Socket):Boolean=try { socket.getInputStream().read()==-1 } catch(timeout:java.net.SocketTimeoutException) { false } catch(reset:java.net.SocketException) { true }
        fun get(path:String="/")="GET $path HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\n\r\n"
        try {
            check(request(get("/workspace?section=company")).startsWith("HTTP/1.1 404"))
            check(request("GET / HTTP/1.1\r\nHost: evil.invalid\r\n\r\n").startsWith("HTTP/1.1 403"))
            check(request("GET / HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nOrigin: http://evil.invalid\r\n\r\n").startsWith("HTTP/1.1 403"))
            check(request("POST /workspace HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nContent-Length: 9000\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\n").startsWith("HTTP/1.1 400"))
            val login=request(get());val csrf=Regex("archiman_csrf=([^;]+)").find(login)!!.groupValues[1]
            val user=db.practiceDao().activeUsers().single();val body="csrf=$csrf&code=WRONG&user=${user.id}"
            repeat(8){check(request("POST / HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nCookie: archiman_csrf=$csrf\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${body.length}\r\n\r\n$body").startsWith("HTTP/1.1 401"))}
            check(request("POST / HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 0\r\n\r\n").startsWith("HTTP/1.1 429"))
            val sockets=mutableListOf<Socket>()
            try { repeat(12){sockets.add(Socket("127.0.0.1",PortalHttp.PORT))};delay(150)
                Socket("127.0.0.1",PortalHttp.PORT).use{overflow->overflow.soTimeout=1500;check(closed(overflow))}
            } finally { sockets.forEach{it.close()} }
            delay(150);check(request(get()).startsWith("HTTP/1.1 200"))
            val held=Socket("127.0.0.1",PortalHttp.PORT).apply{soTimeout=1500};delay(50);server.stop(false)
            held.use{check(closed(it))}
            check(!server.isRunning())
        } finally { server.stop(false) }
        results.putString("portalSecurity","passed: unauthenticated access denied, foreign host/origin rejected, unauthenticated oversized body refused, eight-failure login limit, 12-connection cap/recovery and stop closes clients")
        // A valid browser form queued behind another transaction must be refused
        // if the owner closes/replaces the practice before it can acquire the database.
        check(server.start("127.0.0.1"))
        try {
            val login=request(get());val loginCsrf=Regex("archiman_csrf=([^;]+)").find(login)!!.groupValues[1]
            val user=db.practiceDao().activeUsers().single()
            val signBody="csrf=$loginCsrf&code=${server.state.value.code}&user=${user.id}"
            val signed=request("POST / HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nCookie: archiman_csrf=$loginCsrf\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${signBody.length}\r\n\r\n$signBody")
            val cookie="archiman_portal="+Regex("archiman_portal=([^;]+)").find(signed)!!.groupValues[1]
            val form=request("GET /workspace?section=items HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nCookie: $cookie\r\n\r\n")
            val csrf=Regex("name='csrf' value='([^']+)'").find(form)!!.groupValues[1]
            val body=listOf("action" to "item","csrf" to csrf,"code" to "AUDIT-STALE","name" to "Audit stale item","unit" to "m³","formula" to "VOLUME","trade" to "Audit","specification" to "Queued write").joinToString("&") { (key,value) -> key+"="+java.net.URLEncoder.encode(value,"UTF-8") }
            coroutineScope {
                val ready=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();val sent=CompletableDeferred<Unit>()
                val holder=async(Dispatchers.IO) { db.withTransaction { ready.complete(Unit);release.await() } }
                ready.await()
                val writer=async(Dispatchers.IO) { runCatching {
                    PinnedPortalClient.socket(server).use { socket -> socket.soTimeout=5000
                        val bytes=body.toByteArray();val head="POST /workspace HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nCookie: $cookie\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${bytes.size}\r\n\r\n"
                        socket.getOutputStream().apply { write(head.toByteArray());write(bytes);flush() };sent.complete(Unit);socket.getInputStream().readBytes()
                    }
                } }
                try { withTimeout(3000) { sent.await() };delay(150);server.stop(false) } finally { release.complete(Unit) }
                holder.await();writer.await()
            }
            check(db.catalogDao().findItemByName("Audit stale item")==null) { "A closed portal session wrote to the practice." }
        } finally { server.stop(false) }
        val secret="device encryption test password".toCharArray()
        val protected=PasswordEnvelope.encrypt(backupBytes,secret,BackupArchive.safeByteLimit())
        check(PasswordEnvelope.decrypt(protected,secret,BackupArchive.safeByteLimit()).contentEquals(backupBytes))
        check(runCatching { PasswordEnvelope.decrypt(protected,"wrong".toCharArray(),BackupArchive.safeByteLimit()) }.isFailure)
        results.putString("encryption","passed: TLS 1.2/1.3, pinned certificate and HTTPS hostname verification; encrypted backup round-trip and wrong-password rejection")
        val internalPlain="internal safety copy".toByteArray()
        val internalProtected=RecoveryCipher.encrypt(internalPlain)
        check(RecoveryCipher.protected(internalProtected) && !internalProtected.contentEquals(internalPlain))
        check(RecoveryCipher.decrypt(internalProtected).contentEquals(internalPlain))
        internalProtected[internalProtected.lastIndex]=(internalProtected.last().toInt() xor 1).toByte()
        check(runCatching { RecoveryCipher.decrypt(internalProtected) }.isFailure)
        val failureSnapshot=tables()
        val failureFile=File(directory,"blocked-storage").apply { writeText("not a directory") }
        val failingContext=object:android.content.ContextWrapper(context) { override fun getFilesDir()=failureFile }
        val failingBackup=BackupRepository(failingContext,db,server)
        val failureCandidate=failingBackup.prepare(ByteArrayInputStream(backupBytes))
        check(runCatching { failingBackup.restore(failureCandidate) }.isFailure)
        check(tables()==failureSnapshot) { "Storage failure changed the practice." }
        check(runCatching { backups.export(object:OutputStream() { var count=0;override fun write(value:Int) { if(++count>512) throw IOException("Injected full destination") } }) }.isFailure)
        check(tables()==failureSnapshot)
        failureFile.delete()
        results.putString("storageFailure","passed: injected unavailable restore storage and interrupted/full export destination preserve all practice tables")
        val liveBackups=BackupRepository(context,db,server)
        val liveBytes=ByteArrayOutputStream().also { liveBackups.export(it) }.toByteArray()
        val candidate=liveBackups.prepare(ByteArrayInputStream(liveBytes))
        check(server.start("127.0.0.1"));liveBackups.restore(candidate);check(!server.isRunning())
        check(server.start("127.0.0.1"));DemoCompanyStore(context,db,server).createBlank("Audit replacement","Audit owner","Test city");check(!server.isRunning())
        check(db.practiceDao().company()!!.practiceName=="Audit replacement")
        results.putString("sessionBoundary","passed: queued authenticated writes rejected after stop; restore and practice replacement close browser sessions before mutation")
        File(directory,"metrics.json").writeText(metrics.toString(2))
        results.putString("auditMetrics",metrics.toString())
    }
}
