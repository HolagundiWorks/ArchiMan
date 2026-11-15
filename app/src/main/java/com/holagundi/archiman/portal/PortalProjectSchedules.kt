package com.holagundi.archiman.portal

import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.*
import com.holagundi.archiman.domain.*
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import org.json.JSONObject

class PortalProjectSchedules(private val db:ArchiManDatabase) {
    private val repo=ProjectScheduleRepository(db)
    private fun e(v:Any?)=PortalHttp.escape(v?.toString().orEmpty())
    private fun input(name:String,label:String,value:String="")="<label>${e(label)}<input name='$name' value='${e(value)}'></label>"
    private fun hidden(name:String,value:String)="<input type='hidden' name='$name' value='${e(value)}'>"
    private fun select(name:String,label:String,choices:List<Pair<String,String>>,value:String="")="<label>${e(label)}<select name='$name'>"+choices.joinToString(""){(id,text)->"<option value='${e(id)}' ${if(id==value) "selected" else ""}>${e(text)}</option>"}+"</select></label>"
    private fun form(action:String,csrf:String,body:String,button:String)="<form method='post' action='/workspace'>${hidden("csrf",csrf)}${hidden("action",action)}$body<button>$button</button></form>"
    suspend fun page(projectId:Long,csrf:String,contractorId:Long?=null):String {
        val project=requireNotNull(db.projectDao().getProject(projectId)); val revisions=repo.revisions(projectId); val draft=revisions.firstOrNull{it.status=="DRAFT"}; val contractors=db.catalogDao().observeContractors(projectId).first(); val items=db.catalogDao().observeActiveItems().first()
        return "<h1>${e(project.name)} / Contractor item schedule</h1><p>One active revision applies project-wide. Each contractor has assigned items and negotiated rates. Rates, measurements and contractor bills exclude all taxes. Active revisions are immutable; create the next revision to change assignments or rates.</p>"+form("scheduleCreate",csrf,hidden("project",projectId.toString()),"Create next project revision (copy active)")+revisions.joinToString(""){rev->
            "<article><h2>${rev.label} | ${rev.status}</h2>"+rev.lines.filter{contractorId==null||it.contractorId==contractorId}.joinToString(""){line->"<section><p><strong>${e(contractors.firstOrNull{it.id==line.contractorId}?.name)} | ${e(line.code)}</strong><br>${e(line.description)}<br>${e(line.unit)} | ${e(line.rate)} / unit | ${e(line.basis)}<br><small>Import LineKey: ${e(line.key)}</small></p>"+(if(rev.status=="DRAFT")form("scheduleRemove",csrf,hidden("project",projectId.toString())+hidden("revision",rev.key)+hidden("line",line.key),"Remove draft line") else "")+"</section>"}+(if(rev.status=="DRAFT")form("scheduleActivate",csrf,hidden("project",projectId.toString())+hidden("revision",rev.key),"Activate ${rev.label} for the whole project")+form("scheduleDiscard",csrf,hidden("project",projectId.toString())+hidden("revision",rev.key),"Discard draft revision") else "")+"</article>"
        }+(draft?.let{rev-> "<h2>Add item to ${rev.label}</h2>"+form("scheduleLine",csrf,hidden("project",projectId.toString())+hidden("revision",rev.key)+select("contractor","Contractor",contractors.map{it.id.toString() to it.name},contractorId?.toString().orEmpty())+select("item","General catalogue item",items.map{it.id.toString() to "${it.itemCode} | ${it.name}"})+"<label>Contract description (leave blank to use general item specification)<textarea name='description' rows='4'></textarea></label>"+select("unit","Agreed rate unit",listOf("m³","ft³","m²","ft²","m","ft","Nos").map{it to it})+input("rate","Negotiated unit rate — excluding taxes")+select("basis","Contract basis",listOf("LABOUR_ONLY" to "Labour only","WITH_MATERIAL" to "Labour with materials")),"Add contractor item") } ?: "<p>Create a draft revision to assign items.</p>")+"<p><a href='/workspace?section=measure&id=$projectId'>Record measurements</a> | <a href='/workspace?section=estimate&id=$projectId'>Prepare estimate from general catalogue</a> | <a href='/workspace?section=contacts&project=$projectId&type=contractor'>Contractor profiles</a></p>"
    }
    suspend fun action(f:Map<String,String>,csrf:String):String {
        val project=requireNotNull(f["project"]?.toLongOrNull()); val key=f["revision"].orEmpty()
        if(key.isNotBlank()) require(repo.revisions(project).any{it.key==key}) { "Wrong project revision." }
        when(f["action"]) {
            "scheduleCreate"->repo.create(project)
            "scheduleActivate"->repo.activate(key)
            "scheduleDiscard"->repo.discard(key)
            "scheduleRemove"->repo.remove(key,f["line"].orEmpty())
            "scheduleLine"->{ val item=requireNotNull(db.catalogDao().observeActiveItems().first().firstOrNull{it.id==f["item"]?.toLongOrNull()}); repo.line(key,requireNotNull(f["contractor"]?.toLongOrNull()),item.id,f["description"].orEmpty().ifBlank{item.specification.ifBlank{item.name}},f["unit"].orEmpty(),f["rate"].orEmpty(),f["basis"].orEmpty()) }
        }
        return page(project,csrf)
    }
    private val clientExtra=linkedMapOf("legalName" to "Legal / registered name", "pan" to "PAN (optional)", "gstin" to "GSTIN (if registered)", "tan" to "TAN (if deducting TDS)", "registration" to "Company / entity registration", "signatory" to "Authorised signatory and designation", "billingContact" to "Billing contact name / email / phone", "paymentTerms" to "Agreed payment terms / due days", "tdsAdvice" to "TDS deduction / certificate arrangements")
    private val contractorExtra=linkedMapOf("legalName" to "Legal / registered business name","contactPerson" to "Authorised contact / signatory","email" to "Email","pan" to "PAN (optional)","gstin" to "GSTIN (compliance record only)","registration" to "Firm / company registration","udyam" to "MSME / Udyam registration","beneficiary" to "Bank beneficiary name","account" to "Bank account number","ifsc" to "Bank IFSC","bank" to "Bank name / branch","paymentTerms" to "Payment cycle, retention and security terms","insurance" to "Insurance reference / expiry","labourLicence" to "Labour licence / compliance reference","notes" to "Notes and scope limitations")
    private fun carbonField(name: String, label: String, value: String = "", span2: Boolean = false): String {
        val id = "field-${name.replace('_', '-')}"
        val span = if (span2) " portal-form-span-2" else ""
        return """<div class="cds--form-item$span"><label class="cds--label" for="$id">${e(label)}</label><input class="cds--text-input" id="$id" name="${e(name)}" value="${e(value)}"></div>"""
    }
    private fun carbonSelect(name: String, label: String, choices: List<Pair<String, String>>, value: String = "", span2: Boolean = false): String {
        val id = "field-${name.replace('_', '-')}"
        val span = if (span2) " portal-form-span-2" else ""
        val options = choices.joinToString("") { (option, text) ->
            """<option value="${e(option)}"${if (option == value) " selected" else ""}>${e(text)}</option>"""
        }
        return """<div class="cds--form-item$span"><label class="cds--label" for="$id">${e(label)}</label><select class="cds--select-input" id="$id" name="${e(name)}">$options</select></div>"""
    }
    private fun carbonArea(name: String, label: String, value: String = ""): String {
        val id = "field-${name.replace('_', '-')}"
        return """<div class="cds--form-item portal-form-span-2"><label class="cds--label" for="$id">${e(label)}</label><textarea class="cds--text-input" id="$id" name="${e(name)}" rows="3">${e(value)}</textarea></div>"""
    }
    private fun carbonForm(action: String, csrf: String, body: String, button: String): String =
        """<form method="post" action="/workspace" class="portal-contact-form">${hidden("csrf", csrf)}${hidden("action", action)}${hidden("requestId", java.util.UUID.randomUUID().toString())}$body<div class="portal-actions">${CarbonPortal.button(button, "primary", "document")}</div></form>"""
    private fun detailsOf(row: Map<String, String>) =
        row["detailsJson"]?.takeIf(String::isNotBlank)?.let(::JSONObject) ?: JSONObject()
    suspend fun contacts(csrf: String, type: String, id: Long?, projectId: Long?): String {
        val contractor = type == "contractor"
        val table = if (contractor) "contractors" else "clients"
        val rows = mutableListOf<Map<String, String>>()
        db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY name").use { c ->
            while (c.moveToNext()) rows.add(c.columnNames.associateWith { col -> c.getString(c.getColumnIndexOrThrow(col)).orEmpty() })
        }
        val current = rows.firstOrNull { it["id"] == id?.toString() }
        require(id == null || current != null) { "Profile no longer exists." }
        val extra = current?.let(::detailsOf) ?: JSONObject()
        val projectChoices = db.projectDao().projects().map { it.id.toString() to it.name }
        val projectNames = projectChoices.toMap()
        val withGst = rows.count { detailsOf(it).optString("gstin").isNotBlank() }
        val withPan = rows.count { detailsOf(it).optString("pan").isNotBlank() }
        val withTan = rows.count { detailsOf(it).optString("tan").isNotBlank() }
        val withBank = rows.count {
            val d = detailsOf(it)
            d.optString("account").isNotBlank() || d.optString("ifsc").isNotBlank()
        }
        val title = if (contractor) "Contractor profiles" else "Client profiles"
        val lead = if (contractor) {
            "Legal identity, bank details and compliance references for site contractors. Rates and bills remain tax-exclusive."
        } else {
            "Legal identity, billing contacts and GST/TDS references used on fee invoices and portal exports."
        }
        val switcher = """
            <div class="portal-segment" role="tablist" aria-label="People directory">
              <a class="portal-segment__item${if (!contractor) " is-active" else ""}" href="/workspace?section=contacts&type=client" role="tab" aria-selected="${!contractor}">${CarbonPortalIcons.svg("users")}<span>Clients</span></a>
              <a class="portal-segment__item${if (contractor) " is-active" else ""}" href="/workspace?section=contacts&type=contractor" role="tab" aria-selected="$contractor">${CarbonPortalIcons.svg("construction")}<span>Contractors</span></a>
            </div>
        """.trimIndent()
        val kpis = if (contractor) {
            PortalKpis.strip(
                listOf(
                    PortalKpis.Kpi("Contractors", rows.size.toString(), "construction"),
                    PortalKpis.Kpi("With GSTIN", withGst.toString(), "receipt"),
                    PortalKpis.Kpi("With PAN", withPan.toString(), "document"),
                    PortalKpis.Kpi("Bank details on file", withBank.toString(), "wallet"),
                ),
            )
        } else {
            PortalKpis.strip(
                listOf(
                    PortalKpis.Kpi("Clients", rows.size.toString(), "users"),
                    PortalKpis.Kpi("With GSTIN", withGst.toString(), "receipt"),
                    PortalKpis.Kpi("With PAN", withPan.toString(), "document"),
                    PortalKpis.Kpi("With TAN (TDS)", withTan.toString(), "wallet"),
                ),
            )
        }
        val tableRows = rows.map { row ->
            val d = detailsOf(row)
            val href = "/workspace?section=contacts&type=$type&id=${row["id"]}&project=${projectId ?: row["projectId"].orEmpty()}"
            val selected = row["id"] == id?.toString()
            val nameCell = if (selected) {
                """<strong>${e(row["name"])}</strong>"""
            } else {
                """<a href="$href">${e(row["name"])}</a>"""
            }
            if (contractor) {
                listOf(
                    nameCell,
                    e(projectNames[row["projectId"]].orEmpty().ifBlank { "—" }),
                    e(row["contractorType"].orEmpty().ifBlank { "—" }),
                    e(row["contactNo"].orEmpty().ifBlank { "—" }),
                    e(d.optString("gstin").ifBlank { "—" }),
                    """<a href="$href">Open</a>""",
                )
            } else {
                listOf(
                    nameCell,
                    e(row["clientType"].orEmpty().ifBlank { "—" }),
                    e(row["contactPerson"].orEmpty().ifBlank { "—" }),
                    e(row["email"].orEmpty().ifBlank { row["contactNo"].orEmpty().ifBlank { "—" } }),
                    e(d.optString("gstin").ifBlank { "—" }),
                    e(d.optString("tan").ifBlank { "—" }),
                    """<a href="$href">Open</a>""",
                )
            }
        }
        val directory = if (contractor) {
            PortalKpis.dataTable(
                "Contractor directory",
                listOf("Name", "Project", "Trade", "Phone", "GSTIN", ""),
                tableRows,
                "No contractors yet. Create a profile below.",
            )
        } else {
            PortalKpis.dataTable(
                "Client directory",
                listOf("Name", "Type", "Primary contact", "Email / phone", "GSTIN", "TAN", ""),
                tableRows,
                "No clients yet. Create a profile below.",
            )
        }
        val editing = id != null
        val formTitle = when {
            contractor && editing -> "Edit contractor"
            contractor -> "New contractor"
            editing -> "Edit client"
            else -> "New client"
        }
        val identity = if (contractor) {
            carbonField("name", "Contractor display name", current?.get("name").orEmpty()) +
                carbonSelect("project", "Project", projectChoices, projectId?.toString() ?: current?.get("projectId").orEmpty()) +
                carbonField("contractorType", "Trade / contractor type", current?.get("contractorType").orEmpty()) +
                carbonField("extra_legalName", "Legal / registered business name", extra.optString("legalName")) +
                carbonField("extra_registration", "Firm / company registration", extra.optString("registration")) +
                carbonField("extra_udyam", "MSME / Udyam registration", extra.optString("udyam"))
        } else {
            carbonField("name", "Client display name", current?.get("name").orEmpty()) +
                carbonField("clientType", "Individual / company / trust / other", current?.get("clientType").orEmpty()) +
                carbonField("extra_legalName", "Legal / registered name", extra.optString("legalName")) +
                carbonField("extra_registration", "Company / entity registration", extra.optString("registration")) +
                carbonField("extra_signatory", "Authorised signatory and designation", extra.optString("signatory"), span2 = true)
        }
        val contact = if (contractor) {
            carbonField("extra_contactPerson", "Authorised contact / signatory", extra.optString("contactPerson")) +
                carbonField("contactNo", "Primary phone", current?.get("contactNo").orEmpty()) +
                carbonField("phone", "Alternate phone", current?.get("phone").orEmpty()) +
                carbonField("extra_email", "Email", extra.optString("email")) +
                carbonField("address", "Registered / correspondence address", current?.get("address").orEmpty(), span2 = true)
        } else {
            carbonField("contactPerson", "Primary contact", current?.get("contactPerson").orEmpty()) +
                carbonField("contactNo", "Phone", current?.get("contactNo").orEmpty()) +
                carbonField("email", "Email", current?.get("email").orEmpty()) +
                carbonField("preferredCommunication", "Preferred communication", current?.get("preferredCommunication").orEmpty()) +
                carbonField("address", "Legal / billing address", current?.get("address").orEmpty(), span2 = true) +
                carbonField("correspondenceAddress", "Correspondence address", current?.get("correspondenceAddress").orEmpty(), span2 = true) +
                carbonArea("notes", "Notes", current?.get("notes").orEmpty())
        }
        val tax = if (contractor) {
            carbonField("extra_pan", "PAN (optional)", extra.optString("pan")) +
                carbonField("extra_gstin", "GSTIN (compliance record only)", extra.optString("gstin")) +
                carbonField("extra_labourLicence", "Labour licence / compliance reference", extra.optString("labourLicence")) +
                carbonField("extra_insurance", "Insurance reference / expiry", extra.optString("insurance"))
        } else {
            carbonField("extra_pan", "PAN (optional)", extra.optString("pan")) +
                carbonField("extra_gstin", "GSTIN (if registered)", extra.optString("gstin")) +
                carbonField("extra_tan", "TAN (if deducting TDS)", extra.optString("tan")) +
                carbonField("extra_tdsAdvice", "TDS deduction / certificate arrangements", extra.optString("tdsAdvice"), span2 = true)
        }
        val money = if (contractor) {
            carbonField("extra_beneficiary", "Bank beneficiary name", extra.optString("beneficiary")) +
                carbonField("extra_account", "Bank account number", extra.optString("account")) +
                carbonField("extra_ifsc", "Bank IFSC", extra.optString("ifsc")) +
                carbonField("extra_bank", "Bank name / branch", extra.optString("bank")) +
                carbonField("extra_paymentTerms", "Payment cycle, retention and security terms", extra.optString("paymentTerms"), span2 = true) +
                carbonArea("extra_notes", "Notes and scope limitations", extra.optString("notes"))
        } else {
            carbonField("extra_billingContact", "Billing contact name / email / phone", extra.optString("billingContact"), span2 = true) +
                carbonField("extra_paymentTerms", "Agreed payment terms / due days", extra.optString("paymentTerms"), span2 = true)
        }
        val sections = CarbonPortal.formSection("Identity", identity) +
            CarbonPortal.formSection("Contact", contact) +
            CarbonPortal.formSection("Tax & compliance", tax, if (contractor) "Stored for reference only; contractor bills exclude taxes." else "Used on fee invoices where GST or TDS applies.") +
            CarbonPortal.formSection(if (contractor) "Bank & payment" else "Billing", money)
        val newLink = if (editing) {
            """<p class="portal-actions"><a class="cds--btn cds--btn--tertiary" href="/workspace?section=contacts&type=$type${if (projectId != null) "&project=$projectId" else ""}">${CarbonPortalIcons.svg("users")}<span>New profile</span></a></p>"""
        } else ""
        val formHtml = """<h2 class="portal-section-title">${CarbonPortalIcons.svg(if (contractor) "construction" else "users")}<span>$formTitle</span></h2>$newLink""" +
            carbonForm(
                "contact",
                csrf,
                hidden("type", type) + hidden("id", id?.toString().orEmpty()) + sections,
                if (editing) "Save profile" else "Create profile",
            )
        var assigned = ""
        if (contractor && id != null) {
            val project = projectId ?: current?.get("projectId")?.toLongOrNull()
            if (project != null && project > 0) {
                val active = repo.active(project)
                val lines = active?.lines.orEmpty().filter { it.contractorId == id }.map {
                    listOf(e(it.code), e(it.description), e(it.unit), e(it.rate), e(it.basis))
                }
                assigned = """<h2 class="portal-section-title">${CarbonPortalIcons.svg("task")}<span>Assigned items · ${e(active?.label ?: "No active revision")}</span></h2>""" +
                    PortalKpis.dataTable("Active revision assignments", listOf("Code", "Description", "Unit", "Rate", "Basis"), lines, "No items assigned in the active revision.") +
                    """<p class="portal-actions"><a class="cds--btn cds--btn--tertiary" href="/workspace?section=schedule&id=$project&contractor=$id">${CarbonPortalIcons.svg("task")}<span>Manage project revision</span></a></p>"""
            }
        }
        return CarbonPortal.masthead(title, lead, if (contractor) "construction" else "users") +
            switcher + kpis + directory + formHtml + assigned
    }
    suspend fun saveContact(f:Map<String,String>,csrf:String):String=db.withTransaction {
        fun v(k:String)=f[k].orEmpty().trim().also{require(it.length<=2000)}
        require(v("name").isNotBlank()) { "Display name is required." }; val contractor=f["type"]=="contractor"; val id=f["id"]?.toLongOrNull(); val extra=JSONObject(); (if(contractor)contractorExtra else clientExtra).keys.forEach{extra.put(it,v("extra_"+it))}
        val now=System.currentTimeMillis()
        val saved=if(contractor) {
            val project=requireNotNull(f["project"]?.toLongOrNull()); requireNotNull(db.projectDao().getProject(project))
            if(id==null)db.catalogDao().insertContractor(ContractorEntity(projectId=project,name=v("name"),address=v("address"),contactNo=v("contactNo"),phone=v("phone"),contractorType=v("contractorType"),createdAt=now,detailsJson=extra.toString()))
            else { db.openHelper.writableDatabase.query("SELECT projectId FROM contractors WHERE id=?",arrayOf(id)).use { cursor -> require(cursor.moveToFirst());val original=cursor.getLong(0);require(original==0L||original==project) { "A contractor with project history cannot be moved to another project. Create a new contractor instead." } }; db.openHelper.writableDatabase.execSQL("UPDATE contractors SET name=?, address=?, contactNo=?, phone=?, contractorType=?, detailsJson=? WHERE id=?",arrayOf(v("name"),v("address"),v("contactNo"),v("phone"),v("contractorType"),extra.toString(),id)); id }
        } else if(id==null) db.practiceDao().insertClient(ClientEntity(name=v("name"),clientType=v("clientType"),contactPerson=v("contactPerson"),address=v("address"),correspondenceAddress=v("correspondenceAddress"),contactNo=v("contactNo"),email=v("email"),preferredCommunication=v("preferredCommunication"),notes=v("notes"),createdAt=now,detailsJson=extra.toString()))
        else { val cols=listOf("name","clientType","contactPerson","address","correspondenceAddress","contactNo","email","preferredCommunication","notes"); db.openHelper.writableDatabase.execSQL("UPDATE clients SET ${cols.joinToString(","){"$it=?"}}, detailsJson=? WHERE id=?",(cols.map{v(it)}+extra.toString()+id).toTypedArray()); id }
        contacts(csrf,if(contractor)"contractor" else "client",saved,f["project"]?.toLongOrNull())
    }
}
