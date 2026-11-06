package com.holagundi.archiman.portal
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.FinanceRecordEntity
import com.holagundi.archiman.domain.*
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID
class PortalEstimates(private val db:ArchiManDatabase) {
    private fun e(s:Any?)=PortalHttp.escape(s?.toString().orEmpty())
    private suspend fun data(project:Long)=db.suiteDao().finance("estimate-$project")?.let{JSONObject(it.json)} ?: JSONObject().put("projectId",project).put("lines",JSONArray())
    suspend fun rows(project:Long):List<List<String>> {
        val lines=data(project).getJSONArray("lines")
        return listOf(listOf("Code","Description","Quantity","Unit","Rate excluding taxes","Amount excluding taxes"))+(0 until lines.length()).map{val l=lines.getJSONObject(it);listOf(l.getString("code"),l.getString("description"),l.getString("quantity"),l.getString("unit"),l.getString("rate"),(l.getString("quantity").toBigDecimal()*l.getString("rate").toBigDecimal()).setScale(2,java.math.RoundingMode.HALF_UP).toPlainString())}
    }
    suspend fun page(project:Long,csrf:String):String {
        val p=requireNotNull(db.projectDao().getProject(project)); val items=db.catalogDao().observeActiveItems().first(); val d=data(project);val lines=d.getJSONArray("lines")
        fun hidden(k:String,v:String)="<input type='hidden' name='$k' value='${e(v)}'>"
        return "<h1>${e(p.name)} / Estimate</h1><p>General CPWD-referenced / practice catalogue. Enter planned quantities and explicitly tax-exclusive estimate rates; these are separate from negotiated contractor rates. This draft estimate does not issue an invoice.</p><form method='post' action='/workspace'>${hidden("csrf",csrf)}${hidden("action","estimateAdd")}${hidden("project",project.toString())}<label>General item<select name='item'>"+items.joinToString(""){"<option value='${it.id}'>${e(it.itemCode)} | ${e(it.name)}</option>"}+"</select></label><label>Planned quantity<input name='quantity' type='number' min='0' step='any'></label><label>Rate excluding taxes<input name='rate' type='number' min='0' step='any'></label><button>Add estimate line</button></form>"+(0 until lines.length()).joinToString(""){val l=lines.getJSONObject(it);"<article>${e(l.getString("code"))} | ${e(l.getString("description"))}<p>${e(l.getString("quantity"))} ${e(l.getString("unit"))} × ${e(l.getString("rate"))}</p><form method='post' action='/workspace'>${hidden("csrf",csrf)}${hidden("action","estimateRemove")}${hidden("project",project.toString())}${hidden("line",l.getString("key"))}<button>Remove draft line</button></form></article>"}+"<p>Total excluding taxes: "+rows(project).drop(1).sumOf{it.last().toBigDecimal()}.toPlainString()+"</p>"+listOf("pdf","xlsx","csv").joinToString(" | "){"<a href='/workspace/export?kind=estimate&project=$project&format=$it'>Download $it</a>"}
    }
    suspend fun save(f:Map<String,String>,csrf:String):String=db.withTransaction {
        val project=requireNotNull(f["project"]?.toLongOrNull());requireNotNull(db.projectDao().getProject(project));val d=data(project);val lines=d.getJSONArray("lines")
        if(f["action"]=="estimateAdd") {
            val item=requireNotNull(db.catalogDao().observeActiveItems().first().firstOrNull{it.id==f["item"]?.toLongOrNull()});val qty=FinanceMath.decimal(f["quantity"].orEmpty()); val rate=FinanceMath.decimal(f["rate"].orEmpty());require(qty.signum()>0&&rate.signum()>0)
            lines.put(JSONObject().put("key",UUID.randomUUID().toString()).put("code",item.itemCode).put("description",item.specification.ifBlank{item.name}).put("unit",item.unit).put("quantity",qty.toPlainString()).put("rate",rate.toPlainString()))
        } else { val filtered=JSONArray();for(i in 0 until lines.length())if(lines.getJSONObject(i).getString("key")!=f["line"])filtered.put(lines.get(i));d.put("lines",filtered) }
        db.suiteDao().saveFinance(FinanceRecordEntity("estimate-$project","ESTIMATE",d.toString()));page(project,csrf)
    }
}
