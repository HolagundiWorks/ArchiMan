package com.holagundi.archiman.portal

import androidx.room.withTransaction
import com.holagundi.archiman.data.FeeDashboardSummary
import com.holagundi.archiman.data.local.ArchiManDatabase
import org.json.JSONArray
import org.json.JSONObject

class PortalDashboard(private val db:ArchiManDatabase) {
    private fun e(value:Any?)=PortalHttp.escape(value?.toString().orEmpty())
    suspend fun page():String=db.withTransaction {
        val projects=db.projectDao().projects()
        fun count(sql:String)=db.openHelper.writableDatabase.query(sql).use{it.moveToFirst();it.getInt(0)}
        val tasks=count("SELECT COUNT(*) FROM project_tasks t JOIN projects p ON p.id=t.projectId WHERE p.status!='ARCHIVED' AND t.status NOT IN ('DONE','COMPLETED')")
        val reviews=count("SELECT COUNT(*) FROM measurement_sheets s JOIN projects p ON p.id=s.projectId WHERE p.status!='ARCHIVED' AND s.archivedAt IS NULL AND s.status IN ('SUBMITTED','CHECKED')")
        val issues=count("SELECT COUNT(*) FROM site_issues i JOIN projects p ON p.id=i.projectId WHERE p.status!='ARCHIVED' AND i.status!='CLOSED'")
        val fees=FeeDashboardSummary.calculate(db.suiteDao().allInvoices(),db.suiteDao().allReceipts())
        fun status(value:String)=when(value.uppercase()){"ACTIVE"->"Active";"ON_HOLD"->"On hold";"COMPLETED"->"Completed";"ARCHIVED"->"Archived";else->"Other"}
        val mix=projects.groupingBy{status(it.status)}.eachCount().toSortedMap().map{(label,value)->label to value.toString()}
        val balances=listOf("Cash received" to fees.received.toPlainString(),"TDS withheld" to fees.tdsWithheld.toPlainString(),"Outstanding" to fees.outstanding.toPlainString())
        fun chart(title:String,description:String,type:String,rows:List<Pair<String,String>>):String {
            val data=JSONArray(rows.map{(label,value)->JSONObject().put("group",label).put("value",value)})
            return "<section class='carbon-insight'><h2>${e(title)}</h2><p>${e(description)}</p><div class='carbon-chart' data-carbon-chart='$type' data-rows='${e(data)}' aria-label='${e(title)}'></div><ul class='carbon-data-list' aria-label='${e(title)} exact values'>"+rows.mapIndexed{i,(label,value)->"<li><span><span class='swatch series-$i' aria-hidden='true'></span>${e(label)}</span><strong>${e(value)}</strong></li>"}.joinToString("")+"</ul></section>"
        }
        CarbonPortal.masthead("Practice overview","Projects, site work and client fees in one place.","dashboard")+
            "<p><a class='cds--btn cds--btn--primary' href='/'>${CarbonPortalIcons.svg("building")}<span>Choose a project</span></a></p><div class='carbon-kpis'>"+
            listOf(Triple("Current projects",projects.count{it.status!="ARCHIVED"},"building"),Triple("Open tasks",tasks,"task"),Triple("Sheets to review",reviews,"notebook"),Triple("Open site issues",issues,"construction")).joinToString(""){(label,value,icon)->"<div class='carbon-kpi'>${CarbonPortalIcons.svg(icon,"carbon-kpi-icon")}<strong>$value</strong><span>${e(label)}</span></div>"}+"</div>"+
            "<div class='carbon-insights'>"+chart("Project mix","All projects, including archived projects.","donut",mix)+chart("Fee balances","INR · ${fees.unpaidInvoices} unpaid invoices. TDS is separate from cash received.","bar",balances)+"</div><h2 class='portal-section-title'>${CarbonPortalIcons.svg("folder")}<span>Project directory</span></h2><div class='tablewrap'><table><thead><tr><th>Project</th><th>Code</th><th>Status</th><th>Measurements</th></tr></thead><tbody>"+
            projects.joinToString(""){p->"<tr><td>${e(p.name)}</td><td>${e(p.projectCode)}</td><td>${e(status(p.status))}</td><td><a href='/workspace?section=measure&id=${p.id}'>${CarbonPortalIcons.svg("notebook","carbon-inline-icon")}<span>Open measurement book</span></a></td></tr>"}+"</tbody></table></div>"
    }
}
