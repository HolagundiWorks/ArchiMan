package com.holagundi.archiman
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.*
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.portal.*
import kotlinx.coroutines.flow.first
import java.io.*
import org.json.JSONObject
import java.util.zip.ZipInputStream
object WorkspaceDeviceChecks {
    suspend fun run(context:Context,db:ArchiManDatabase) {
        val projects=ProjectRepository(db,db.projectDao(),db.practiceDao());val project=projects.createProject("Workspace QA project","WS-QA","QA client","Residential","QA site","QA architect")
        val floor=db.projectDao().observeFloors(project).first().firstOrNull() ?: run { projects.addFloor(project,"Ground floor");db.projectDao().observeFloors(project).first().first() }
        val contractor=db.catalogDao().insertContractor(ContractorEntity(projectId=project,name="QA Contractor",address="QA address",contactNo="",phone="",contractorType="Civil",createdAt=1))
        CpwdCatalogue.seed(db); CpwdCatalogue.seed(db)
        val catalogue="Code,Name,Description,Unit,Formula,Edition,SourceItemCode,Trade\nQA-AREA,QA flooring,Full QA flooring specification,m²,AREA,Practice QA,,Flooring"
        check(CpwdCatalogue.import(db,catalogue)==1);check(CpwdCatalogue.import(db,catalogue)==0)
        check(runCatching{CpwdCatalogue.parse(catalogue.replace("m²,AREA","m³,AREA"))}.isFailure)
        val item=db.catalogDao().observeActiveItems().first().first{it.itemCode=="CPWD23-4.1.3"}
        val repo=ProjectScheduleRepository(db);val r1=repo.create(project);repo.line(r1.key,contractor,item.id,"QA concrete", "m³","100","LABOUR_ONLY");repo.activate(r1.key)
        val first=repo.active(project)!!;val line=first.lines.single()
        val measurement=MeasurementRepository(db,db.measurementDao(),db.catalogDao(),db.projectDao())
        val draft=MeasurementDraft(project,"WS-QA",floor.id,floor.name,null,null,contractor,"QA Contractor",item.id,line.description,line.unit,line.formula,"Concrete slab","Zone A","",2.0,3.0,0.15,2.0,0.0,first.label,line.key,100.0,line.basis)
        check(measurement.record(draft) is RecordResult.Saved)
        check(db.measurementDao().measurementsNow(project).single().quantity==1.8)
        check(runCatching{repo.line(first.key,contractor,item.id,"Illegal change","m³","999","WITH_MATERIAL")}.isFailure)
        val second=repo.create(project);repo.remove(second.key,second.lines.single().key);repo.line(second.key,contractor,item.id,"QA concrete","m³","120","WITH_MATERIAL");repo.activate(second.key)
        val active=repo.active(project)!!;check(active.label=="R002" && repo.revisions(project).count{it.status=="ACTIVE"}==1)
        check(measurement.record(draft) is RecordResult.Rejected)
        val current=active.lines.single();check(measurement.record(draft.copy(boqRevision=active.label,boqLineKey=current.key,rateSnapshot=120.0,contractBasis=current.basis)) is RecordResult.Saved)
        check(db.measurementDao().observeSheets(project).first().size==2)
        check(db.measurementDao().measurementsNow(project).first{it.boqRevision=="R001"}.rateSnapshot==100.0)
        val workspace=PortalWorkspace(db)
        val csv="ItemCode,Description,Nos,Length,Breadth,Height,Deduction\n${item.itemCode},Imported feet,1,3.280839895013123,3.280839895013123,3.280839895013123,0"
        val fields=mapOf("action" to "previewImport","project" to project.toString(),"floor" to floor.id.toString(),"contractor" to contractor.toString(),"sourceSystem" to "IMPERIAL","csv" to csv)
        check(workspace.save(fields,"qa-csrf").contains("Review 1 measurement rows"));check(db.measurementDao().measurementsNow(project).size==2)
        workspace.save(fields+("action" to "import"),"qa-csrf");check(db.measurementDao().measurementsNow(project).first{it.description=="Imported feet"}.quantity==1.0)
        workspace.save(fields+("action" to "import"),"qa-csrf");check(db.measurementDao().measurementsNow(project).size==3)
        val invalid=fields+("csv" to csv.replace("3.280839895013123","bad"));check(runCatching{workspace.save(invalid,"qa-csrf")}.isFailure)
        db.openHelper.writableDatabase.execSQL("UPDATE measurement_sheets SET status='APPROVED' WHERE projectId=?",arrayOf(project))
        val bills=PortalContractorBills(db);bills.save(mapOf("action" to "billCreate","project" to project.toString(),"contractor" to contractor.toString()),"qa-csrf")
        val record=db.suiteDao().observeFinance().first().first{it.kind=="CONTRACTOR_BILL"&&JSONObject(it.json).getLong("projectId")==project}
        check(JSONObject(record.json).getString("amount")=="516.00")
        bills.save(mapOf("action" to "billIssue","project" to project.toString(),"key" to record.key,"reviewed" to "yes"),"qa-csrf")
        check(runCatching{bills.save(mapOf("action" to "billCreate","project" to project.toString(),"contractor" to contractor.toString()),"qa-csrf")}.isFailure)
        val bill=workspace.export(mapOf("kind" to "contractorbill","id" to record.key,"format" to "csv"));check(bill.bytes.toString(Charsets.UTF_8).contains("516.00"));check(!bill.bytes.toString(Charsets.UTF_8).contains("CGST"))
        val png=ByteArrayOutputStream();Bitmap.createBitmap(120,60,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.rgb(194,65,12));compress(Bitmap.CompressFormat.PNG,100,png);recycle()}
        val logo="data:image/png;base64,"+Base64.encodeToString(png.toByteArray(),Base64.NO_WRAP)
        val style=JSONObject().put("logo",logo).put("accent","#C2410C").put("layout","LOGO_LEFT").put("header","Architecture | Interiors | Landscape").put("footer","QA bank details | QA registration")
        db.suiteDao().saveFinance(FinanceRecordEntity("letterhead","LETTERHEAD",style.toString()))
        val company=db.practiceDao().company()!!;db.practiceDao().upsertCompany(company.copy(practiceName="QA Studio",legalName="QA Studio",address="12 Example Road",city="Bengaluru",pan="ABCDE1234F",gstin="",phone="1234567890",email="qa@example.test",website="example.test"))
        val artifacts=File(context.cacheDir,"workspace-qa").apply{mkdirs()}
        for(format in listOf("pdf","xlsx","csv")) {
            val download=workspace.export(mapOf("kind" to "measurements","project" to project.toString(),"format" to format));File(artifacts,"measurement.$format").writeBytes(download.bytes)
            if(format=="xlsx") {val entries=mutableListOf<String>();ZipInputStream(ByteArrayInputStream(download.bytes)).use{zip->while(true){val entry=zip.nextEntry?:break;entries.add(entry.name);if(entry.name.endsWith("sheet1.xml")){val body=zip.readBytes().toString(Charsets.UTF_8);check(body.contains("R001")&&body.contains("R002"))}}};check("xl/workbook.xml" in entries)}
        }
        File(artifacts,"company.html").writeText(workspace.page("qa-csrf","company",null))
        File(artifacts,"measure.html").writeText(workspace.page("qa-csrf","measure",project,query=mapOf("contractor" to contractor.toString())))
        File(artifacts,"schedule.html").writeText(workspace.page("qa-csrf","schedule",project))
        File(artifacts,"workspace.css").writeText(PortalWorkspaceAssets.css)
        File(artifacts,"workspace.js").writeText(PortalWorkspaceAssets.js)
        val pdf=File(artifacts,"measurement.pdf");ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY).use{fd->PdfRenderer(fd).use{renderer->check(renderer.pageCount>=1);renderer.openPage(0).use{page->val bitmap=Bitmap.createBitmap(page.width*2,page.height*2,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);File(artifacts,"letterhead-preview.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}}}
        check(workspace.page("qa-csrf","measure",project,query=mapOf("contractor" to contractor.toString())).contains(current.key))
        val submission=mapOf("action" to "scheduleCreate","project" to project.toString(),"requestId" to java.util.UUID.randomUUID().toString())
        workspace.save(submission,"qa-csrf");workspace.save(submission,"qa-csrf");check(repo.revisions(project).size==3)
        val server=PortalServer(db,context)
        check(server.start("127.0.0.1"))
        try {
            fun request(method:String,path:String,body:String="",cookie:String=""):String {
                PinnedPortalClient.socket(server).use { socket ->
                    socket.soTimeout=10000
                    val bytes=body.toByteArray(Charsets.UTF_8)
                    val head="$method $path HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nCookie: $cookie\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().apply{write(head.toByteArray());write(bytes);flush()}
                    return socket.getInputStream().readBytes().toString(Charsets.UTF_8)
                }
            }
            fun encode(value:String)=java.net.URLEncoder.encode(value,"UTF-8")
            check(!request("GET","/workspace?section=company").contains("QA Studio"))
            check(request("GET","/carbon/vendor.js").contains("application/javascript"))
            check(request("GET","/carbon/unknown.txt").startsWith("HTTP/1.1 404"))
            val themeCss=request("GET","/theme.css")
            check(themeCss.contains("--button-primary:#0f62fe") && themeCss.contains("--background:#161616") && themeCss.contains("--warning-icon:#f1c21b"))
            check(request("GET","/theme.js").contains("archiman-theme"))
            val login=request("GET","/")
            check(login.contains("/theme.css") && login.contains("/theme.js"))
            val csrf=Regex("archiman_csrf=([^;]+)").find(login)!!.groupValues[1]
            val user=db.practiceDao().activeUsers().single()
            val signed=request("POST","/","csrf=${encode(csrf)}&code=${server.state.value.code}&user=${user.id}","archiman_csrf=$csrf")
            val cookie="archiman_portal="+Regex("archiman_portal=([^;]+)").find(signed)!!.groupValues[1]
            check(request("GET","/workspace?section=company",cookie=cookie).contains("QA Studio"))
            val overview=request("GET","/workspace?section=dashboard",cookie=cookie);check(overview.contains("data-carbon-chart")&&overview.contains("TDS withheld")&&overview.contains("Project directory"))
            check(request("GET","/workspace.js",cookie=cookie).contains("application/javascript"))
            check(request("POST","/workspace","action=seed&csrf=wrong",cookie).startsWith("HTTP/1.1 403"))
            val downloadPage=request("GET","/workspace/export?kind=letterhead&format=pdf",cookie=cookie)
            check(downloadPage.contains("Protect & download") && downloadPage.contains("Download standard file"))
            val exportCsrf=Regex("name='csrf' value='([^']+)'").find(downloadPage)!!.groupValues[1]
            val exportPassword="integration download password"
            val exportBody="kind=letterhead&format=pdf&csrf=${encode(exportCsrf)}&mode=protected&password=${encode(exportPassword)}&confirmation=${encode(exportPassword)}"
            val response=PinnedPortalClient.socket(server).use { socket ->
                socket.soTimeout=60000
                val bytes=exportBody.toByteArray()
                socket.getOutputStream().apply { write("POST /workspace/export HTTP/1.1\r\nHost: 127.0.0.1:${PortalHttp.PORT}\r\nCookie: $cookie\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: ${bytes.size}\r\n\r\n".toByteArray());write(bytes);flush() }
                socket.getInputStream().readBytes()
            }
            val responseText=response.toString(Charsets.ISO_8859_1)
            check(responseText.startsWith("HTTP/1.1 200") && responseText.contains("application/octet-stream"))
            check(responseText.contains(".archimansecure"))
            val encrypted=response.copyOfRange(responseText.indexOf("\r\n\r\n")+4,response.size)
            val unlocked=com.holagundi.archiman.domain.PasswordEnvelope.decrypt(encrypted,exportPassword.toCharArray(),com.holagundi.archiman.data.BackupArchive.safeByteLimit())
            java.io.DataInputStream(unlocked.inputStream()).use { input -> check(input.readUTF().endsWith(".pdf"));check(input.readBytes().take(4).toByteArray().toString(Charsets.US_ASCII)=="%PDF") }

        } finally { server.stop(false) }
        db.practiceDao().upsertCompany(company)
    }
}
