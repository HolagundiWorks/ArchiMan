package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holagundi.archiman.ui.theme.CarbonIcons

/** Both layouts occupy exactly 280 dp; only the scrolling form above changes. */
@Composable
fun MeasurementKeyboard(field: String, numeric: Boolean, enabled: Boolean, onInsert: (String) -> Unit, onBackspace: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, fieldNavigation: Boolean = true) {
    var upper by rememberSaveable { mutableStateOf(false) }
    var symbols by rememberSaveable(field) { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().height(280.dp).semantics { contentDescription = "Measurement keyboard" }.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(field, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                IconButton(enabled = enabled && fieldNavigation, onClick = onPrevious) { Icon(CarbonIcons.ArrowBack, "Previous field") }
                IconButton(enabled = enabled && fieldNavigation, onClick = onNext) { Icon(CarbonIcons.ArrowForward, "Next field") }
            }
            val rows = when {
                numeric -> listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "Backspace"))
                symbols -> listOf("1234567890".map(Char::toString), listOf("@", "#", "₹", "%", "&", "*", "(", ")", "-", "+"), listOf("/", ":", ";", "!", "?", "'", ",", "."), listOf("ABC", "Space", "Backspace"))
                else -> listOf("qwertyuiop".map(Char::toString), "asdfghjkl".map(Char::toString), "zxcvbnm".map(Char::toString), listOf("Shift", "123", "Space", "Backspace"))
            }
            rows.forEach { keys ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    keys.forEach { key ->
                        val label = if (key.length == 1 && upper && !numeric && !symbols) key.uppercase() else key
                        Button(
                            onClick = { when (key) {
                                "Backspace" -> onBackspace()
                                "Shift" -> upper = !upper
                                "123" -> symbols = true
                                "ABC" -> symbols = false
                                "Space" -> onInsert(" ")
                                else -> onInsert(label)
                            } },
                            enabled = enabled,
                            modifier = Modifier.weight(if (key == "Space") 2.5f else 1f).fillMaxHeight().padding(vertical = 2.dp).semantics { contentDescription = if (key == "Shift") "Shift ${if (upper) "on" else "off"}" else if (key.length == 1) "Key $label" else label },
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, contentColor = MaterialTheme.colorScheme.onSurface),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        ) {
                            if (key == "Backspace") Icon(CarbonIcons.Backspace, null, Modifier.size(20.dp))
                            else Text(label, fontSize = if (numeric) 20.sp else 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
