package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonSpacing

@Composable
fun CompanyTaskEditor(state: CompanyTaskEditorState,busy: Boolean,
    onChange: (String,String,String,String,String)->Unit,onSave:()->Unit,onReview:(Boolean)->Unit,onClose:()->Unit) {
    val draft = state.draft
    Column(verticalArrangement=CarbonSpacing.stackArrangement) {
        Text(if (draft.base == null) "New company task" else "Edit company task",style=MaterialTheme.typography.titleMedium)
        Text("Save online before leaving. Unsaved task details are held only while this live screen is open.")
        draft.base?.let { Text("Based on revision ${it.version}.") }
        val editable = !busy && !state.sent
        OutlinedTextField(draft.title,{onChange(it,draft.description,draft.dueDate,draft.status,draft.priority)},Modifier.fillMaxWidth(),
            label={Text("Task title")},enabled=editable,singleLine=true)
        OutlinedTextField(draft.description,{onChange(draft.title,it,draft.dueDate,draft.status,draft.priority)},Modifier.fillMaxWidth(),
            label={Text("Task description")},enabled=editable,maxLines=4)
        OutlinedTextField(draft.dueDate,{onChange(draft.title,draft.description,it,draft.status,draft.priority)},Modifier.fillMaxWidth(),
            label={Text("Due date (YYYY-MM-DD)")},supportingText={Text("Leave empty for no due date.")},enabled=editable,singleLine=true)
        TaskChoice("Status",draft.status,listOf("open","in_progress","completed","cancelled"),editable) {
            onChange(draft.title,draft.description,draft.dueDate,it,draft.priority)
        }
        TaskChoice("Priority",draft.priority,listOf("low","normal","high","urgent"),editable) {
            onChange(draft.title,draft.description,draft.dueDate,draft.status,it)
        }
        state.conflict?.let { current ->
            Text("Current company version · Revision ${current.version}",style=MaterialTheme.typography.titleMedium)
            Text("${current.title}\n${current.description}\n${current.status.replace('_',' ')} · ${current.priority}\nDue: ${current.dueAt?.take(10) ?: "None"}")
            Text("Your submitted values remain above. Nothing from this conflicting edit was saved.")
            OutlinedButton(enabled=!busy,onClick={onReview(true)}) {Text("Review my task on current revision")}
            OutlinedButton(enabled=!busy,onClick={onReview(false)}) {Text("Use company task values")}
        } ?: run {
            if (state.sent) Text("An interrupted request may already have saved. Retry keeps the same task and operation.")
            Button(enabled=!busy && draft.valid,onClick=onSave) {Text(if (state.sent) "Retry same task save" else "Save company task")}
        }
        OutlinedButton(enabled=!busy,onClick=onClose) {Text("Close task editor")}
    }
}

@Composable
private fun TaskChoice(label:String,value:String,options:List<String>,enabled:Boolean,onSelect:(String)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Box {
        OutlinedButton(enabled=enabled,onClick={expanded=true}) {Text("$label: ${value.replace('_',' ')}")}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            options.forEach { option -> DropdownMenuItem(text={Text(option.replace('_',' '))},onClick={expanded=false;onSelect(option)}) }
        }
    }
}
