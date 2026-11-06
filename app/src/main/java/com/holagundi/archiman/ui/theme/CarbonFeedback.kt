package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

enum class CarbonNoticeKind { INFO, SUCCESS, WARNING, ERROR }

@Composable fun CarbonInlineNotification(title:String,body:String,kind:CarbonNoticeKind=CarbonNoticeKind.INFO) {
    val support=carbonSupport
    val foreground=when(kind){CarbonNoticeKind.INFO->support.info;CarbonNoticeKind.SUCCESS->support.success;CarbonNoticeKind.WARNING->support.warning;CarbonNoticeKind.ERROR->MaterialTheme.colorScheme.error}
    val background=when(kind){CarbonNoticeKind.INFO->support.infoContainer;CarbonNoticeKind.SUCCESS->support.successContainer;CarbonNoticeKind.WARNING->support.warningContainer;CarbonNoticeKind.ERROR->MaterialTheme.colorScheme.errorContainer}
    val icon=when(kind){CarbonNoticeKind.INFO->CarbonIcons.Info;CarbonNoticeKind.SUCCESS->CarbonIcons.CheckCircle;CarbonNoticeKind.WARNING->CarbonIcons.Warning;CarbonNoticeKind.ERROR->CarbonIcons.ErrorOutline}
    Row(Modifier.fillMaxWidth().background(background).height(IntrinsicSize.Min).semantics(mergeDescendants=true){liveRegion=LiveRegionMode.Polite},verticalAlignment=Alignment.Top) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(foreground))
        Icon(icon,null,Modifier.padding(12.dp).size(20.dp),tint=foreground)
        Column(Modifier.weight(1f).padding(top=12.dp,end=16.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleSmall,color=foreground)
            Text(body,style=MaterialTheme.typography.bodyMedium,color=foreground)
        }
    }
}
