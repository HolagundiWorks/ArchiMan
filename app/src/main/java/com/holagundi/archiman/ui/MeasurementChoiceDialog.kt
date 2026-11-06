@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.holagundi.archiman.domain.MeasurementTextEditing
import com.holagundi.archiman.ui.theme.CarbonIcons

/** Searchable large pickers use the same on-screen keyboard, with no system IME session. */
@Composable
fun <T> MeasurementChoiceDialog(label: String, choices: List<Pair<T, String>>, onDismiss: () -> Unit, onChoose: (T) -> Unit) {
    var search by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val focus = remember { FocusRequester() }
    fun edit(input: String?, backspace: Boolean) {
        val updated = if (backspace) MeasurementTextEditing.backspace(search.text, search.selection.start, search.selection.end) else MeasurementTextEditing.insert(search.text, search.selection.start, search.selection.end, input.orEmpty(), false)
        search = TextFieldValue(updated.text, TextRange(updated.cursor)); focus.requestFocus()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MeasurementInputBoundary {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Select $label") }, navigationIcon = { IconButton(onClick = onDismiss) { Icon(CarbonIcons.ArrowBack, "Close $label picker") } }) },
                bottomBar = { Box(Modifier.navigationBarsPadding()) { MeasurementKeyboard("Search $label", false, true, { edit(it, false) }, { edit(null, true) }, {}, {}, fieldNavigation = false) } },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(search, { if (it.text.length <= 8000) search = it }, Modifier.fillMaxWidth().focusRequester(focus), label = { Text("Search $label") }, singleLine = true, keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false))
                    val visible = choices.filter { it.second.contains(search.text, true) }
                    if (visible.isEmpty()) Text("No matching options.")
                    LazyColumn(Modifier.weight(1f)) { items(visible, key = { it.first.toString() }) { (key, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onChoose(key) }) } }
                }
            }
        }
    }
}
