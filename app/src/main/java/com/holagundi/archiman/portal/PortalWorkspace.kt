package com.holagundi.archiman.portal

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.*
import com.holagundi.archiman.data.MeasurementRepository
import com.holagundi.archiman.domain.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.security.MessageDigest

class PortalWorkspace(private val db: ArchiManDatabase) {
    private val sql get() = db.openHelper.writableDatabase
    private fun e(text: Any?) = PortalHttp.escape(text?.toString().orEmpty())
    private fun field(name: String, label: String, value: String = "", type: String = "text") = "<label>${e(label)}<input name='${e(name)}' value='${e(value)}' type='$type'></label>"
    private fun area(name: String, label: String, value: String = "") = "<label>${e(label)}<textarea name='${e(name)}' rows='4'>${e(value)}</textarea></label>"
    private fun select(name: String, label: String, options: List<Pair<String,String>>, selected: String = "") = "<label>${e(label)}<select name='$name'>" + options.joinToString("") { (value,text) -> "<option value='${e(value)}' ${if(value==selected) "selected" else ""}>${e(text)}</option>" } + "</select></label>"
    private fun form(action: String, csrf: String, content: String, button: String) = "<form method='post' action='/workspace'><input type='hidden' name='csrf' value='${e(csrf)}'><input type='hidden' name='action' value='$action'><input type='hidden' name='requestId' value='${java.util.UUID.randomUUID()}'>$content<button>$button</button></form>"
    private suspend fun style() = db.suiteDao().finance("letterhead")?.let { JSONObject(it.json) } ?: JSONObject().put("accent", "#0F62FE").put("layout", "LOGO_LEFT")
    private fun table(headers: List<String>, rows: List<List<String>>) = "<div class='tablewrap'><table><thead><tr>${headers.joinToString("") { "<th>${e(it)}</th>" }}</tr></thead><tbody>${rows.joinToString("") { row -> "<tr>${row.joinToString("") { "<td>${e(it)}</td>" }}</tr>" }}</tbody></table></div>"
    private fun hidden(name: String, value: String) = "<input type='hidden' name='${e(name)}' value='${e(value)}'>"
    suspend fun page(csrf: String, section: String, id: Long?, notice: String = "", query: Map<String,String> = emptyMap(), noticeKind: String = "success"): String {
        val content = when(section) { "", "dashboard" -> PortalDashboard(db).page(); "company" -> company(csrf); "items" -> items(csrf,id); "measure" -> measurement(csrf,id,query["contractor"]?.toLongOrNull()); "documents" -> documents(); "schedule" -> PortalProjectSchedules(db).page(requireNotNull(id),csrf,query["contractor"]?.toLongOrNull()); "contacts" -> PortalProjectSchedules(db).contacts(csrf,query["type"] ?: "client",id,query["project"]?.toLongOrNull()); "bills" -> PortalContractorBills(db).page(requireNotNull(id),csrf); "estimate" -> PortalEstimates(db).page(requireNotNull(id),csrf); else -> "<h1>Practice workspace</h1><p>Manage your letterhead, item formulas, measurement imports and document downloads.</p>" }
        val projectNav = if (id != null && section in listOf("schedule", "measure", "estimate", "bills")) {
            val project = requireNotNull(db.projectDao().getProject(id))
            val revision = com.holagundi.archiman.data.ProjectScheduleRepository(db).active(id)
            "<section class='cds--tile carbon-project-context'><h2>${e(project.name)}</h2><p class='portal-tile-meta'>Active item revision: ${e(revision?.label ?: "Not activated")}. Contractor quantities and bills exclude taxes.</p><nav class='portal-workflow' aria-label='Project workflow'>" +
                listOf("estimate" to "1. Estimate", "schedule" to "2. Assigned works & revision", "measure" to "3. Record / import", "bills" to "4. Contractor bills").joinToString("") { (target,label) -> "<a href='/workspace?section=$target&id=$id'>${e(label)}</a>" } + "</nav><p>Review and approve recorded sheets in the phone's Measurement Book before preparing a bill.</p></section>"
        } else ""
        val feedback = if (notice.isBlank()) "" else "<p role='status' data-kind='${e(noticeKind.takeIf { it in listOf("success","info","warning","error") } ?: "info")}'>${e(notice)}</p>"
        val current = when {
            section == "contacts" && query["type"] == "contractor" -> "contractors"
            section == "contacts" -> "clients"
            section.isBlank() -> "dashboard"
            else -> section
        }
        return shell(feedback + projectNav + content, current, csrf)
    }
    private suspend fun company(csrf: String): String {
        val c = requireNotNull(db.practiceDao().company()); val s=style()
        val fields = linkedMapOf("practiceName" to c.practiceName,"legalName" to c.legalName,"companyType" to c.companyType,"address" to c.address,"city" to c.city,"state" to c.state,"country" to c.country,"pinCode" to c.pinCode,"phone" to c.phone,"email" to c.email,"website" to c.website,"pan" to c.pan,"gstin" to c.gstin,"coaRegistrationNumber" to c.coaRegistrationNumber,"principalName" to c.principalName,"principalQualification" to c.principalQualification,"practiceRegistrationDetails" to c.practiceRegistrationDetails)
        val editor = fields.entries.joinToString("") { field(it.key,it.key.replace(Regex("([A-Z])")," $1"),it.value) }
        val logo = s.optString("logo")
        val design = select("layout","Letterhead layout",listOf("LOGO_LEFT" to "Logo on left", "LOGO_RIGHT" to "Logo on right"),s.optString("layout")) + field("accent","Accent colour",s.optString("accent","#0F62FE"),"color") + field("header","Tagline / additional header",s.optString("header")) + area("footer","Footer (bank/payment or registration details)",s.optString("footer")) + "<label>Company logo (PNG/JPEG, maximum 512 KB)<input type='file' id='logo-file' accept='image/png,image/jpeg'></label>" + hidden("logo",logo) + "<label><input type='checkbox' name='removeLogo' value='yes'> Remove saved logo</label>"
        return "<h1>Company & letterhead designer</h1><p>Company identity prints on portal PDFs and document exports. Tax registration mode and rate are managed under Taxes & banking on the phone; GSTIN changes are synchronized.</p>" + form("company",csrf,"<div class='grid'>$editor</div><h2>Letterhead design</h2>$design","Save company & design") + "<h2>Saved design preview</h2><article class='paper' style='--accent:${e(s.optString("accent","#0F62FE"))}'>${if(logo.isNotBlank()) "<img class='logo' style='float:${if(s.optString("layout")=="LOGO_RIGHT") "right" else "left"};margin:0 18px 12px 0' src='${e(logo)}' alt='Company logo'>" else ""}<h2>${e(c.legalName.ifBlank { c.practiceName })}</h2>${DocumentExports.identity(c).drop(1).joinToString("") { "<p>${e(it)}</p>" }}<hr><p>${e(s.optString("header"))}</p><h3>Document title</h3><p>Document content will appear here.</p><footer>${e(s.optString("footer"))}</footer></article><a href='/workspace/export?kind=letterhead&format=pdf'>Download letterhead PDF preview</a>"
    }
    private suspend fun items(csrf: String, id: Long?): String {
        val rows=db.catalogDao().observeActiveItems().first(); val current=rows.firstOrNull { it.id==id }
        val content=hidden("id",current?.id?.toString().orEmpty()) + field("code","Item code",current?.itemCode.orEmpty()) + field("name","Item name",current?.name.orEmpty()) + field("trade","Trade / category",current?.workType.orEmpty()) + select("formula","Formula",WorkItemLibrary.formulas.map { it.key to "${it.key}: ${it.value}" },current?.calculationType ?: CalculationTypes.VOLUME) + select("unit","Default unit",listOf("m³","ft³","m²","ft²","m","ft","Nos").map { it to it },current?.unit ?: "m³") + area("specification","Full description / specification / measurement rules",current?.specification.orEmpty()) + field("sourceName","Source edition / correction slip",current?.sourceName.orEmpty()) + field("sourceItemCode","Source item number",current?.sourceItemCode.orEmpty())
        return "<h1>General catalogue — estimation items</h1><p>Choose a dimensionally compatible unit and formula. Saved entries and issued sheets retain their snapshots. Nos is a multiplier; use deductions for wall openings. These are editable geometric templates, not a certified schedule of rates.</p>" + form("item",csrf,content,if(current==null) "Create item" else "Save item") + form("seed",csrf,"<p>Add concrete, masonry, plaster, flooring, waterproofing, length and count templates. Existing names are skipped.</p>","Add standard templates") + form("seedCpwd",csrf,"<p>CPWD DSR 2023 starter references with adapted short descriptions; verify full specifications and correction slips. Published DSR rates are not imported as tax-exclusive contract rates.</p><a href='https://cpwd.gov.in/WriteReadData/Publication/DSR_Vol_1_Civil.pdf'>Official volume I</a> | <a href='https://cpwd.gov.in/WriteReadData/Publication/DSR_Vol_2_Civil.pdf'>Volume II</a>","Add CPWD-referenced starters") + form("previewCatalogue",csrf,"<p>Import your full catalogue descriptions as CSV: Code,Name,Description,Unit,Formula,Edition,SourceItemCode,Trade. Up to 2,000 rows; existing codes skipped. Rates are entered separately in estimates and contractor schedules.</p><input type='file' id='csv-file' accept='.csv,.tsv'>"+area("csv","Catalogue CSV"),"Preview general catalogue") + rows.joinToString("") { "<article><strong>${e(it.itemCode)} | ${e(it.name)}</strong><p>${e(it.unit)} | ${e(WorkItemLibrary.formulas[it.calculationType])}</p><a href='/workspace?section=items&id=${it.id}'>Edit</a>" + form("archiveItem",csrf,hidden("id",it.id.toString()),"Archive item") + "</article>" }
    }
    private suspend fun measurement(csrf: String, id: Long?, selectedContractorForPage: Long? = null): String {
        val project=requireNotNull(id?.let { db.projectDao().getProject(it) }) { "Choose a project from Projects." }
        val floors=db.projectDao().observeFloors(project.id).first(); val contractors=db.catalogDao().observeContractors(project.id).first(); val items=db.catalogDao().observeActiveItems().first()
        val active=com.holagundi.archiman.data.ProjectScheduleRepository(db).active(project.id)
        val selectedContractor = selectedContractorForPage ?: contractors.firstOrNull()?.id
        val assigned=active?.lines.orEmpty().filter { it.contractorId==selectedContractor }
        val chooser="<form method='get' action='/workspace'>${hidden("section","measure")}${hidden("id",project.id.toString())}${select("contractor","Select contractor before choosing an assigned item",contractors.map { it.id.toString() to it.name },selectedContractor?.toString().orEmpty())}<button>Show contractor items</button></form>"
        val context=hidden("project",project.id.toString()) + select("floor","Floor",floors.map { it.id.toString() to it.name }) + hidden("contractor",selectedContractor?.toString().orEmpty())
        val systems=MeasurementUnitSystem.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }
        val entry=context + hidden("revision",active?.label.orEmpty()) + select("system","Input dimension unit system (converted to assigned rate unit)",systems) + "<label>Item<select name='item' id='work-item'>" + assigned.joinToString("") { "<option value='${e(it.key)}' data-formula='${e(it.formula)}' data-unit='${e(it.unit)}' data-rate='${e(it.rate)}'>${e(it.code)} | ${e(it.description)} | ${e(it.rate)} / ${e(it.unit)} | ${e(it.basis)}</option>" } + "</select></label>" + field("description","Description") + "<div class='dimensions'>" + listOf("nos","length","breadth","height").joinToString("") { field(it,it,if(it=="nos") "1" else "","number") } + "</div>" + field("deduction","Wall opening deduction (square input units)","0","number") + "<output id='quantity' aria-live='polite'>Enter dimensions for a live quantity.</output>" + field("location","Location") + field("remarks","Remarks")
        val imp=context + select("sourceSystem","CSV dimension units",systems) + "<p>Saved units follow the assigned contractor rate unit; dimensions are converted automatically.</p>" + "<p>Columns: ItemCode, Description, Nos, Length, Breadth, Height, Deduction, Location, Remarks (optional LineKey for multiple assignments of the same item). Decimal feet for imperial inputs; deduction is square feet/metres. One selected floor and contractor per import.</p><input type='file' id='csv-file' accept='.csv,.tsv,text/csv,text/tab-separated-values'>" + area("csv","CSV content")
        return "<h1>${e(project.name)} / Record entry</h1><p>Active project revision: ${e(active?.label ?: "none")}. Only assigned contractor items are available. Quantities and bills exclude taxes.</p><p><a href='/workspace?section=schedule&id=${project.id}'>Manage project revision</a></p>$chooser" + form("measure",csrf,entry,"Save entry") + "<h2>Import measurements</h2>" + form("previewImport",csrf,imp,"Preview import (no records saved)") + "<h2>Downloads</h2>" + listOf("pdf","xlsx","csv").joinToString(" | ") { "<a href='/workspace/export?kind=measurements&project=${project.id}&format=$it'>Measurement $it</a>" } + "<p><a href='/workspace/export?kind=runningbill&project=${project.id}&format=xlsx'>Approved quantities for running-bill valuation (Excel)</a></p>"
    }
    private suspend fun documents(): String {
        val projects=db.projectDao().projects()
        val tables=listOf("fee_proposals" to "proposal","work_contracts" to "agreement","invoices" to "invoice","letters" to "letter")
        return "<h1>Documents & exports</h1><p>PDF uses the saved company letterhead. Excel (.xlsx) and CSV contain the complete document text or tabular quantities. Draft/issued status is retained; downloads do not issue documents.</p>" + projects.joinToString("") { "<article><h2>${e(it.name)}</h2><a href='/workspace?section=measure&id=${it.id}'>Record / import measurements</a> | <a href='/workspace?section=schedule&id=${it.id}'>Project item revisions</a> | <a href='/workspace?section=estimate&id=${it.id}'>Estimate</a> | <a href='/workspace?section=bills&id=${it.id}'>Contractor running bills</a> | " + listOf("pdf","xlsx","csv").joinToString(" | ") { f -> "<a href='/workspace/export?kind=measurements&project=${it.id}&format=$f'>Measurements $f</a>" } + "</article>" } + tables.joinToString("") { (table,kind) ->
            sql.query("SELECT * FROM `$table` ORDER BY id DESC LIMIT 500").use { cursor -> buildString { while(cursor.moveToNext()) { val id=cursor.getLong(cursor.getColumnIndexOrThrow("id")); val title=cursor.getString(cursor.getColumnIndexOrThrow(if(kind=="letter") "subject" else "title")); append("<article>${e(kind)}: ${e(title)} | " + listOf("pdf","xlsx","csv").joinToString(" | ") { "<a href='/workspace/export?kind=$kind&id=$id&format=$it'>${e(it)}</a>" } + "</article>") } } }
        }
    }
    suspend fun save(fields: Map<String,String>, csrf: String): String {
        val requestId=fields["requestId"]
        if(requestId.isNullOrBlank() || fields["action"].orEmpty().startsWith("preview")) return saveAction(fields,csrf)
        require(requestId.matches(Regex("[A-Fa-f0-9-]{36}")))
        return db.withTransaction {
            val key="portal-request-$requestId"
            if(db.suiteDao().finance(key)!=null) return@withTransaction page(csrf,"",null,"This submission was already saved. Open the relevant page to continue.", noticeKind="info")
            val html=saveAction(fields,csrf)
            db.suiteDao().saveFinance(FinanceRecordEntity(key,"PORTAL_REQUEST",JSONObject().put("at",System.currentTimeMillis()).toString()))
            html
        }
    }
    private suspend fun saveAction(fields: Map<String,String>, csrf: String): String {
        fun value(key: String)=fields[key].orEmpty().trim()
        if (value("action").startsWith("schedule")) return shell(PortalProjectSchedules(db).action(fields,csrf), "schedule", csrf)
        if (value("action")=="contact") return shell(PortalProjectSchedules(db).saveContact(fields,csrf), "clients", csrf)
        if (value("action").startsWith("bill")) return shell(PortalContractorBills(db).save(fields,csrf), "bills", csrf)
        if (value("action").startsWith("estimate")) return shell(PortalEstimates(db).save(fields,csrf), "estimate", csrf)
        return when(value("action")) {
            "company" -> {
                db.withTransaction {
                    val columns=listOf("practiceName","legalName","companyType","address","city","state","country","pinCode","phone","email","website","pan","gstin","coaRegistrationNumber","principalName","principalQualification","practiceRegistrationDetails")
                    require(value("practiceName").isNotBlank()) { "Practice name is required." }; require(columns.all { value(it).length <= 180 }) { "Company fields are limited to 180 characters." }
                    val config=db.suiteDao().finance("settings")
                    config?.let { val gst=JSONObject(it.json); if(gst.optString("mode")!="NONE") require(value("gstin").matches(Regex("[0-9]{2}[A-Z0-9]{13}")) && value("gstin").take(2)==gst.optString("state")) { "GSTIN must match the registered supplier state." }; gst.put("gstin",value("gstin")); db.suiteDao().saveFinance(it.copy(json=gst.toString())) }
                    sql.execSQL("UPDATE company_profile SET ${columns.joinToString(",") { "$it = ?" }}, updatedAt = ? WHERE id = 1",(columns.map { value(it) } + System.currentTimeMillis()).toTypedArray())
                    val style=style(); val accent=value("accent"); require(accent.matches(Regex("#[0-9a-fA-F]{6}"))) { "Choose a colour." }; require(value("layout") in listOf("LOGO_LEFT","LOGO_RIGHT")); require(value("header").length<=180 && value("footer").length<=240)
                    val logo=if(value("removeLogo")=="yes") "" else value("logo")
                    if(logo.isNotBlank()) {
                        require(logo.startsWith("data:image/png;base64,") || logo.startsWith("data:image/jpeg;base64,")) { "Use PNG or JPEG." }
                        val bytes=Base64.decode(logo.substringAfter(','),Base64.DEFAULT); require(bytes.size<=524288) { "Logo maximum 512 KB." }
                        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                        require(bounds.outWidth in 1..4000 && bounds.outHeight in 1..4000 && bounds.outWidth.toLong()*bounds.outHeight<=4000000) { "Logo must be a readable image, maximum 4 megapixels." }
                    }
                    style.put("logo",logo).put("accent",accent).put("layout",value("layout")).put("header",value("header")).put("footer",value("footer"))
                    db.suiteDao().saveFinance(FinanceRecordEntity("letterhead","LETTERHEAD",style.toString()))
                }; page(csrf,"company",null,"Company and letterhead saved.")
            }
            "seedCpwd" -> { com.holagundi.archiman.data.CpwdCatalogue.seed(db); page(csrf,"items",null,"CPWD-referenced starters added.") }
            "previewCatalogue" -> { val csv=value("csv"); val parsed=com.holagundi.archiman.data.CpwdCatalogue.parse(csv); shell("<h1>Review ${parsed.size} catalogue items</h1>"+table(listOf("Code","Description","Unit","Formula","Edition"),parsed.take(100).map{listOf(it.itemCode,it.specification,it.unit,it.calculationType,it.sourceName)})+form("importCatalogue",csrf,hidden("csv",csv),"Confirm catalogue import")) }
            "importCatalogue" -> { val count=com.holagundi.archiman.data.CpwdCatalogue.import(db,value("csv")); page(csrf,"items",null,"Imported $count new general catalogue items.") }
            "seed" -> { db.withTransaction { WorkItemLibrary.defaults.forEachIndexed { i,(trade,name,type) -> if(db.catalogDao().findItemByName(name)==null) db.catalogDao().insertItem(ItemMasterEntity(itemCode="ARCHI-STD-${i+1}",workType=trade,name=name,unit=MeasurementUnitConverter.unitFor(type,MeasurementUnitSystem.METRIC),calculationType=type,specification="Confirm inclusions and deductions for this contract.",sourceName="ArchiMan geometric template",sourceItemCode="",isPredefined=true,isActive=true)) } }; page(csrf,"items",null,"Standard items added.") }
            "item" -> { db.withTransaction {
                WorkItemLibrary.validate(value("formula"),value("unit")); require(value("name").isNotBlank() && value("code").matches(Regex("[A-Za-z0-9._-]{1,40}"))) { "Enter a name and an item code (letters, numbers, dot, dash)." }
                require(listOf("name","trade","specification").all { value(it).length<=1000 }); val id=value("id").toLongOrNull()
                val duplicate=sql.query("SELECT COUNT(*) FROM item_master WHERE (lower(name)=lower(?) OR itemCode=?) AND id<>?",arrayOf(value("name"),value("code"),id ?: 0)).use { it.moveToFirst(); it.getLong(0)>0 }; require(!duplicate) { "An item with that name/code already exists." }
                if(id==null) db.catalogDao().insertItem(ItemMasterEntity(itemCode=value("code"),workType=value("trade"),name=value("name"),unit=value("unit"),calculationType=value("formula"),specification=value("specification"),sourceName=value("sourceName"),sourceItemCode=value("sourceItemCode"),isPredefined=false,isActive=true))
                else sql.execSQL("UPDATE item_master SET itemCode=?, name=?, workType=?, unit=?, calculationType=?, specification=?, sourceName=?, sourceItemCode=? WHERE id=?",arrayOf(value("code"),value("name"),value("trade"),value("unit"),value("formula"),value("specification"),value("sourceName"),value("sourceItemCode"),id))
            }; page(csrf,"items",null,"Item saved; historical measurement snapshots are unchanged.") }
            "archiveItem" -> { sql.execSQL("UPDATE item_master SET isActive=0 WHERE id=?",arrayOf(value("id").toLong())); db.invalidationTracker.refreshVersionsAsync(); page(csrf,"items",null,"Item archived.") }
            "measure" -> {
                val draft=draft(fields); val result=repository().record(draft); require(result is RecordResult.Saved) { (result as RecordResult.Rejected).reason }; page(csrf,"measure",draft.projectId,"Entry saved to draft measurement sheet.")
            }
            "previewImport", "import" -> import(fields,csrf)
            else -> error("Unknown workspace action.")
        }
    }
    private fun repository() = MeasurementRepository(db,db.measurementDao(),db.catalogDao(),db.projectDao())
    private suspend fun draft(fields: Map<String,String>): MeasurementDraft {
        val project=requireNotNull(fields["project"]?.toLongOrNull()?.let { db.projectDao().getProject(it) }); require(project.status!="ARCHIVED")
        val floor=requireNotNull(db.projectDao().observeFloors(project.id).first().firstOrNull { it.id==fields["floor"]?.toLongOrNull() }) { "Select a floor." }
        val contractor=requireNotNull(db.catalogDao().observeContractors(project.id).first().firstOrNull { it.id==fields["contractor"]?.toLongOrNull() }) { "Select a contractor." }
        val active=requireNotNull(com.holagundi.archiman.data.ProjectScheduleRepository(db).active(project.id)) { "Activate the project schedule first." }
        require(fields["revision"]==null || fields["revision"]==active.label) { "The schedule revision changed. Reopen the form." }
        val line=requireNotNull(active.lines.firstOrNull { it.key==fields["item"] && it.contractorId==contractor.id }) { "Select an assigned item for this contractor." }
        val system=MeasurementUnitSystem.valueOf(fields["system"] ?: "METRIC"); val target=MeasurementUnitConverter.infer(line.unit)
        fun number(key: String, default: String="0"):Double {
            val raw=fields[key].orEmpty().ifBlank { default }
            return requireNotNull(raw.toDoubleOrNull()) { "Invalid $key." }.also { require(it.isFinite() && it>=0 && it<=1e9) { "Invalid $key." } }
        }
        fun dimension(key:String,power:Int=1)=MeasurementUnitConverter.convert(number(key),power,system,target)
        return MeasurementDraft(project.id,project.projectCode,floor.id,floor.name,null,null,contractor.id,contractor.name,line.itemId,line.description,line.unit,line.formula,fields["description"].orEmpty(),fields["location"].orEmpty(),fields["remarks"].orEmpty(),dimension("length"),dimension("breadth"),dimension("height"),number("nos","1"),dimension("deduction",2),active.label,line.key,line.rate.toDouble(),line.basis)

    }
    private suspend fun import(fields: Map<String,String>, csrf: String): String {
        val csv=fields["csv"].orEmpty(); require(csv.length<=1000000) { "Import maximum 1 MB." }; val cells=BankCsv.cells(csv); require(cells.size in 2..1001) { "Import 1 to 1000 rows." }
        val header=cells.first().map { it.removePrefix("\uFEFF").lowercase().replace(" ","") }
        val allowedColumns=setOf("itemcode","description","nos","length","breadth","width","height","deduction","location","remarks","linekey")
        require(header.distinct().size==header.size && header.all { it in allowedColumns }) { "Unsupported or duplicate measurement columns. Project, floor and contractor must be selected in the form, not supplied by CSV." }
        require("itemcode" in header) { "CSV needs ItemCode header." }
        val source=MeasurementUnitSystem.valueOf(fields["sourceSystem"].orEmpty());  val active=requireNotNull(com.holagundi.archiman.data.ProjectScheduleRepository(db).active(requireNotNull(fields["project"]?.toLongOrNull()))) { "Activate the project schedule first." }
        val occurrences=mutableMapOf<String,Int>()
        val drafts=cells.drop(1).mapIndexed { index,row ->
            try {
                val map=fields.toMutableMap(); header.forEachIndexed { i,key -> map[if(key=="width") "breadth" else key]=row.getOrNull(i).orEmpty() }
                val candidates=active.lines.filter { it.code==map["itemcode"] && it.contractorId==fields["contractor"]?.toLongOrNull() && (map["linekey"].isNullOrBlank() || it.key==map["linekey"]) }
                val line=requireNotNull(candidates.singleOrNull()) { "Item must match exactly one active contractor assignment; use LineKey if ambiguous." }
                map["item"]=line.key; map["revision"]=active.label; map["system"]=source.name
                val draft=draft(map); require(QuantityCalculator.calculate(draft.calculationType,MeasurementInput(draft.nos,draft.length,draft.width,draft.height,draft.deduction))>0) { "Quantity must be positive." }
                val signature=draft.toString(); val count=occurrences.getOrDefault(signature,0); occurrences[signature]=count+1
                val key="measurement-import-"+Hex.encode(MessageDigest.getInstance("SHA-256").digest("$signature|$count".toByteArray()))
                key to draft
            } catch(error: Exception) { throw IllegalArgumentException("CSV row ${index+2}: ${error.message}") }
        }
        if(fields["action"]=="import") {
            var count=0
            db.withTransaction { drafts.forEach { (key,draft) -> if(db.suiteDao().finance(key)==null) { val result=repository().record(draft); require(result is RecordResult.Saved) { (result as RecordResult.Rejected).reason }; db.suiteDao().saveFinance(FinanceRecordEntity(key,"MEASUREMENT_IMPORT",JSONObject().put("projectId",draft.projectId).toString())); count++ } } }
            return page(csrf,"measure",fields["project"]?.toLong(),"Imported $count entries; duplicate rows skipped.")
        }
        val preview=table(listOf("Item","Description","Dimensions (saved units)","Quantity"),drafts.take(100).map { (_,d) -> listOf(d.itemName,d.description,"${d.nos} × ${d.length} × ${d.width} × ${d.height}","${QuantityCalculator.calculate(d.calculationType,MeasurementInput(d.nos,d.length,d.width,d.height,d.deduction))} ${d.unit}") })
        val inputs=fields.filterKeys { it !in listOf("action","csrf","requestId") }.entries.joinToString("") { hidden(it.key,it.value) }
        return shell("<h1>Review ${drafts.size} measurement rows</h1><p>No records saved. Input ${e(source)} → assigned rate units. Showing first 100 rows. Repeated identical imports are skipped; verify genuine repeated entries before importing.</p>$preview"+form("import",csrf,inputs,"Confirm import into draft sheets")+"<a href='/workspace?section=measure&id=${e(fields["project"])}'>Cancel</a>")
    }
    data class Download(val bytes: ByteArray, val mime: String, val filename: String)
    suspend fun export(query: Map<String,String>): Download {
        val c=requireNotNull(db.practiceDao().company()); val kind=query["kind"].orEmpty(); val format=query["format"] ?: "pdf"; val id=query["id"]?.toLongOrNull()
        var title="Letterhead preview"; var text="Document content goes here."; var rows: List<List<String>> = emptyList()
        if(kind=="contractorbill") {
            val result=PortalContractorBills(db).export(query["id"].orEmpty()); title=result.first; rows=result.second; text=rows.joinToString("\n\n"){it.joinToString(" | ")}
        } else if(kind=="estimate") {
            val project=requireNotNull(query["project"]?.toLongOrNull());val p=requireNotNull(db.projectDao().getProject(project));title="${p.name} | Draft estimate excluding taxes"
            rows=PortalEstimates(db).rows(project);val total=rows.drop(1).sumOf{it.last().toBigDecimal()};rows=rows+listOf(listOf("TOTAL",total.toPlainString()));text=rows.joinToString("\n\n"){it.joinToString(" | ")}
        } else if(kind in listOf("measurements","runningbill")) {
            val project=requireNotNull(query["project"]?.toLongOrNull()?.let { db.projectDao().getProject(it) }); val all=db.measurementDao().measurementsNow(project.id); val sheets=db.measurementDao().observeSheets(project.id).first().associateBy { it.id }
            val measurements=if(kind=="runningbill") all.filter { sheets[it.sheetId]?.status=="APPROVED" } else all
            title="${project.name} | ${if(kind=="runningbill") "Approved quantities for running bill" else "Measurement book"}"
            rows=listOf(listOf("Project",project.projectCode,project.name),listOf("Sheet","Status","Item code / name","Floor","Contractor","Description","Nos","Length","Breadth","Height","Deduction","Quantity","Unit","Formula","Location","Remarks","Project revision","Assigned line","Rate (excl. taxes)","Contract basis","Amount (excl. taxes)")) + measurements.map { m -> listOf(sheets[m.sheetId]?.sheetCode.orEmpty(),sheets[m.sheetId]?.status.orEmpty(),m.itemName,m.floor,m.contractorName,m.description,m.nos.toString(),m.length.toString(),m.width.toString(),m.height.toString(),m.deduction.toString(),m.quantity.toString(),m.unit,m.formulaCode,m.location,m.remarks,m.boqRevision,m.boqLineKey,m.rateSnapshot.toString(),m.contractBasis,InvoiceBalance.money(m.quantity * m.rateSnapshot).toPlainString()) }
            text=measurements.joinToString("\n\n") { m -> "${sheets[m.sheetId]?.sheetCode} | ${sheets[m.sheetId]?.status} | ${m.itemName} | ${m.floor} | ${m.contractorName}\n${m.description}\n${m.nos} × ${m.length} × ${m.width} × ${m.height}; deduction ${m.deduction} | ${m.quantity} ${m.unit} | ${m.formulaCode}\nRevision ${m.boqRevision} | ${m.contractBasis} | rate ${m.rateSnapshot} | value ${InvoiceBalance.money(m.quantity*m.rateSnapshot)} (excluding taxes)" }
            val totals=measurements.groupBy { listOf(it.itemName,it.unit,it.boqRevision,it.rateSnapshot.toString(),it.contractBasis) }.map { (key,list) -> listOf("TOTAL",key[0],list.sumOf { it.quantity }.toString(),key[1],key[2],key[3],key[4],InvoiceBalance.money(list.sumOf { it.quantity * it.rateSnapshot }).toPlainString()) }
            rows=rows+listOf(emptyList())+totals; text += "\n\n"+totals.joinToString("\n") { it.joinToString(" | ") }
            if(kind=="runningbill") { text += "\nCumulative approved quantities only. Values use stored tax-exclusive rates. Deduct previously certified quantities and review before certifying payment. This export is not a net payment certificate."; rows=rows+listOf(listOf("Cumulative approved quantities only; reconcile previous certificates and agreed rates before payment.")) }
        } else if(kind!="letterhead") {
            val table=mapOf("proposal" to "fee_proposals","agreement" to "work_contracts","invoice" to "invoices","letter" to "letters")[kind] ?: error("Unknown document type.")
            sql.query("SELECT * FROM `$table` WHERE id=?",arrayOf(requireNotNull(id))).use { cursor ->
                require(cursor.moveToFirst()) { "Document not found." }; fun get(name:String)=cursor.getColumnIndex(name).let { if(it<0 || cursor.isNull(it)) "" else cursor.getString(it) }
                title=get(if(kind=="letter") "subject" else "title")
                text=if(kind in listOf("proposal","agreement")) get("documentText").ifBlank { "Legacy record: $title\nAmount INR ${get("amount")}\nStatus ${get("status")}\nNo structured terms stored." } else if(kind=="letter") "Status ${get("status")}\n\n${get("body")}" else {
                    val tax=get("taxJson").takeIf(String::isNotBlank)?.let(::JSONObject)
                    "Invoice ${get("number")} | ${get("status")}\n$title\nAmount due INR ${get("amount")}\n" + if(tax==null) "Legacy invoice: no stored GST classification." else "GST mode ${tax.getString("mode")}\nSupplier GSTIN ${tax.optString("gstin")}\nClient GSTIN ${tax.optString("clientGstin")}\nSAC ${tax.optString("sac")} | Place ${tax.optString("place")}\nProfessional fee ${tax.optString("base")} | CGST ${tax.optString("cgst")} | SGST ${tax.optString("sgst")} | IGST ${tax.optString("igst")}\n${if(tax.getString("mode")=="COMPOSITION") "Composition taxable person, not eligible to collect tax on supplies." else ""}"
                }
            }; rows=listOf(listOf("Document",title))+text.lines().map { listOf(it) }
        }
        if(rows.isEmpty()) rows=listOf(listOf(title),listOf(text))
        val identity=DocumentExports.identity(c).map { listOf(it) }; rows=identity+listOf(emptyList())+rows
        return when(format) { "pdf" -> Download(DocumentExports.pdf(c,style(),title,text),"application/pdf","archiman-$kind.pdf"); "xlsx" -> Download(DocumentExports.xlsx(rows),"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","archiman-$kind.xlsx"); "csv" -> Download(DocumentExports.csv(rows),"text/csv; charset=utf-8","archiman-$kind.csv"); else -> error("Choose PDF, XLSX or CSV.") }
    }
    fun shell(body: String, current: String = "dashboard", csrf: String? = null) = CarbonPortal.page(
        title = "Workspace",
        body = body,
        audience = CarbonPortal.Audience.ADMIN,
        current = current,
        crumb = "Workspace",
        workspace = true,
        csrf = csrf,
    )
}
