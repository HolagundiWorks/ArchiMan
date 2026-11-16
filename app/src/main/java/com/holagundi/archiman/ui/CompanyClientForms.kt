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
fun CompanyClientForm(state:CompanyClientEditorState,busy:Boolean,onChange:(CompanyClient,String)->Unit,onSave:()->Unit,onReview:(Boolean)->Unit,onClose:()->Unit) {
 val draft=state.draft;val value=draft.value;val enabled=!busy && !state.sent
 Column(verticalArrangement=CarbonSpacing.stackArrangement) {
  Text(if(draft.deleting)"Delete company client" else if(draft.base==null)"New company client" else "Edit company client",style=MaterialTheme.typography.titleMedium)
  Text("Unsaved details stay in memory and clear when you leave or disconnect.")
  if(draft.deleting) {
   Text("${value.name} · Revision ${draft.base?.version}")
   Text("Deletion hides this contact from live views and retains audit history. Unlink all active projects first.")
   OutlinedTextField(draft.confirmation,{onChange(value,it)},Modifier.fillMaxWidth(),label={Text("Type DELETE to confirm client deletion")},enabled=enabled,singleLine=true)
  } else {
   OutlinedTextField(value.name,{onChange(value.copy(name=it),"")},Modifier.fillMaxWidth(),label={Text("Client name")},enabled=enabled,singleLine=true)
   listOf("individual","company","architect_firm").forEach {kind->OutlinedButton(enabled=enabled,onClick={onChange(value.copy(kind=kind),"")}) {Text(if(kind==value.kind)"Selected: ${kind.replace('_',' ')}" else kind.replace('_',' '))}}
   OutlinedTextField(value.contactPerson,{onChange(value.copy(contactPerson=it),"")},Modifier.fillMaxWidth(),label={Text("Contact person")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.email,{onChange(value.copy(email=it),"")},Modifier.fillMaxWidth(),label={Text("Client email")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.phone,{onChange(value.copy(phone=it),"")},Modifier.fillMaxWidth(),label={Text("Client phone")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.address,{onChange(value.copy(address=it),"")},Modifier.fillMaxWidth(),label={Text("Postal address")},enabled=enabled)
   OutlinedTextField(value.correspondenceAddress,{onChange(value.copy(correspondenceAddress=it),"")},Modifier.fillMaxWidth(),label={Text("Correspondence address")},enabled=enabled)
   OutlinedTextField(value.preferredCommunication,{onChange(value.copy(preferredCommunication=it),"")},Modifier.fillMaxWidth(),label={Text("Preferred communication")},enabled=enabled,singleLine=true)
   OutlinedTextField(value.notes,{onChange(value.copy(notes=it),"")},Modifier.fillMaxWidth(),label={Text("Client notes")},enabled=enabled)
  }
  state.conflict?.let {current->
   Text("Current client: ${current.name} · ${current.email} · Revision ${current.version}")
   Text("Nothing was changed by this operation. Review the current record before submitting again.")
   OutlinedButton(enabled=!busy,onClick={onReview(false)}) {Text("Use current client details")}
   if(!draft.deleting)OutlinedButton(enabled=!busy,onClick={onReview(true)}) {Text("Keep my client draft against current revision")}
  } ?: run {
   if(state.sent)Text("The request may already have succeeded. Retry sends the same operation and values.")
   Button(enabled=!busy && draft.valid,onClick=onSave) {Text(if(state.sent)"Retry same client operation" else if(draft.deleting)"Confirm client deletion" else "Save company client")}
  }
  OutlinedButton(enabled=!busy,onClick=onClose) {Text("Close client form")}
 }
}

@Composable
fun CompanyClientLinkForm(state:CompanyClientLinkState,page:CompanyClientPage?,busy:Boolean,onChoose:(String?)->Unit,onSave:()->Unit,onReview:()->Unit,onClose:()->Unit) {
 val draft=state.draft
 Column(verticalArrangement=CarbonSpacing.stackArrangement) {
  Text("Project client",style=MaterialTheme.typography.titleMedium)
  Text("Current: ${draft.base.clientName ?: "No client"} · Project revision ${draft.base.version}")
  Text("Changing this link changes which contact assigned project members can see. Browse company client pages above to find another contact.")
  Text("Selected: ${if(draft.clientId==null)"No client" else page?.clients?.find {it.id==draft.clientId}?.name ?: if(draft.clientId==draft.base.clientId)draft.base.clientName else "Contact on another page"}")
  if(state.conflict!=null) {
   Text("Current link: ${state.conflict.currentName()} · Revision ${state.conflict.version}")
   OutlinedButton(enabled=!busy,onClick=onReview) {Text("Review current project client")}
  } else {
   OutlinedButton(enabled=!busy && !state.sent,onClick={onChoose(null)}) {Text("Select no client")}
   page?.clients?.forEach {client->OutlinedButton(enabled=!busy && !state.sent,onClick={onChoose(client.id)}) {Text("Select ${client.name}")}}
   if(state.sent)Text("Retry uses the same link operation and expected revision.")
   Button(enabled=!busy && draft.valid && (state.sent || draft.clientId!=draft.base.clientId),onClick=onSave) {Text(if(state.sent)"Retry same client link" else "Save project client")}
  }
  OutlinedButton(enabled=!busy,onClick=onClose) {Text("Close project client")}
 }
}
private fun CompanyProjectClient.currentName()=clientName ?: "No client"
