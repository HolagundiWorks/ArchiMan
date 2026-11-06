@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Tap or long-press the adjacent info control; the explanation stays until dismissed. */
@Composable
fun CarbonScreenHelp(title: String, paragraphs: List<String>) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        state = state,
        tooltip = {
            RichTooltip(
                title = { Text(title) },
                action = { CarbonTextButton(onClick = { state.dismiss() }) { Text("Got it") } },
            ) {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paragraphs.forEach { Text(it) }
                }
            }
        },
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) { Icon(CarbonIcons.Info, "$title help") }
    }
}
