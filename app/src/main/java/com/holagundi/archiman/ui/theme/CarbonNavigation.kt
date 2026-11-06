package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable fun CarbonNavigationBar(content:@Composable RowScope.()->Unit) {
    Surface(color=MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup(),content=content)
        }
    }
}
@Composable fun CarbonNavigationRail(content:@Composable ColumnScope.()->Unit) {
    Surface(color=MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.width(88.dp).fillMaxHeight().statusBarsPadding().selectableGroup(),content=content)
    }
}
@Composable fun CarbonNavigationItem(label:String,icon:ImageVector,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,rail:Boolean=false) {
    val indicator=MaterialTheme.colorScheme.primary
    Box(modifier.heightIn(min=64.dp).background(if(selected)MaterialTheme.colorScheme.surface else Color.Transparent).selectable(selected=selected,role=Role.Tab,onClick=onClick)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Icon(icon,null,Modifier.size(20.dp),tint=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label,style=MaterialTheme.typography.labelMedium,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=MaterialTheme.colorScheme.onSurface,softWrap=true)
        }
        if(selected)Canvas(Modifier.matchParentSize()) {
            val thickness=3.dp.toPx()
            if(rail)drawRect(indicator,size=Size(thickness,size.height))
            else drawRect(indicator,topLeft=Offset(0f,size.height-thickness),size=Size(size.width,thickness))
        }
    }
}
