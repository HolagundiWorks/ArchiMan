package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.holagundi.archiman.company.*
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonSpacing

@Composable
fun CompanyTaskActionForm(state:CompanyTaskActionState,page:CompanyTaskCandidatePage?,busy:Boolean,canBack:Boolean,
 onToggle:(String)->Unit,onClear:()->Unit,onConfirm:(String)->Unit,onSave:()->Unit,onReview:()->Unit,onClose:()->Unit,onPage:(Boolean)->Unit) {
 val draft=state.draft
 Column(verticalArrangement=CarbonSpacing.stackArrangement) {
  Text(if(draft.deleting)"Delete company task" else "Manage task assignees",style=MaterialTheme.typography.titleMedium)
  Text("${draft.base.title} · Revision ${draft.base.version}")
  Text("Unsaved selections stay in memory and clear when you leave or disconnect.")
  state.conflict?.let {current->
   Text("Current task: ${current.title} · Revision ${current.version}")
   Text("Nothing changed by this operation. Review the current task before submitting again.")
   OutlinedButton(enabled=!busy,onClick=onReview) {Text("Review current task")}
  } ?: run {
   if(draft.deleting) {
    Text("This hides the company task from live views. Its record, assignments and audit history are retained. Local practice tasks remain separate.")
    OutlinedTextField(draft.confirmation,onConfirm,Modifier.fillMaxWidth(),label={Text("Type DELETE to confirm task deletion")},enabled=!busy && !state.sent,singleLine=true)
   } else {
    Text("Select up to 20 active company members who can read this project. Selected: ${draft.membershipIds.size}")
    OutlinedButton(enabled=!busy && !state.sent,onClick=onClear) {Text("Clear task assignees")}
    if(page==null)Text("Connecting to eligible members…") else {
     if(page.members.isEmpty())Text("No eligible members on this page.")
     page.members.forEach {member->OutlinedButton(enabled=!busy && !state.sent && (member.membershipId in draft.membershipIds || draft.membershipIds.size<20),onClick={onToggle(member.membershipId)}) {Text(if(member.membershipId in draft.membershipIds)"Selected: ${member.name}" else member.name)}}
     if(canBack)OutlinedButton(enabled=!busy && !state.sent,onClick={onPage(true)}) {Text("Previous eligible members")}
     if(page.nextCursor!=null)OutlinedButton(enabled=!busy && !state.sent,onClick={onPage(false)}) {Text("Next eligible members")}
    }
   }
   if(state.sent)Text("The request may already have succeeded. Retry sends the same operation and expected revision.")
   Button(enabled=!busy && draft.valid,onClick=onSave) {Text(if(state.sent)"Retry same task action" else if(draft.deleting)"Confirm task deletion" else "Save task assignees")}
  }
  OutlinedButton(enabled=!busy,onClick=onClose) {Text("Close task action")}
 }
}
