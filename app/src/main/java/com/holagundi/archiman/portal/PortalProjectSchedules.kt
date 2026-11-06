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
    suspend fun contacts(csrf:String,type:String,id:Long?,projectId:Long?):String {
        val contractor=type=="contractor"; val table=if(contractor)"contractors" else "clients"; val rows=mutableListOf<Map<String,String>>()
        db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY name").use{c->while(c.moveToNext())rows.add(c.columnNames.associateWith{col->c.getString(c.getColumnIndexOrThrow(col)).orEmpty()})}
        val current=rows.firstOrNull{it["id"]==id?.toString()}; require(id==null||current!=null) { "Profile no longer exists." }; val extra=current?.get("detailsJson")?.takeIf(String::isNotBlank)?.let(::JSONObject) ?: JSONObject()
        val core=if(contractor)linkedMapOf("name" to "Contractor display name","address" to "Registered / correspondence address","contactNo" to "Primary phone","phone" to "Alternate phone","contractorType" to "Trade / contractor type") else linkedMapOf("name" to "Client display name","clientType" to "Individual / company / trust / other","contactPerson" to "Primary contact","address" to "Legal / billing address","correspondenceAddress" to "Correspondence address","contactNo" to "Phone","email" to "Email","preferredCommunication" to "Preferred communication","notes" to "Notes")
        val projectChoices=db.projectDao().projects().map{it.id.toString() to it.name}
        var body="<h1>${if(contractor)"Contractor" else "Client"} profiles</h1><p>Record legal identity, authorised contacts, billing/payment information and compliance references. Optional identifiers may be left blank. Contractor GST details are stored for reference; measurement and contractor-bill values exclude taxes.</p><p><a href='/workspace?section=contacts&type=client'>Clients</a> | <a href='/workspace?section=contacts&type=contractor'>Contractors</a></p>"+rows.joinToString(""){"<p><a href='/workspace?section=contacts&type=$type&id=${it["id"]}&project=${projectId ?: it["projectId"].orEmpty()}'>${e(it["name"])}</a></p>"}
        body+=form("contact",csrf,hidden("type",type)+hidden("id",id?.toString().orEmpty())+(if(contractor)select("project","Project",projectChoices,projectId?.toString() ?: current?.get("projectId").orEmpty())else "")+"<div class='grid'>"+core.entries.joinToString(""){input(it.key,it.value,current?.get(it.key).orEmpty())}+(if(contractor)contractorExtra else clientExtra).entries.joinToString(""){input("extra_"+it.key,it.value,extra.optString(it.key))}+"</div>",if(id==null)"Create profile" else "Save profile")
        if(contractor && id!=null) {
            val project=projectId ?: current?.get("projectId")?.toLongOrNull()
            if(project!=null && project>0) { val active=repo.active(project); body+="<h2>Assigned items | ${active?.label ?: "No active project revision"}</h2>"+active?.lines.orEmpty().filter{it.contractorId==id}.joinToString(""){"<article>${e(it.code)} | ${e(it.description)}<br>${e(it.rate)} / ${e(it.unit)} | ${e(it.basis)}</article>"}+"<a href='/workspace?section=schedule&id=$project&contractor=$id'>Manage project revision and contractor items</a>" }
        }
        return body
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
