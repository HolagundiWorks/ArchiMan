package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectRateBooksTab(
    viewModel: SiteViewModel,
    project: ProjectEntity?,
    contractors: List<ContractorEntity>,
    pwdItems: List<ItemMasterEntity>,
    rateBooks: List<ContractorRateBookEntity>,
    assignments: List<ProjectRateBookAssignmentEntity>
) {
    var contractorId by remember(contractors) { mutableStateOf(contractors.firstOrNull()?.id) }
    val contractor = contractors.firstOrNull { it.id == contractorId }
    val contractorBooks = rateBooks.filter { it.contractorId == contractorId }
    var selectedBookId by remember(contractorId, contractorBooks) {
        mutableStateOf(assignments.firstOrNull { it.contractorId == contractorId }?.rateBookId ?: contractorBooks.firstOrNull()?.id)
    }
    val selectedBook = contractorBooks.firstOrNull { it.id == selectedBookId }
    val bookItems by remember(selectedBookId) {
        selectedBookId?.let(viewModel.repository::getRateBookItems) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var contractorMenu by remember { mutableStateOf(false) }
    var showNewBook by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    val activeAssignment = assignments.firstOrNull { it.contractorId == contractorId }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contractor Rate Books", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Import PWD work items, enter contractor rates, then assign one book to this project.", color = CarbonGray70, fontSize = 11.sp)
            ExposedDropdownMenuBox(expanded = contractorMenu, onExpandedChange = { contractorMenu = it }) {
                OutlinedTextField(
                    value = contractor?.name.orEmpty(), onValueChange = {}, readOnly = true,
                    label = { Text("Contractor") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(contractorMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = contractorMenu, onDismissRequest = { contractorMenu = false }) {
                    contractors.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { contractorId = option.id; contractorMenu = false }) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("RATE BOOKS", fontWeight = FontWeight.Bold, color = CarbonGray70, fontSize = 11.sp)
                TextButton(enabled = contractor != null, onClick = { showNewBook = true }) { Icon(Icons.Default.Add, null); Text("New book") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(contractorBooks, key = { it.id }) { book ->
                    FilterChip(
                        selected = selectedBookId == book.id,
                        onClick = { selectedBookId = book.id },
                        label = { Text("${book.name} v${book.version}") },
                        leadingIcon = if (activeAssignment?.rateBookId == book.id) ({ Icon(Icons.Default.CheckCircle, "Applied", Modifier.size(16.dp)) }) else null
                    )
                }
            }
            if (contractorBooks.isEmpty() && contractor != null) Text("Create the contractor's first rate book.", color = CarbonGray60, fontSize = 12.sp)
            if (selectedBook != null && project != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.assignRateBook(project.id, selectedBook.contractorId, selectedBook.id) }, modifier = Modifier.weight(1f)) {
                        Text(if (activeAssignment?.rateBookId == selectedBook.id) "Applied to project" else "Apply to project")
                    }
                    OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) { Text("Import PWD items") }
                }
            }
        }
        HorizontalDivider(color = CarbonGray20)
        if (selectedBook == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.LibraryBooks, null, Modifier.size(42.dp), tint = CarbonGray50)
                Text("Select or create a rate book", fontWeight = FontWeight.Bold)
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(bookItems, key = { it.id }) { rateItem ->
                Card(colors = CardDefaults.cardColors(containerColor = CarbonWhite), border = BorderStroke(1.dp, CarbonGray20)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(rateItem.itemNameSnapshot, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("PWD ${rateItem.sourceItemCodeSnapshot} • ${rateItem.uomSnapshot}", color = CarbonBlue60, fontSize = 10.sp)
                            Text("₹ ${"%.2f".format(rateItem.rate)} / ${rateItem.uomSnapshot}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (rateItem.specificationSnapshot.isNotBlank()) Text(rateItem.specificationSnapshot, color = CarbonGray70, fontSize = 10.sp, maxLines = 2)
                        }
                        IconButton(onClick = { viewModel.deleteRateBookItem(rateItem) }) { Icon(Icons.Default.DeleteOutline, "Remove rate item") }
                    }
                }
            }
        }
    }

    if (showNewBook && contractor != null) NewRateBookDialog(onDismiss = { showNewBook = false }) { name, version ->
        viewModel.createRateBook(contractor.id, name, version) { selectedBookId = it }
        showNewBook = false
    }
    if (showImport && selectedBook != null) ImportPwdRateItemsDialog(
        items = pwdItems,
        existingItemIds = bookItems.mapTo(mutableSetOf()) { it.itemId },
        onDismiss = { showImport = false },
        onAdd = { item, rate -> viewModel.setRateBookItem(selectedBook.id, item, rate) }
    )
}

@Composable
private fun NewRateBookDialog(onDismiss: () -> Unit, onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }; var version by remember { mutableStateOf("1") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("New rate book") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Book name") }, singleLine = true)
            OutlinedTextField(version, { version = it.filter(Char::isDigit) }, label = { Text("Version") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, version.toIntOrNull() ?: 1) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ImportPwdRateItemsDialog(
    items: List<ItemMasterEntity>, existingItemIds: Set<Long>, onDismiss: () -> Unit, onAdd: (ItemMasterEntity, Double) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = items.filter { it.id !in existingItemIds && (search.isBlank() || it.name.contains(search, true) || it.sourceItemCode.contains(search, true)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Import from PWD SR") }, text = {
        Column(Modifier.heightIn(max = 520.dp)) {
            OutlinedTextField(search, { search = it }, label = { Text("Search item or SR code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { item ->
                    var rateText by remember(item.id) { mutableStateOf("") }
                    val rate = rateText.toDoubleOrNull()
                    Card(border = BorderStroke(1.dp, CarbonGray20), colors = CardDefaults.cardColors(containerColor = CarbonWhite)) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("PWD ${item.sourceItemCode} • ${item.unit}", color = CarbonBlue60, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(rateText, { rateText = it }, label = { Text("Rate / ${item.unit}") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                                Button(enabled = rate != null && rate >= 0, onClick = { onAdd(item, rate ?: 0.0) }) { Text("Add") }
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
