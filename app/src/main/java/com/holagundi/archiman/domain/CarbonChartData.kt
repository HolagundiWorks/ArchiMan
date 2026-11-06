package com.holagundi.archiman.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class CarbonChartDatum(val label:String,val value:BigDecimal)

/** Ratios are presentation only; all labels keep the original exact decimal value. */
object CarbonChartData {
    fun fractions(data:List<CarbonChartDatum>):List<Float> {
        require(data.all { it.value.signum()>=0 }) { "Distribution charts require non-negative values." }
        val total=data.fold(BigDecimal.ZERO){sum,row->sum+row.value}
        if(total.signum()==0)return List(data.size){0f}
        return data.map { it.value.divide(total,12,RoundingMode.HALF_UP).toFloat() }
    }
    fun barFractions(data:List<CarbonChartDatum>):List<Float> {
        require(data.all { it.value.signum()>=0 })
        val max=data.maxOfOrNull{it.value}?:BigDecimal.ZERO
        if(max.signum()==0)return List(data.size){0f}
        return data.map{it.value.divide(max,12,RoundingMode.HALF_UP).toFloat()}
    }
}
