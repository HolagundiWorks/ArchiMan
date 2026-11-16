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
import com.holagundi.archiman.company.*
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonSpacing
import java.time.LocalDate

@Composable
fun CompanySiteRecordsSection(state:CompanyAccountState,busy:Boolean,onStart:(CompanySiteRecord?,Boolean)->Unit,onChange:(CompanySiteRecord,String)->Unit,onSave:()->Unit,onReview:(Boolean)->Unit,onClose:()->Unit,onPage:(Boolean)->Unit) {
 Text("Live company site records",style=MaterialTheme.typography.titleMedium)
 Text("Instructions, inspections and snags. Local practice site records remain separate.")
 val page=state.sitePage
 if(page==null)Text("Connecting to company site records…") else {
  if(page.records.isEmpty())Text("No site records on this page.")
  if("sites.write" in state.identity?.permissions.orEmpty() && state.siteEditor==null)OutlinedButton(enabled=!busy,onClick={onStart(null,false)}) {Text("New company site record")}
  state.siteEditor?.takeIf {it.draft.base==null}?.let {CompanySiteRecordForm(it,busy,onChange,onSave,onReview,onClose)}
  page.records.forEach {record->
   Text(record.title,style=MaterialTheme.typography.titleSmall)
   Text("${record.kind} · ${record.recordDate} · ${record.status} · Revision ${record.version}")
   if(record.location.isNotBlank())Text("Location: ${record.location}")
   if(record.assignedTo.isNotBlank())Text("Responsible: ${record.assignedTo}")
   if(record.details.isNotBlank())Text(record.details)
   record.dueDate?.let {Text("Due: $it"+if(record.status=="open" && it<LocalDate.now().toString())" · Overdue" else "")}
   record.resolvedDate?.let {Text("Resolved: $it")}
   if(state.siteEditor?.draft?.base?.id!=record.id) {
    if("sites.write" in state.identity?.permissions.orEmpty())OutlinedButton(enabled=!busy,onClick={onStart(record,false)}) {Text("Edit company site record")}
    if("sites.delete" in state.identity?.permissions.orEmpty())OutlinedButton(enabled=!busy,onClick={onStart(record,true)}) {Text("Delete company site record")}
   }
   state.siteEditor?.takeIf {it.draft.base?.id==record.id}?.let {CompanySiteRecordForm(it,busy,onChange,onSave,onReview,onClose)}
  }
  if(state.canGoBackSites)OutlinedButton(enabled=!busy,onClick={onPage(true)}) {Text("Previous site records")}
  if(page.nextCursor!=null)OutlinedButton(enabled=!busy,onClick={onPage(false)}) {Text("Next site records")}
 }
}
@Composable
private fun CompanySiteRecordForm(state:CompanySiteEditorState,busy:Boolean,onChange:(CompanySiteRecord,String)->Unit,onSave:()->Unit,onReview:(Boolean)->Unit,onClose:()->Unit) {
 val draft=state.draft;val value=draft.value;val enabled=!busy && !state.sent
 var kinds by remember {mutableStateOf(false)};var statuses by remember {mutableStateOf(false)}
 Column(verticalArrangement=CarbonSpacing.stackArrangement) {
  Text(if(draft.deleting)"Delete company site record" else if(draft.base==null)"New company site record" else "Edit company site record",style=MaterialTheme.typography.titleMedium)
  Text("Unsaved details stay in memory and clear when you leave or disconnect.")
  if(draft.deleting) {
   Text("${value.title} · Revision ${draft.base?.version}")
   Text("This hides the record from live views and retains its database record and audit history.")
   OutlinedTextField(draft.confirmation,{onChange(value,it)},Modifier.fillMaxWidth(),label={Text("Type DELETE to confirm site record deletion")},enabled=enabled,singleLine=true)
  } else {
   Box {
    OutlinedButton(enabled=enabled,onClick={kinds=true}) {Text("Kind: ${value.kind}")}
    DropdownMenu(expanded=kinds,onDismissRequest={kinds=false}) {listOf("instruction","inspection","snag").forEach {kind->DropdownMenuItem(text={Text(kind)},onClick={kinds=false;onChange(value.copy(kind=kind),"")})}}
   }
   OutlinedTextField(value.recordDate,{onChange(value.copy(recordDate=it),"")},Modifier.fillMaxWidth(),label={Text("Record date YYYY-MM-DD")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.title,{onChange(value.copy(title=it),"")},Modifier.fillMaxWidth(),label={Text("Site record title")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.location,{onChange(value.copy(location=it),"")},Modifier.fillMaxWidth(),label={Text("Site location")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.details,{onChange(value.copy(details=it),"")},Modifier.fillMaxWidth(),label={Text("Site details")},enabled=enabled)
   OutlinedTextField(value.assignedTo,{onChange(value.copy(assignedTo=it),"")},Modifier.fillMaxWidth(),label={Text("Person responsible (record text)")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.dueDate.orEmpty(),{onChange(value.copy(dueDate=it.takeIf {it.isNotBlank()}),"")},Modifier.fillMaxWidth(),label={Text("Due date YYYY-MM-DD (optional)")},enabled=enabled,singleLine=true)
   Box {
    OutlinedButton(enabled=enabled,onClick={statuses=true}) {Text("Status: ${value.status}")}
    DropdownMenu(expanded=statuses,onDismissRequest={statuses=false}) {listOf("open","resolved","closed").forEach {status->DropdownMenuItem(text={Text(status)},onClick={statuses=false;onChange(value.copy(status=status,resolvedDate=if(status=="open")null else value.resolvedDate ?: LocalDate.now().toString()),"")})}}
   }
   if(value.status!="open")OutlinedTextField(value.resolvedDate.orEmpty(),{onChange(value.copy(resolvedDate=it.takeIf {it.isNotBlank()}),"")},Modifier.fillMaxWidth(),label={Text("Resolution date YYYY-MM-DD")},enabled=enabled,singleLine=true)
  }
  state.conflict?.let {current->
   Text("Current record: ${current.title} · ${current.status} · Revision ${current.version}")
   OutlinedButton(enabled=!busy,onClick={onReview(false)}) {Text("Use current site record")}
   if(!draft.deleting)OutlinedButton(enabled=!busy,onClick={onReview(true)}) {Text("Keep my site draft against current revision")}
  } ?: run {
   if(state.sent)Text("The request may already have succeeded. Retry sends the same operation and values.")
   Button(enabled=!busy && draft.valid,onClick=onSave) {Text(if(state.sent)"Retry same site operation" else if(draft.deleting)"Confirm site record deletion" else "Save company site record")}
  }
  OutlinedButton(enabled=!busy,onClick=onClose) {Text("Close site record form")}
 }
}
