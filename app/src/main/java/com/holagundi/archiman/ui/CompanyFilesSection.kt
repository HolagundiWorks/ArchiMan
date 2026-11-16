package com.holagundi.archiman.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.holagundi.archiman.company.*
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonSpacing

@Composable
fun CompanyFilesSection(state:CompanyAccountState,busy:Boolean,onStartUpload:()->Unit,onUpload:()->Unit,onCloseUpload:()->Unit,onOpen:(CompanyFile)->Unit,onPreviewPage:(Int)->Unit,onClosePreview:()->Unit,
 onDeleteStart:(CompanyFile)->Unit,onConfirm:(String)->Unit,onDelete:()->Unit,onReview:()->Unit,onDeleteClose:()->Unit,onPage:(Boolean)->Unit) {
 Text("Live company files",style=MaterialTheme.typography.titleMedium)
 Text("PDF, PNG, JPEG, WebP, TXT and CSV, up to 5 MiB. File previews stay in memory while connected.")
 if(state.fileStatus?.available==false)Text("Private attachment storage is unavailable. Contact your company administrator.")
 if("files.write" in state.identity?.permissions.orEmpty() && state.selectedFile!=null && state.fileUpload==null) {
  Text("Selected local file: ${state.selectedFile.name}")
  OutlinedButton(enabled=!busy && state.fileStatus?.available==true,onClick=onStartUpload) {Text("Use selected file for this project")}
 }
 state.fileUpload?.let {upload->Column(verticalArrangement=CarbonSpacing.stackArrangement) {
  Text("Upload ${upload.draft.name} · ${upload.draft.size} bytes")
  Text("Confirm this file belongs to the selected company project. The selection clears when you leave or disconnect.")
  if(upload.sent)Text("An interrupted upload may already have succeeded. Retry keeps the same file and operation.")
  Button(enabled=!busy && upload.draft.valid && state.fileStatus?.available==true,onClick=onUpload) {Text(if(upload.sent)"Retry same file upload" else "Confirm company file upload")}
  OutlinedButton(enabled=!busy,onClick=onCloseUpload) {Text("Close file upload")}
 }}
 val page=state.filePage
 if(page==null)Text("Connecting to company files…") else {
  if(page.files.isEmpty())Text("No company files on this page.")
  page.files.forEach {file->
   Text(file.name,style=MaterialTheme.typography.titleSmall)
   Text("${file.contentType} · ${file.size} bytes · Revision ${file.version}")
   OutlinedButton(enabled=!busy && state.fileStatus?.available==true,onClick={onOpen(file)}) {Text("View company file")}
   state.filePreview?.takeIf {it.file.id==file.id}?.let {preview->
    Text("${file.name} · Page ${preview.pageIndex+1} of ${preview.pageCount}")
    preview.bitmap?.let {Image(it.asImageBitmap(),contentDescription="Company file preview: ${file.name}",modifier=Modifier.fillMaxWidth())}
    preview.text?.let {Text(it)}
    if(preview.pageIndex>0)OutlinedButton(enabled=!busy,onClick={onPreviewPage(preview.pageIndex-1)}) {Text("Previous file page")}
    if(preview.pageIndex+1<preview.pageCount)OutlinedButton(enabled=!busy,onClick={onPreviewPage(preview.pageIndex+1)}) {Text("Next file page")}
    OutlinedButton(enabled=!busy,onClick=onClosePreview) {Text("Close company file preview")}
   }
   if("files.delete" in state.identity?.permissions.orEmpty() && state.fileDeletion?.action?.base?.id!=file.id)
    OutlinedButton(enabled=!busy,onClick={onDeleteStart(file)}) {Text("Delete company file")}
   state.fileDeletion?.takeIf {it.action.base.id==file.id}?.let {deletion->Column(verticalArrangement=CarbonSpacing.stackArrangement) {
    Text("Delete ${file.name}",style=MaterialTheme.typography.titleMedium)
    Text("This removes the file from live views. Its private stored content and audit history are retained.")
    deletion.conflict?.let {current->
     Text("Current file: ${current.name} · Revision ${current.version}")
     OutlinedButton(enabled=!busy,onClick=onReview) {Text("Review current file for deletion")}
    } ?: run {
     OutlinedTextField(deletion.action.confirmation,onConfirm,Modifier.fillMaxWidth(),label={Text("Type DELETE to confirm file deletion")},enabled=!busy && !deletion.sent,singleLine=true)
     if(deletion.sent)Text("Retry checks the same deletion operation.")
     Button(enabled=!busy && deletion.action.valid,onClick=onDelete) {Text(if(deletion.sent)"Retry same file deletion" else "Confirm company file deletion")}
    }
    OutlinedButton(enabled=!busy,onClick=onDeleteClose) {Text("Close file deletion")}
   }}
  }
  if(state.canGoBackFiles)OutlinedButton(enabled=!busy,onClick={onPage(true)}) {Text("Previous company files")}
  if(page.nextCursor!=null)OutlinedButton(enabled=!busy,onClick={onPage(false)}) {Text("Next company files")}
 }
}
