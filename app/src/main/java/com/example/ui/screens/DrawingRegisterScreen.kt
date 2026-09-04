package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.DrawingRevisionEntity
import com.example.data.local.entity.DrawingTransmittalEntity
import com.example.data.local.entity.ProjectDrawingEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SiteViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DrawingRegisterScreen(
    viewModel: SiteViewModel,
    drawings: List<ProjectDrawingEntity>,
    revisions: List<DrawingRevisionEntity>,
    transmittals: List<DrawingTransmittalEntity>
) {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var revisionOf by remember { mutableStateOf<ProjectDrawingEntity?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pendingUri = uri
        } else {
            revisionOf = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CarbonBlue10, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Architecture, contentDescription = null, tint = CarbonBlue60)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Controlled drawing register", fontWeight = FontWeight.Bold, color = CarbonGray100)
                    Text(
                        "DWG viewing, markup and measurement engine is the next delivery slice. Registered source revisions remain authoritative.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarbonGray70
                    )
                }
                Button(
                    onClick = {
                        revisionOf = null
                        picker.launch(arrayOf("application/acad", "application/x-acad", "application/x-autocad", "application/dwg", "image/vnd.dwg", "application/octet-stream"))
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Drawing")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RegisterStat("DRAWINGS", drawings.size.toString(), Modifier.weight(1f))
            RegisterStat("REVISIONS", revisions.size.toString(), Modifier.weight(1f))
            RegisterStat("TRANSMITTALS", transmittals.size.toString(), Modifier.weight(1f))
        }

        if (drawings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Layers, null, tint = CarbonGray50, modifier = Modifier.size(40.dp))
                    Text("No controlled drawings", fontWeight = FontWeight.Bold, color = CarbonGray90)
                    Text("Import the first DWG revision to start the register.", color = CarbonGray60)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(drawings, key = { it.id }) { drawing ->
                    val drawingRevisions = revisions.filter { it.drawingId == drawing.id }
                    DrawingRegisterCard(
                        drawing = drawing,
                        revisions = drawingRevisions,
                        onAddRevision = {
                            revisionOf = drawing
                            picker.launch(arrayOf("application/acad", "application/x-acad", "application/x-autocad", "application/dwg", "image/vnd.dwg", "application/octet-stream"))
                        },
                        onOpen = { revision ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(revision.fileUri), revision.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "No DWG renderer is installed yet", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
    }

    pendingUri?.let { uri ->
        DrawingRevisionDialog(
            existingDrawing = revisionOf,
            initialFileName = displayName(context, uri),
            onDismiss = {
                pendingUri = null
                revisionOf = null
            },
            onSave = { number, title, discipline, revision, status, notes, asBuilt ->
                val mime = context.contentResolver.getType(uri) ?: "application/acad"
                val existing = revisionOf
                if (existing == null) {
                    viewModel.registerDrawing(number, title, discipline, revision, displayName(context, uri), mime, uri.toString(), status, notes, asBuilt)
                } else {
                    viewModel.addDrawingRevision(existing, revision, displayName(context, uri), mime, uri.toString(), status, notes, asBuilt)
                }
                pendingUri = null
                revisionOf = null
            }
        )
    }
}

@Composable
private fun RegisterStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = CarbonGray10, border = BorderStroke(1.dp, CarbonGray20)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = CarbonGray60)
            Text(value, fontWeight = FontWeight.Bold, color = CarbonGray100)
        }
    }
}

@Composable
private fun DrawingRegisterCard(
    drawing: ProjectDrawingEntity,
    revisions: List<DrawingRevisionEntity>,
    onAddRevision: () -> Unit,
    onOpen: (DrawingRevisionEntity) -> Unit
) {
    val latest = revisions.maxByOrNull { it.createdAt }
    Surface(color = CarbonWhite, border = BorderStroke(1.dp, CarbonGray30), shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Description, null, tint = CarbonBlue60)
                Column(modifier = Modifier.weight(1f)) {
                    Text(drawing.drawingNumber, fontWeight = FontWeight.Bold, color = CarbonGray100)
                    Text(drawing.title, color = CarbonGray80, style = MaterialTheme.typography.bodySmall)
                    Text("${drawing.discipline} · ${revisions.size} revision(s)", color = CarbonGray60, style = MaterialTheme.typography.labelSmall)
                }
                AssistChip(onClick = {}, label = { Text(latest?.issueStatus ?: drawing.status) })
            }
            latest?.let { revision ->
                HorizontalDivider(color = CarbonGray20)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Revision ${revision.revisionCode}${if (revision.isAsBuilt) " · AS-BUILT" else ""}", fontWeight = FontWeight.SemiBold)
                        Text("${revision.fileName} · ${formatDate(revision.createdAt)}", style = MaterialTheme.typography.labelSmall, color = CarbonGray60)
                    }
                    IconButton(onClick = { onOpen(revision) }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open drawing source")
                    }
                }
            }
            OutlinedButton(onClick = onAddRevision, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add immutable revision")
            }
        }
    }
}

@Composable
private fun DrawingRevisionDialog(
    existingDrawing: ProjectDrawingEntity?,
    initialFileName: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, Boolean) -> Unit
) {
    var number by remember { mutableStateOf(existingDrawing?.drawingNumber.orEmpty()) }
    var title by remember { mutableStateOf(existingDrawing?.title.orEmpty()) }
    var discipline by remember { mutableStateOf(existingDrawing?.discipline ?: "Architectural") }
    var revision by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("WIP") }
    var notes by remember { mutableStateOf("") }
    var asBuilt by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingDrawing == null) "Register DWG" else "Add drawing revision") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(initialFileName, style = MaterialTheme.typography.labelSmall, color = CarbonGray60)
                OutlinedTextField(number, { number = it }, label = { Text("Drawing number") }, enabled = existingDrawing == null, singleLine = true)
                OutlinedTextField(title, { title = it }, label = { Text("Drawing title") }, enabled = existingDrawing == null, singleLine = true)
                OutlinedTextField(discipline, { discipline = it }, label = { Text("Discipline") }, enabled = existingDrawing == null, singleLine = true)
                OutlinedTextField(revision, { revision = it }, label = { Text("Revision code") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WIP", "SHARED", "ISSUED").forEach { option ->
                        FilterChip(selected = status == option, onClick = { status = option }, label = { Text(option) })
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Revision notes") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = asBuilt, onCheckedChange = { asBuilt = it })
                    Text("As-built revision")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = number.isNotBlank() && title.isNotBlank() && revision.isNotBlank(),
                onClick = { onSave(number, title, discipline, revision, status, notes, asBuilt) }
            ) { Text("Save revision") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun displayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) return cursor.getString(column)
    }
    return uri.lastPathSegment ?: "drawing.dwg"
}

private fun formatDate(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))
