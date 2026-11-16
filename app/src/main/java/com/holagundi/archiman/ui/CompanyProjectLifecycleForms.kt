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
fun CompanyProjectCreator(state:CompanyProjectCreateState,busy:Boolean,onChange:(String,String,String)->Unit,onSave:()->Unit,onClose:()->Unit) {
    val draft=state.draft
    var statuses by remember {mutableStateOf(false)}
    Column(verticalArrangement=CarbonSpacing.stackArrangement) {
        Text("New company project",style=MaterialTheme.typography.titleMedium)
        Text("This creates a company record online. Local practice projects remain separate. Unsaved details are cleared when you leave.")
        OutlinedTextField(draft.name,{onChange(it,draft.code,draft.status)},Modifier.fillMaxWidth(),label={Text("New project name")},enabled=!busy && !state.sent,singleLine=true)
        OutlinedTextField(draft.code,{onChange(draft.name,it,draft.status)},Modifier.fillMaxWidth(),label={Text("New project code")},enabled=!busy && !state.sent,singleLine=true)
        Box {
            OutlinedButton(enabled=!busy && !state.sent,onClick={statuses=true}) {Text("Status: ${draft.status.replace('_',' ')}")}
            DropdownMenu(expanded=statuses,onDismissRequest={statuses=false}) {
                listOf("active","on_hold","completed","archived").forEach {status->
                    DropdownMenuItem(text={Text(status.replace('_',' '))},onClick={statuses=false;onChange(draft.name,draft.code,status)})
                }
            }
        }
        if(state.sent)Text("An interrupted request may already have created this project. Retry keeps the same project and operation.")
        Button(enabled=!busy && draft.valid,onClick=onSave) {Text(if(state.sent) "Retry same project creation" else "Create company project")}
        OutlinedButton(enabled=!busy,onClick=onClose) {Text("Close new project")}
    }
}

@Composable
fun CompanyProjectRemoval(state:CompanyProjectDeleteState,busy:Boolean,onConfirm:(String)->Unit,onDelete:()->Unit,onReview:()->Unit,onClose:()->Unit) {
    val project=state.action.base
    Column(verticalArrangement=CarbonSpacing.stackArrangement) {
        Text("Delete company project",style=MaterialTheme.typography.titleMedium)
        Text("${project.name} · ${project.code} · Revision ${project.version}")
        Text("This project and its tasks will disappear from live company views for all members. Records and audit history are retained. Local practice projects are unchanged.")
        state.conflict?.let {current->
            Text("Current project: ${current.name} · ${current.code} · Revision ${current.version}")
            Text("Nothing was deleted. Review the current project and confirm again.")
            OutlinedButton(enabled=!busy,onClick=onReview) {Text("Review current project for deletion")}
        } ?: run {
            OutlinedTextField(state.action.confirmation,onConfirm,Modifier.fillMaxWidth(),label={Text("Type DELETE to confirm")},enabled=!busy && !state.sent,singleLine=true)
            if(state.sent)Text("An interrupted request may already have removed this project. Retry checks the same operation.")
            Button(enabled=!busy && state.action.valid,onClick=onDelete) {Text(if(state.sent) "Retry same project deletion" else "Confirm project deletion")}
        }
        OutlinedButton(enabled=!busy,onClick=onClose) {Text("Cancel project deletion")}
    }
}
