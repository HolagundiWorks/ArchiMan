package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.holagundi.archiman.data.FeeDashboardSummary
import com.holagundi.archiman.domain.*
import java.text.NumberFormat
import java.util.Locale

/** Carbon categorical palette. Every series also has a text label and exact value. */
val carbonChartColors:List<Color>
    @Composable get()=if(MaterialTheme.colorScheme.background.luminance()<.5f)
        listOf(Color(0xFFBE95FF),Color(0xFF33B1FF),Color(0xFF3DDBD9),Color(0xFFFF7EB6),Color(0xFFA6C8FF))
    else listOf(Color(0xFF6929C4),Color(0xFF1192E8),Color(0xFF005D5D),Color(0xFF9F1853),Color(0xFF002D9C))

@Composable fun CarbonDistributionTile(title:String,description:String,data:List<CarbonChartDatum>,money:Boolean=false,stacked:Boolean=false,highlight:CarbonChartDatum?=null,onOpen:(()->Unit)?=null) {
    val colors=carbonChartColors
    val invalid=data.any{it.value.signum()<0}
    val fractions=remember(data,stacked){if(invalid)List(data.size){0f}else if(stacked)CarbonChartData.fractions(data)else CarbonChartData.barFractions(data)}
    val formatter=remember(money){NumberFormat.getNumberInstance(Locale("en","IN")).apply{minimumFractionDigits=if(money)2 else 0;maximumFractionDigits=if(money)2 else 0}}
    val track=MaterialTheme.colorScheme.surfaceContainerHigh
    val noData=data.isEmpty()||data.all{it.value.signum()==0}
    ArchiCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics{heading()})
            Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(highlight!=null) { Text(highlight.label,style=MaterialTheme.typography.labelMedium);Text("INR "+formatter.format(highlight.value),style=MaterialTheme.typography.headlineMedium) }
            if(invalid)CarbonInlineNotification("Review balances","A distribution cannot represent negative values. Exact balances are listed below.",CarbonNoticeKind.WARNING)
            if(noData) {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(CarbonIcons.Dashboard,null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if(money)"No issued fee balances to chart yet." else "Create a project to see its status here.",style=MaterialTheme.typography.bodyMedium)
                }
            }
            if(stacked&&!noData&&!invalid)Canvas(Modifier.fillMaxWidth().height(16.dp).clearAndSetSemantics{}) {
                var left=0f
                fractions.forEachIndexed { index,fraction->if(fraction>0f){val width=size.width*fraction;drawRect(colors[index%colors.size],Offset(left,0f),Size(width,size.height));left+=width} }
            }
            data.forEachIndexed { index,row->
                Column(Modifier.fillMaxWidth().semantics(mergeDescendants=true){},verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(colors[index%colors.size]))
                        Text(row.label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                        Text((if(money)"INR " else "")+formatter.format(row.value),style=MaterialTheme.typography.labelLarge)
                    }
                    if(!stacked&&!noData&&!invalid)Canvas(Modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics{}) {
                        drawRect(track);drawRect(colors[index%colors.size],size=Size(size.width*fractions[index],size.height))
                    }
                }
            }
            if(onOpen!=null)CarbonTextButton(onClick=onOpen){Text(if(money)"View invoices" else "View projects");Spacer(Modifier.width(16.dp));Icon(CarbonIcons.ArrowForward,null,Modifier.size(16.dp))}
        }
    }
}

@Composable fun CarbonFeeTile(summary:FeeDashboardSummary?,onOpen:(()->Unit)?=null) {
    if(summary==null) {
        ArchiCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Fees overview",style=MaterialTheme.typography.titleMedium);LinearProgressIndicator(Modifier.fillMaxWidth());Text("Loading fee balances…",style=MaterialTheme.typography.bodySmall)}}
    } else CarbonDistributionTile("Fees overview","${summary.unpaidInvoices} unpaid invoices. TDS is separate from cash received.",listOf(CarbonChartDatum("Cash received",summary.received),CarbonChartDatum("TDS withheld",summary.tdsWithheld),CarbonChartDatum("Outstanding",summary.outstanding)),money=true,stacked=true,highlight=CarbonChartDatum("Outstanding fees",summary.outstanding),onOpen=onOpen)
}
