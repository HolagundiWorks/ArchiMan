@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ClientEntity
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: SiteViewModel
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<ClientEntity?>(null) }
    var clientToDelete by remember { mutableStateOf<ClientEntity?>(null) }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else {
            clients.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true) ||
                it.contactNo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Clients")
                            Text("${clients.size} registered", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search clients") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("input_search_clients"),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_client")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add client")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Client List
            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No clients created yet" else "No matching clients found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Clients must be registered prior to creating new site projects.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_empty_add_client")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create First Client", fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val clientProjectsCount = projects.count { it.clientId == client.id || it.client.equals(client.name, ignoreCase = true) }
                        ClientCard(
                            client = client,
                            projectCount = clientProjectsCount,
                            onEdit = { clientToEdit = client },
                            onDelete = { clientToDelete = client }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Client Dialog
    if (showAddDialog || clientToEdit != null) {
        ClientFormDialog(
            initialClient = clientToEdit,
            onDismiss = {
                showAddDialog = false
                clientToEdit = null
            },
            onSave = { client ->
                if (clientToEdit != null) {
                    viewModel.updateClient(
                        client.copy(id = clientToEdit!!.id, createdAt = clientToEdit!!.createdAt)
                    )
                } else {
                    viewModel.addClient(client)
                }
                showAddDialog = false
                clientToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (clientToDelete != null) {
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            title = { Text("Delete Client", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to delete client \"${clientToDelete?.name}\"? Existing projects will retain their name records.") },
            confirmButton = {
                Button(
                    onClick = {
                        clientToDelete?.let { viewModel.deleteClient(it) }
                        clientToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { clientToDelete = null },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small
        )
    }
}

@Composable
fun ClientCard(
    client: ClientEntity,
    projectCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}"),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = client.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${client.clientType} • ${if (projectCount > 0) "$projectCount project(s)" else "No projects"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (projectCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Client", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Client", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // Address
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = client.address.ifBlank { "No address specified" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contact / Contract No
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = listOf(client.contactPerson, client.contactNo, client.email).filter(String::isNotBlank).joinToString(" • ").ifBlank { "No contact details provided" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ClientFormDialog(
    initialClient: ClientEntity?,
    onDismiss: () -> Unit,
    onSave: (ClientEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialClient?.name ?: "") }
    var clientType by remember { mutableStateOf(initialClient?.clientType ?: "Individual") }
    var contactPerson by remember { mutableStateOf(initialClient?.contactPerson ?: "") }
    var address by remember { mutableStateOf(initialClient?.address ?: "") }
    var correspondenceAddress by remember { mutableStateOf(initialClient?.correspondenceAddress ?: "") }
    var contactNo by remember { mutableStateOf(initialClient?.contactNo ?: "") }
    var email by remember { mutableStateOf(initialClient?.email ?: "") }
    var preferredCommunication by remember { mutableStateOf(initialClient?.preferredCommunication ?: "Phone") }
    var notes by remember { mutableStateOf(initialClient?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialClient != null) "Edit Client Details" else "Add New Client",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    label = { Text("CLIENT NAME *") },
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. Laxmi Developers Ltd.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_name"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("CLIENT TYPE") },
                    value = clientType,
                    onValueChange = { clientType = it },
                    placeholder = { Text("Individual, Company, Trust, Government...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_type"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("CONTACT PERSON") },
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    placeholder = { Text("Primary contact name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_contact_person"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("CLIENT ADDRESS") },
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("e.g. Suite 401, Apex Hub, MG Road") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_address"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("CORRESPONDENCE ADDRESS") },
                    value = correspondenceAddress,
                    onValueChange = { correspondenceAddress = it },
                    placeholder = { Text("Leave blank when same as client address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_correspondence_address"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("CONTACT NO / PHONE *") },
                    value = contactNo,
                    onValueChange = { contactNo = it },
                    placeholder = { Text("e.g. +91 98450 12345") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_contact"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("EMAIL") },
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("client@example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_email"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("PREFERRED COMMUNICATION") },
                    value = preferredCommunication,
                    onValueChange = { preferredCommunication = it },
                    placeholder = { Text("Phone, email, WhatsApp, letter...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_preferred_communication"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("NOTES") },
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Relationship or communication notes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_notes"),
                    singleLine = true)

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Client name is required."
                            } else {
                                onSave(
                                    ClientEntity(
                                        name = name.trim(),
                                        clientType = clientType.trim().ifBlank { "Individual" },
                                        contactPerson = contactPerson.trim(),
                                        address = address.trim(),
                                        correspondenceAddress = correspondenceAddress.trim(),
                                        contactNo = contactNo.trim(),
                                        email = email.trim(),
                                        preferredCommunication = preferredCommunication.trim().ifBlank { "Phone" },
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_client"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            if (initialClient != null) "Update Client" else "Save Client",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}
