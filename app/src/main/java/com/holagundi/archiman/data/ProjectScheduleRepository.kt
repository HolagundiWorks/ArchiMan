package com.holagundi.archiman.data

import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.FinanceRecordEntity
import com.holagundi.archiman.domain.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ScheduleLine(val key:String,val contractorId:Long,val itemId:Long,val code:String,val description:String,val unit:String,val formula:String,val rate:String,val basis:String)
data class ScheduleRevision(val key:String,val projectId:Long,val number:Int,val status:String,val lines:List<ScheduleLine>) {
    val label get() = "R"+number.toString().padStart(3,'0')
}
@Singleton
class ProjectScheduleRepository @Inject constructor(private val db:ArchiManDatabase) {
    fun observe(projectId:Long) = db.suiteDao().observeFinanceKinds(listOf("BOQ_REVISION")).map { records -> records.filter { it.kind=="BOQ_REVISION" }.map { decode(it) }.filter { it.projectId==projectId }.sortedByDescending { it.number } }
    suspend fun active(projectId:Long) = revisions(projectId).singleOrNull { it.status=="ACTIVE" }
    suspend fun revisions(projectId:Long) = db.suiteDao().financeKinds(listOf("BOQ_REVISION")).map(::decode).filter { it.projectId==projectId }.sortedByDescending { it.number }
    fun decode(row:FinanceRecordEntity):ScheduleRevision {
        val json=JSONObject(row.json); val lines=json.getJSONArray("lines")
        return ScheduleRevision(row.key,json.getLong("projectId"),json.getInt("number"),json.getString("status"),(0 until lines.length()).map { i -> val l=lines.getJSONObject(i); ScheduleLine(l.getString("key"),l.getLong("contractorId"),l.getLong("itemId"),l.getString("code"),l.getString("description"),l.getString("unit"),l.getString("formula"),l.getString("rate"),l.getString("basis")) })
    }
    private suspend fun store(rev:ScheduleRevision) {
        val lines=JSONArray(); rev.lines.forEach { l -> lines.put(JSONObject().put("key",l.key).put("contractorId",l.contractorId).put("itemId",l.itemId).put("code",l.code).put("description",l.description).put("unit",l.unit).put("formula",l.formula).put("rate",l.rate).put("basis",l.basis)) }
        db.suiteDao().saveFinance(FinanceRecordEntity(rev.key,"BOQ_REVISION",JSONObject().put("projectId",rev.projectId).put("number",rev.number).put("status",rev.status).put("lines",lines).toString()))
    }
    suspend fun create(projectId:Long):ScheduleRevision = db.withTransaction {
        requireNotNull(db.projectDao().getProject(projectId)); val all=revisions(projectId)
        require(all.none { it.status=="DRAFT" }) { "Finish the existing draft revision first." }
        val draft=ScheduleRevision("boq-"+UUID.randomUUID(),projectId,(all.maxOfOrNull { it.number } ?: 0)+1,"DRAFT",all.firstOrNull { it.status=="ACTIVE" }?.lines?.map { it.copy(key=UUID.randomUUID().toString()) }.orEmpty())
        store(draft); draft
    }
    private suspend fun draft(key:String):ScheduleRevision = requireNotNull(db.suiteDao().finance(key)).let(::decode).also { require(it.status=="DRAFT") { "Active and superseded revisions are immutable. Create the next project revision." } }
    suspend fun line(key:String, contractorId:Long, itemId:Long, description:String, unit:String, rate:String, basis:String, lineKey:String?=null) = db.withTransaction {
        val rev=draft(key); val item=requireNotNull(db.catalogDao().observeActiveItems().first().firstOrNull { it.id==itemId }) { "Select a general catalogue item." }
        require(db.catalogDao().observeContractors(rev.projectId).first().any { it.id==contractorId }) { "Select a contractor for this project." }
        WorkItemLibrary.validate(item.calculationType,unit); require(basis in listOf("LABOUR_ONLY","WITH_MATERIAL")); val money=FinanceMath.decimal(rate); require(money.signum()>0) { "Enter the negotiated tax-exclusive unit rate." }; require(description.isNotBlank() && description.length<=8000)
        if(lineKey!=null) require(rev.lines.any { it.key==lineKey })
        val line=ScheduleLine(lineKey ?: UUID.randomUUID().toString(),contractorId,itemId,item.itemCode,description,unit,item.calculationType,money.toPlainString(),basis)
        store(rev.copy(lines=rev.lines.filter { it.key!=lineKey }+line))
    }
    suspend fun remove(key:String,lineKey:String) = db.withTransaction { val rev=draft(key); store(rev.copy(lines=rev.lines.filter { it.key!=lineKey })) }
    suspend fun activate(key:String) = db.withTransaction {
        val rev=draft(key); require(rev.lines.isNotEmpty()) { "Add contractor items before activating." }
        revisions(rev.projectId).filter { it.status=="ACTIVE" }.forEach { store(it.copy(status="SUPERSEDED")) }
        store(rev.copy(status="ACTIVE"))
    }
    suspend fun discard(key:String) = db.withTransaction { draft(key); db.openHelper.writableDatabase.execSQL("DELETE FROM finance_records WHERE `key`=?",arrayOf(key)) }
}
