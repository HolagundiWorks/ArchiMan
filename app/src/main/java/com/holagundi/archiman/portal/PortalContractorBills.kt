package com.holagundi.archiman.portal
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.FinanceRecordEntity
import com.holagundi.archiman.domain.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
class PortalContractorBills(private val db:ArchiManDatabase) {
    private fun e(s:Any?)=PortalHttp.escape(s?.toString().orEmpty())
    private suspend fun bills(project:Long)=db.suiteDao().financeKinds(listOf("CONTRACTOR_BILL")).map{it to JSONObject(it.json)}.filter{it.second.getLong("projectId")==project}
    private suspend fun used(project:Long)=bills(project).filter{it.second.getString("status")=="ISSUED"}.flatMap{val a=it.second.getJSONArray("lines");(0 until a.length()).map{i->a.getJSONObject(i).getLong("measurementId")}}.toSet()
    private fun form(action:String,csrf:String,body:String,button:String)="<form method='post' action='/workspace'><input type='hidden' name='action' value='$action'><input type='hidden' name='csrf' value='${e(csrf)}'>$body<button>$button</button></form>"
    suspend fun page(project:Long,csrf:String):String {
        val p=requireNotNull(db.projectDao().getProject(project));val contractors=db.catalogDao().observeContractors(project).first()
        return "<h1>${e(p.name)} / Contractor running bills</h1><p>Draft bills select approved measurements not already included in an issued bill. Values use each entry's stored project revision and negotiated rate. No taxes are calculated or added. Review before issue; issue retains a locked snapshot.</p>"+form("billCreate",csrf,"<input type='hidden' name='project' value='$project'><label>Contractor<select name='contractor'>"+contractors.joinToString(""){"<option value='${it.id}'>${e(it.name)}</option>"}+"</select></label>","Prepare running-bill draft")+bills(project).joinToString(""){(record,bill)->
            val key=record.key
            "<article><h2>${e(bill.getString("number"))} | ${e(bill.getString("contractorName"))} | <span class='status status-${e(bill.getString("status").lowercase())}'>${e(bill.getString("status"))}</span></h2><p>Amount: ${e(bill.getString("amount"))} | excluding taxes</p>"+listOf("pdf","xlsx","csv").joinToString(" | "){"<a href='/workspace/export?kind=contractorbill&id=$key&format=$it'>Download $it</a>"}+(if(bill.getString("status")=="DRAFT")form("billIssue",csrf,"<input type='hidden' name='project' value='$project'><input type='hidden' name='key' value='${e(key)}'><label><input name='reviewed' type='checkbox' value='yes' required> Reviewed quantities, contractor, revision and unit rates</label>","Issue bill")+form("billDelete",csrf,"<input type='hidden' name='project' value='$project'><input type='hidden' name='key' value='${e(key)}'>","Delete draft")else if(bill.getString("status")=="ISSUED")form("billVoid",csrf,"<input type='hidden' name='project' value='$project'><input type='hidden' name='key' value='${e(key)}'><label>Reason for voiding<input name='reason' required></label>","Void issued bill")else "")+"</article>"
        }
    }
    suspend fun save(f:Map<String,String>,csrf:String):String=db.withTransaction {
        val project=requireNotNull(f["project"]?.toLongOrNull());requireNotNull(db.projectDao().getProject(project))
        if(f["action"]=="billCreate") {
            val contractor=requireNotNull(db.catalogDao().observeContractors(project).first().firstOrNull{it.id==f["contractor"]?.toLongOrNull()})
            val approved=db.measurementDao().observeSheets(project).first().filter{it.status=="APPROVED"}.map{it.id}.toSet();val used=used(project)
            val rows=db.measurementDao().measurementsNow(project).filter{it.contractorId==contractor.id && it.sheetId in approved && it.id !in used}
            require(rows.isNotEmpty()) { "No unbilled approved measurements for this contractor." };require(rows.all{it.boqRevision.isNotBlank()&&it.rateSnapshot>0}) { "Legacy measurements lack negotiated rate snapshots. Retain them and create reviewed schedule-backed measurements; do not infer historical rates." }
            val lines=JSONArray();var amount=java.math.BigDecimal("0.00")
            rows.forEach{m->val value=InvoiceBalance.money(m.quantity*m.rateSnapshot);amount+=value;lines.put(JSONObject().put("measurementId",m.id).put("item",m.itemName).put("description",m.description).put("revision",m.boqRevision).put("basis",m.contractBasis).put("quantity",m.quantity.toString()).put("unit",m.unit).put("rate",InvoiceBalance.money(m.rateSnapshot).toPlainString()).put("amount",value.toPlainString()))}
            val now=System.currentTimeMillis();val json=JSONObject().put("projectId",project).put("contractorId",contractor.id).put("contractorName",contractor.name).put("number","RA-$project-$now").put("status","DRAFT").put("createdAt",now).put("lines",lines).put("amount",amount.toPlainString())
            db.suiteDao().saveFinance(FinanceRecordEntity("bill-"+UUID.randomUUID(),"CONTRACTOR_BILL",json.toString()))
        } else {
            val (record,bill)=requireNotNull(bills(project).firstOrNull{it.first.key==f["key"]})
            when(f["action"]) {
                "billIssue"->{require(bill.getString("status")=="DRAFT"&&f["reviewed"]=="yes");val used=used(project);val lines=bill.getJSONArray("lines");val approved=db.measurementDao().observeSheets(project).first().filter{it.status=="APPROVED"}.map{it.id}.toSet();val current=db.measurementDao().measurementsNow(project).associateBy{it.id};for(i in 0 until lines.length()){val id=lines.getJSONObject(i).getLong("measurementId");require(id !in used&&current[id]?.sheetId in approved){"An entry is no longer approved or was already billed. Recreate this draft."}};bill.put("status","ISSUED").put("issuedAt",System.currentTimeMillis());db.suiteDao().saveFinance(record.copy(json=bill.toString()))}
                "billDelete"->{require(bill.getString("status")=="DRAFT");db.openHelper.writableDatabase.execSQL("DELETE FROM finance_records WHERE `key`=?",arrayOf(record.key))}
                "billVoid"->{require(bill.getString("status")=="ISSUED"&&!f["reason"].isNullOrBlank());bill.put("status","VOID").put("voidReason",f["reason"]);db.suiteDao().saveFinance(record.copy(json=bill.toString()))}
            }
        };page(project,csrf)
    }
    suspend fun export(key:String):Pair<String,List<List<String>>> {
        val record=requireNotNull(db.suiteDao().finance(key));require(record.kind=="CONTRACTOR_BILL");val bill=JSONObject(record.json);val lines=bill.getJSONArray("lines")
        val rows=listOf(listOf("Status",bill.getString("status")),listOf("Contractor",bill.getString("contractorName")),listOf("Item","Description","Project revision","Contract basis","Quantity","Unit","Rate excluding taxes","Amount excluding taxes"))+(0 until lines.length()).map{val l=lines.getJSONObject(it);listOf(l.getString("item"),l.getString("description"),l.getString("revision"),l.getString("basis"),l.getString("quantity"),l.getString("unit"),l.getString("rate"),l.getString("amount"))}+listOf(listOf("TOTAL EXCLUDING TAXES",bill.getString("amount")))
        return bill.getString("number") to rows
    }
}
