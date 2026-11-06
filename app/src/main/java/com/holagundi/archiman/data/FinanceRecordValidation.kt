package com.holagundi.archiman.data
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.data.local.entity.FinanceRecordEntity
import org.json.JSONObject
object FinanceRecordValidation {
    fun validate(row:FinanceRecordEntity) {
        require(row.json.length<=4_000_000) { "Finance record exceeds the safe metadata size." }
        JsonSafety.check(row.json)
        val json=JSONObject(row.json)
        fun money(key:String)=FinanceMath.decimal(json.getString(key))
        when(row.kind) {
            "BANK"->{java.time.LocalDate.parse(json.getString("date"));val amount=json.getString("amount");require(amount.matches(Regex("-?[0-9]{1,12}(\\.[0-9]{1,6})?")));require(amount.toBigDecimal().signum()!=0);require(json.getString("status") in listOf("UNMATCHED","MATCHED","IGNORED"));listOf("description","reference","account").forEach{json.getString(it)}}
            "SETTINGS"->{GstMode.valueOf(json.getString("mode"));require(money("rate")<=java.math.BigDecimal("100"));json.getString("state");json.getString("gstin")}
            "BOQ_REVISION"->{val rev=ProjectScheduleRepositoryDecoder.decode(row);require(rev.projectId>0&&rev.number>0&&rev.status in listOf("DRAFT","ACTIVE","SUPERSEDED"));require(rev.lines.map{it.key}.distinct().size==rev.lines.size);rev.lines.forEach{WorkItemLibrary.validate(it.formula,it.unit);require(it.contractorId>0&&it.itemId>0&&it.key.isNotBlank()&&it.description.isNotBlank());require(FinanceMath.decimal(it.rate).signum()>0);require(it.basis in listOf("LABOUR_ONLY","WITH_MATERIAL"))}}
            "LETTERHEAD"->{require(json.optString("accent","#0F62FE").matches(Regex("#[a-fA-F0-9]{6}")));require(json.optString("layout","LOGO_LEFT") in listOf("LOGO_LEFT","LOGO_RIGHT"));require(json.optString("logo").length<=710000)}
            "ESTIMATE","CONTRACTOR_BILL"->{require(json.getLong("projectId")>0);val lines=json.getJSONArray("lines");var total=java.math.BigDecimal("0.00");for(i in 0 until lines.length()){val line=lines.getJSONObject(i);val qty=FinanceMath.decimal(line.getString("quantity"));val rate=FinanceMath.decimal(line.getString("rate"));require(qty.signum()>0&&rate.signum()>0);val value=(qty*rate).setScale(2,java.math.RoundingMode.HALF_UP);total+=value;if(row.kind=="CONTRACTOR_BILL"){require(line.getLong("measurementId")>0);require(FinanceMath.decimal(line.getString("amount"))==value);line.getString("revision");line.getString("basis")}}
                if(row.kind=="CONTRACTOR_BILL"){require(json.getString("status") in listOf("DRAFT","ISSUED","VOID"));require(json.getLong("contractorId")>0);json.getString("contractorName");json.getString("number");require(money("amount")==total)}}
        }
    }
    /** Shared structural decoder without a database dependency. */
    private object ProjectScheduleRepositoryDecoder {
        fun decode(row:FinanceRecordEntity):ScheduleRevision {
            val json=JSONObject(row.json);val lines=json.getJSONArray("lines")
            return ScheduleRevision(row.key,json.getLong("projectId"),json.getInt("number"),json.getString("status"),(0 until lines.length()).map{val l=lines.getJSONObject(it);ScheduleLine(l.getString("key"),l.getLong("contractorId"),l.getLong("itemId"),l.getString("code"),l.getString("description"),l.getString("unit"),l.getString("formula"),l.getString("rate"),l.getString("basis"))})
        }
    }
}
