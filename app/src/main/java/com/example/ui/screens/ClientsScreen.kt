package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ClientEntity
import com.example.ui.theme.*
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
        containerColor = CarbonWhite,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CarbonBlue60,
                contentColor = CarbonWhite,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("fab_add_client")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Client", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonWhite)
                    .drawBehind {
                        drawLine(
                            color = CarbonGray20,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Clients Directory",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100
                        )
                        Text(
                            text = "${clients.size} Registered Client${if (clients.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = CarbonGray70,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search Bar
                CarbonSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search clients...",
                    testTag = "input_search_clients"
                )
            }

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
                            tint = CarbonGray40,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No clients created yet" else "No matching clients found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarbonGray80
                        )
                        Text(
                            text = "Clients must be registered prior to creating new site projects.",
                            fontSize = 12.sp,
                            color = CarbonGray60,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60),
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
            onSave = { name, address, contactNo ->
                if (clientToEdit != null) {
                    viewModel.updateClient(
                        clientToEdit!!.copy(
                            name = name,
                            address = address,
                            contactNo = contactNo
                        )
                    )
                } else {
                    viewModel.addClient(name, address, contactNo)
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
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonRed60),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Delete", color = CarbonWhite)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { clientToDelete = null },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Cancel", color = CarbonGray100)
                }
            },
            containerColor = CarbonWhite,
            shape = RoundedCornerShape(4.dp)
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
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonWhite),
        border = BorderStroke(1.dp, CarbonGray30),
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
                            .background(CarbonBlue10, shape = RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = CarbonBlue60,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = client.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100
                        )
                        Text(
                            text = if (projectCount > 0) "$projectCount Active Project(s)" else "No Projects Assigned",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (projectCount > 0) CarbonBlue60 else CarbonGray60
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Client", tint = CarbonGray70, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Client", tint = CarbonRed60, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Divider(color = CarbonGray20, thickness = 1.dp)

            // Address
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CarbonGray60,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = client.address.ifBlank { "No address specified" },
                    fontSize = 12.sp,
                    color = CarbonGray80
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
                    tint = CarbonGray60,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Contact / Phone: ${client.contactNo.ifBlank { "Not provided" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CarbonGray90
                )
            }
        }
    }
}

@Composable
fun ClientFormDialog(
    initialClient: ClientEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, contactNo: String) -> Unit
) {
    var name by remember { mutableStateOf(initialClient?.name ?: "") }
    var address by remember { mutableStateOf(initialClient?.address ?: "") }
    var contactNo by remember { mutableStateOf(initialClient?.contactNo ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = CarbonWhite,
            border = BorderStroke(1.dp, CarbonGray30),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        color = CarbonGray100
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CarbonGray70)
                    }
                }

                Divider(color = CarbonGray20, thickness = 1.dp)

                if (errorMessage != null) {
                    Surface(
                        color = CarbonRed10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonRed60),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = CarbonRed60,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                CarbonInputField(
                    label = "CLIENT NAME *",
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    placeholder = "e.g. Laxmi Developers Ltd.",
                    keyboardType = KeyboardType.Text,
                    testTag = "input_client_name"
                )

                CarbonInputField(
                    label = "CLIENT ADDRESS",
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "e.g. Suite 401, Apex Hub, MG Road",
                    keyboardType = KeyboardType.Text,
                    testTag = "input_client_address"
                )

                CarbonInputField(
                    label = "CONTACT NO / PHONE *",
                    value = contactNo,
                    onValueChange = { contactNo = it },
                    placeholder = "e.g. +91 98450 12345",
                    keyboardType = KeyboardType.Phone,
                    testTag = "input_client_contact"
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray40)
                    ) {
                        Text("Cancel", color = CarbonGray100, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Client name is required."
                            } else {
                                onSave(name.trim(), address.trim(), contactNo.trim())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_client"),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                    ) {
                        Text(
                            if (initialClient != null) "Update Client" else "Save Client",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CarbonWhite
                        )
                    }
                }
            }
        }
    }
}
