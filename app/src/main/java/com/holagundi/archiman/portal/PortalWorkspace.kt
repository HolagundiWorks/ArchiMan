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
    private fun table(headers: List<String>, rows: List<List<String>>) =
        PortalKpis.dataTable("", headers, rows.map { row -> row.map { e(it) } }, "No rows yet.")
    private fun hidden(name: String, value: String) = "<input type='hidden' name='${e(name)}' value='${e(value)}'>"
    suspend fun page(csrf: String, section: String, id: Long?, notice: String = "", query: Map<String,String> = emptyMap(), noticeKind: String = "success"): String {
        // Project-scoped pages get a plain message instead of an internal "null" error when the project is missing.
        if (section in listOf("measure", "schedule", "estimate", "bills") && (id == null || db.projectDao().getProject(id) == null)) {
            error("That project is not on this phone. Open it from Projects.")
        }
        val content = when(section) {
            "", "dashboard" -> PortalDashboard(db).page()
            "company" -> company(csrf)
            "items" -> items(csrf,id)
            "measure" -> measurement(csrf,id,query["contractor"]?.toLongOrNull())
            "documents" -> documents()
            "tasks" -> PortalKanban(db).page(csrf, id)
            "schedule" -> PortalProjectSchedules(db).page(requireNotNull(id),csrf,query["contractor"]?.toLongOrNull())
            "contacts" -> PortalProjectSchedules(db).contacts(csrf,query["type"] ?: "client",id,query["project"]?.toLongOrNull())
            "bills" -> PortalContractorBills(db).page(requireNotNull(id),csrf)
            "estimate" -> PortalEstimates(db).page(requireNotNull(id),csrf)
            else -> "<h1>Practice workspace</h1><p>Manage your letterhead, item formulas, measurement imports and document downloads.</p>"
        }
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
        fun label(key: String) = key.replace(Regex("([A-Z])"), " $1").replaceFirstChar(Char::uppercase)
        val identity = listOf("practiceName" to c.practiceName, "legalName" to c.legalName, "companyType" to c.companyType)
            .joinToString("") { field(it.first, label(it.first), it.second) }
        val address = listOf("address" to c.address, "city" to c.city, "state" to c.state, "country" to c.country, "pinCode" to c.pinCode)
            .joinToString("") { field(it.first, label(it.first), it.second) }
        val contact = listOf("phone" to c.phone, "email" to c.email, "website" to c.website)
            .joinToString("") { field(it.first, label(it.first), it.second) }
        val registration = listOf("pan" to c.pan, "gstin" to c.gstin, "coaRegistrationNumber" to c.coaRegistrationNumber, "principalName" to c.principalName, "principalQualification" to c.principalQualification)
            .joinToString("") { field(it.first, label(it.first), it.second) } + field("practiceRegistrationDetails", label("practiceRegistrationDetails"), c.practiceRegistrationDetails)
        val logo = s.optString("logo")
        val design = select("layout","Letterhead layout",listOf("LOGO_LEFT" to "Logo on left", "LOGO_RIGHT" to "Logo on right"),s.optString("layout")) + field("accent","Accent colour",s.optString("accent","#0F62FE"),"color") + field("header","Tagline / additional header",s.optString("header")) + area("footer","Footer (bank/payment or registration details)",s.optString("footer")) + "<div class='cds--form-item portal-form-span-2'><label class='cds--label'>Company logo (PNG/JPEG, maximum 512 KB)</label><input class='portal-file' type='file' id='logo-file' accept='image/png,image/jpeg'></div>" + hidden("logo",logo) + "<div class='cds--form-item portal-form-span-2'><label><input type='checkbox' name='removeLogo' value='yes'> Remove saved logo</label></div>"
        val sections = CarbonPortal.formSection("Practice identity", identity, "Shown on letterhead and portal exports.") +
            CarbonPortal.formSection("Address", address) +
            CarbonPortal.formSection("Contact", contact) +
            CarbonPortal.formSection("Registration", registration, "Tax registration mode remains on the phone.") +
            CarbonPortal.formSection("Letterhead design", design)
        return CarbonPortal.masthead("Company & letterhead", "Company identity prints on portal PDFs and document exports.", "enterprise") +
            form("company", csrf, sections, "Save company & design") +
            "<h2 class='portal-section-title'>${CarbonPortalIcons.svg("document")}<span>Saved design preview</span></h2><article class='paper cds--tile portal-panel' style='--accent:${e(s.optString("accent","#0F62FE"))}'>${if(logo.isNotBlank()) "<img class='logo' style='float:${if(s.optString("layout")=="LOGO_RIGHT") "right" else "left"};margin:0 18px 12px 0;max-width:160px' src='${e(logo)}' alt='Company logo'>" else ""}<h2>${e(c.legalName.ifBlank { c.practiceName })}</h2>${DocumentExports.identity(c).drop(1).joinToString("") { "<p>${e(it)}</p>" }}<hr><p>${e(s.optString("header"))}</p><h3>Document title</h3><p>Document content will appear here.</p><footer>${e(s.optString("footer"))}</footer></article><p><a class='cds--btn cds--btn--tertiary' href='/workspace/export?kind=letterhead&format=pdf'>${CarbonPortalIcons.svg("download")}<span>Download letterhead PDF preview</span></a></p>"
    }
    private suspend fun items(csrf: String, id: Long?): String {
        val rows=db.catalogDao().observeActiveItems().first(); val current=rows.firstOrNull { it.id==id }
        val trades = rows.map { it.workType.ifBlank { "Uncategorised" } }.distinct().size
        val predefined = rows.count { it.isPredefined }
        val custom = rows.size - predefined
        val identity = hidden("id",current?.id?.toString().orEmpty()) + field("code","Item code",current?.itemCode.orEmpty()) + field("name","Item name",current?.name.orEmpty()) + field("trade","Trade / category",current?.workType.orEmpty())
        val measure = select("formula","Formula",WorkItemLibrary.formulas.map { it.key to "${it.key}: ${it.value}" },current?.calculationType ?: CalculationTypes.VOLUME) + select("unit","Default unit",listOf("m³","ft³","m²","ft²","m","ft","Nos").map { it to it },current?.unit ?: "m³") + area("specification","Full description / specification / measurement rules",current?.specification.orEmpty())
        val source = field("sourceName","Source edition / correction slip",current?.sourceName.orEmpty()) + field("sourceItemCode","Source item number",current?.sourceItemCode.orEmpty())
        val content = CarbonPortal.formSection("Item identity", identity) + CarbonPortal.formSection("Measurement rules", measure, "Nos is a multiplier; use deductions for wall openings.") + CarbonPortal.formSection("Source reference", source)
        val catalogueRows = rows.map { item ->
            listOf(
                e(item.itemCode),
                """<a href="/workspace?section=items&id=${item.id}">${e(item.name)}</a>""",
                e(item.workType.ifBlank { "—" }),
                e(item.unit),
                e(WorkItemLibrary.formulas[item.calculationType] ?: item.calculationType),
                if (item.isPredefined) "Template" else "Custom",
                form("archiveItem",csrf,hidden("id",item.id.toString()),"Archive"),
            )
        }
        return CarbonPortal.masthead("Item library", "Editable geometric templates for estimation. Saved entries keep their snapshots.", "ruler") +
            PortalKpis.strip(listOf(
                PortalKpis.Kpi("Active items", rows.size.toString(), "folder"),
                PortalKpis.Kpi("Trades / categories", trades.toString(), "construction"),
                PortalKpis.Kpi("Starter templates", predefined.toString(), "document"),
                PortalKpis.Kpi("Custom items", custom.toString(), "ruler"),
            )) +
            form("item",csrf,content,if(current==null) "Create item" else "Save item") +
            "<section class='portal-form-section'><legend class='portal-form-legend'>Starter templates</legend><p class='portal-form-hint'>Optional libraries. Existing names are skipped.</p>" +
            form("seed",csrf,"<p class='portal-form-hint'>Concrete, masonry, plaster, flooring, waterproofing, length and count templates.</p>","Add standard templates") +
            form("seedCpwd",csrf,"<p class='portal-form-hint'>CPWD DSR 2023 starter references. Verify specifications and correction slips.</p><p><a href='https://cpwd.gov.in/WriteReadData/Publication/DSR_Vol_1_Civil.pdf'>Official volume I</a> · <a href='https://cpwd.gov.in/WriteReadData/Publication/DSR_Vol_2_Civil.pdf'>Volume II</a></p>","Add CPWD-referenced starters") +
            "</section><section class='portal-form-section'><legend class='portal-form-legend'>Import catalogue</legend>" +
            form("previewCatalogue",csrf,"<p class='portal-form-hint'>CSV columns: Code,Name,Description,Unit,Formula,Edition,SourceItemCode,Trade. Up to 2,000 rows.</p><div class='cds--form-item'><input class='portal-file' type='file' id='csv-file' accept='.csv,.tsv'></div>"+area("csv","Catalogue CSV"),"Preview general catalogue") +
            "</section>" +
            PortalKpis.dataTable("Catalogue", listOf("Code", "Name", "Trade", "Unit", "Formula", "Type", "Actions"), catalogueRows, "No catalogue items yet.")
    }
    private suspend fun measurement(csrf: String, id: Long?, selectedContractorForPage: Long? = null): String {
        val project=requireNotNull(id?.let { db.projectDao().getProject(it) }) { "Choose a project from Projects." }
        val floors=db.projectDao().observeFloors(project.id).first(); val contractors=db.catalogDao().observeContractors(project.id).first()
        val active=com.holagundi.archiman.data.ProjectScheduleRepository(db).active(project.id)
        val selectedContractor = selectedContractorForPage ?: contractors.firstOrNull()?.id
        val assigned=active?.lines.orEmpty().filter { it.contractorId==selectedContractor }
        val sheets=db.measurementDao().observeSheets(project.id).first().filter { it.archivedAt == null }
        val contractorSheets = sheets.filter { it.contractorId == selectedContractor }
        val recordedKeys = contractorSheets.map { it.boqLineKey }.filter { it.isNotBlank() }.toSet()
        val recordedAssigned = assigned.count { it.key in recordedKeys }
        val yetToRecord = (assigned.size - recordedAssigned).coerceAtLeast(0)
        val draftSheets = contractorSheets.count { it.status.equals("DRAFT", true) || it.status.equals("RETURNED", true) }
        val reviewSheets = contractorSheets.count { it.status.equals("SUBMITTED", true) || it.status.equals("CHECKED", true) }
        val approvedSheets = contractorSheets.count { it.status.equals("APPROVED", true) }
        val measurementCount = sql.query("SELECT COUNT(*) FROM measurements WHERE projectId = ?", arrayOf(project.id)).use {
            it.moveToFirst(); it.getInt(0)
        }
        val chooser="<section class='portal-form-section'><form method='get' action='/workspace'>${hidden("section","measure")}${hidden("id",project.id.toString())}<p class='portal-form-legend'>Contractor context</p><div class='portal-form-grid'>${select("contractor","Contractor",contractors.map { it.id.toString() to it.name },selectedContractor?.toString().orEmpty())}</div><button class='cds--btn cds--btn--tertiary'>Show contractor items</button></form></section>"
        val context=hidden("project",project.id.toString()) + select("floor","Floor",floors.map { it.id.toString() to it.name }) + hidden("contractor",selectedContractor?.toString().orEmpty())
        val systems=MeasurementUnitSystem.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }
        val itemSelect = "<div class='cds--form-item portal-form-span-2'><label class='cds--label' for='work-item'>Assigned item</label><select class='cds--select-input' name='item' id='work-item'>" + assigned.joinToString("") { "<option value='${e(it.key)}' data-formula='${e(it.formula)}' data-unit='${e(it.unit)}' data-rate='${e(it.rate)}'>${e(it.code)} · ${e(it.description)} · ${e(it.rate)} / ${e(it.unit)}</option>" } + "</select></div>"
        val entry = CarbonPortal.formSection("Placement", context + hidden("revision",active?.label.orEmpty()) + select("system","Dimension unit system",systems) + itemSelect + field("description","Description")) +
            CarbonPortal.formSection("Dimensions", "<div class='dimensions portal-form-span-2'>" + listOf("nos","length","breadth","height").joinToString("") { field(it,it.replaceFirstChar(Char::uppercase),if(it=="nos") "1" else "","number") } + "</div>" + field("deduction","Opening deduction","0","number") + "<div class='cds--form-item portal-form-span-2'><output id='quantity' aria-live='polite'>Enter dimensions for a live quantity.</output></div>", "Converted to the assigned rate unit.") +
            CarbonPortal.formSection("Notes", field("location","Location") + field("remarks","Remarks"))
        val imp = CarbonPortal.formSection("Import file", context + select("sourceSystem","CSV dimension units",systems) + "<p class='portal-form-hint portal-form-span-2'>Columns: ItemCode, Description, Nos, Length, Breadth, Height, Deduction, Location, Remarks. One floor and contractor per import.</p><div class='cds--form-item portal-form-span-2'><input class='portal-file' type='file' id='csv-file' accept='.csv,.tsv,text/csv,text/tab-separated-values'></div>" + area("csv","CSV content"))
        val assignedRows = assigned.map { line ->
            val hasSheet = line.key in recordedKeys
            listOf(
                e(line.code),
                e(line.description),
                e(line.unit),
                e(line.rate),
                if (hasSheet) """<span class="status status-approved">Recorded</span>""" else """<span class="status">Yet to record</span>""",
            )
        }
        val sheetRows = contractorSheets.take(100).map { sheet ->
            listOf(
                e(sheet.sheetCode),
                e(sheet.itemNameSnapshot),
                e(sheet.floorNameSnapshot),
                """<span class="status">${e(sheet.status)}</span>""",
                e(sheet.boqRevision.ifBlank { "—" }),
            )
        }
        return CarbonPortal.masthead("${project.name} · Record entry", "Active revision: ${active?.label ?: "none"}. Quantities exclude taxes.", "notebook") +
            PortalKpis.strip(listOf(
                PortalKpis.Kpi("Assigned items", assigned.size.toString(), "task"),
                PortalKpis.Kpi("Measurement recorded", recordedAssigned.toString(), "notebook"),
                PortalKpis.Kpi("Yet to record", yetToRecord.toString(), "ruler"),
                PortalKpis.Kpi("Draft / returned sheets", draftSheets.toString(), "document"),
                PortalKpis.Kpi("Awaiting review", reviewSheets.toString(), "chart"),
                PortalKpis.Kpi("Approved sheets", approvedSheets.toString(), "folder"),
                PortalKpis.Kpi("Line entries", measurementCount.toString(), "construction"),
            )) +
            "<p><a href='/workspace?section=schedule&id=${project.id}'>Manage project revision</a></p>$chooser" +
            PortalKpis.dataTable("Assigned items for selected contractor", listOf("Code", "Description", "Unit", "Rate", "Recording"), assignedRows, "No items assigned to this contractor in the active revision.") +
            form("measure",csrf,entry,"Save entry") +
            "<h2 class='portal-section-title'>${CarbonPortalIcons.svg("upload")}<span>Import measurements</span></h2>" + form("previewImport",csrf,imp,"Preview import (no records saved)") +
            PortalKpis.dataTable("Measurement sheets", listOf("Sheet", "Item", "Floor", "Status", "Revision"), sheetRows, "No measurement sheets for this contractor yet.") +
            "<h2 class='portal-section-title'>${CarbonPortalIcons.svg("download")}<span>Downloads</span></h2><div class='portal-stack'><div class='cds--tile portal-panel'>" + listOf("pdf","xlsx","csv").joinToString(" · ") { "<a href='/workspace/export?kind=measurements&project=${project.id}&format=$it'>Measurement $it</a>" } + "<p><a href='/workspace/export?kind=runningbill&project=${project.id}&format=xlsx'>Approved quantities for running-bill valuation (Excel)</a></p></div></div>"
    }
    private suspend fun documents(): String {
        val projects=db.projectDao().projects()
        val projectNames=projects.associate { it.id to it.name }
        val tables=listOf("fee_proposals" to "proposal","work_contracts" to "agreement","invoices" to "invoice","letters" to "letter")
        return "<h1>Documents & exports</h1><p>PDF uses the saved company letterhead. Excel (.xlsx) and CSV contain the complete document text or tabular quantities. Draft/issued status is retained; downloads do not issue documents.</p>" + projects.joinToString("") { "<article><h2>${e(it.name)}</h2><a href='/workspace?section=measure&id=${it.id}'>Record / import measurements</a> | <a href='/workspace?section=schedule&id=${it.id}'>Project item revisions</a> | <a href='/workspace?section=estimate&id=${it.id}'>Estimate</a> | <a href='/workspace?section=bills&id=${it.id}'>Contractor running bills</a> | " + listOf("pdf","xlsx","csv").joinToString(" | ") { f -> "<a href='/workspace/export?kind=measurements&project=${it.id}&format=$f'>Measurements $f</a>" } + "</article>" } + tables.joinToString("") { (table,kind) ->
            sql.query("SELECT * FROM `$table` ORDER BY id DESC LIMIT 500").use { cursor -> buildString { while(cursor.moveToNext()) { val id=cursor.getLong(cursor.getColumnIndexOrThrow("id")); val title=cursor.getString(cursor.getColumnIndexOrThrow(if(kind=="letter") "subject" else "title")); val number=cursor.getColumnIndex("number").let{ if(it<0) "" else cursor.getString(it) }; val status=cursor.getColumnIndex("status").let{ if(it<0) "" else cursor.getString(it) }; val pid=cursor.getColumnIndex("projectId").let{ if(it<0) -1L else cursor.getLong(it) }; val project=projectNames[pid].orEmpty(); val kindLabel=mapOf("proposal" to "Fee proposal","agreement" to "Agreement","invoice" to "Client invoice","letter" to "Letter")[kind].orEmpty(); append("<article><strong>${e(kindLabel)}</strong> ${e(number)} · ${e(title)}" + (if(project.isNotBlank()) " · ${e(project)}" else "") + (if(status.isNotBlank()) " · <span class='status'>${e(status)}</span>" else "") + " | " + listOf("pdf","xlsx","csv").joinToString(" | ") { "<a href='/workspace/export?kind=$kind&id=$id&format=$it'>${e(it)}</a>" } + "</article>") } } }
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
        if (value("action")=="contact") {
            val html = PortalProjectSchedules(db).saveContact(fields, csrf)
            return shell(html, if (value("type") == "contractor") "contractors" else "clients", csrf)
        }
        if (value("action") == "taskAdd" || value("action") == "taskMove") {
            return shell(PortalKanban(db).save(fields), "tasks", csrf)
        }
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
    suspend fun shell(body: String, current: String = "dashboard", csrf: String? = null) = CarbonPortal.page(
        title = "Workspace",
        body = body,
        audience = CarbonPortal.Audience.ADMIN,
        current = current,
        crumb = "Workspace",
        workspace = true,
        csrf = csrf,
        brand = PortalBrand.load(db),
    )
}
