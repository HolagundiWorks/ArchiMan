package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.ItemMasterEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemMasterEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CarbonWhite,
        topBar = {
            Surface(
                color = CarbonWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = CarbonGray20,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CarbonGray100, shape = RoundedCornerShape(2.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AMB",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = CarbonWhite
                            )
                        }
                        Column {
                            Text(
                                text = "Work Items & Calculation Formulas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray100
                            )
                            Text(
                                text = "BOQ Master Library",
                                fontSize = 11.sp,
                                color = CarbonBlue60,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { showAddItemDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonBlue60,
                            contentColor = CarbonWhite
                        ),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_add_item_top")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = CarbonBlue60,
                contentColor = CarbonWhite,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Work Item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Context bar
            Surface(
                color = CarbonGray10,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, CarbonGray20),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "STANDARD ITEMS (${items.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray70,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Pure Quantity Calculations",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarbonBlue60
                    )
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = CarbonWhite,
                        border = BorderStroke(1.dp, CarbonGray30),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { itemToEdit = item }
                            .testTag("item_master_row_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonGray100
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Formula Tag
                                    Surface(
                                        color = CarbonBlue10,
                                        shape = RoundedCornerShape(2.dp),
                                        border = BorderStroke(1.dp, CarbonBlue60)
                                    ) {
                                        Text(
                                            text = item.calculationType.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CarbonBlue70,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Unit Tag
                                    Surface(
                                        color = CarbonGray10,
                                        shape = RoundedCornerShape(2.dp),
                                        border = BorderStroke(1.dp, CarbonGray30)
                                    ) {
                                        Text(
                                            text = item.unit,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CarbonGray80,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { itemToEdit = item },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CarbonBlue60, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CarbonRed60, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        ItemMasterEditorDialog(
            initialItem = null,
            onDismiss = { showAddItemDialog = false },
            onSave = { name, unit, calcType ->
                viewModel.addItem(
                    ItemMasterEntity(
                        name = name,
                        unit = unit,
                        defaultRate = 0.0,
                        calculationType = calcType
                    )
                )
                showAddItemDialog = false
            }
        )
    }

    // Edit Item Dialog
    itemToEdit?.let { item ->
        ItemMasterEditorDialog(
            initialItem = item,
            onDismiss = { itemToEdit = null },
            onSave = { name, unit, calcType ->
                viewModel.updateItem(
                    item.copy(
                        name = name,
                        unit = unit,
                        calculationType = calcType
                    )
                )
                itemToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemMasterEditorDialog(
    initialItem: ItemMasterEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, calcType: CalculationType) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "cum") }
    var selectedCalcType by remember { mutableStateOf(initialItem?.calculationType ?: CalculationType.VOLUME) }

    val commonUnits = listOf("cum", "sqm", "rmt", "nos", "kg", "ton", "bags", "litres")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = CarbonWhite,
            border = BorderStroke(1.dp, CarbonGray30),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (initialItem == null) "ADD WORK ITEM" else "EDIT WORK ITEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray100,
                    letterSpacing = 0.5.sp
                )

                // Item Name
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ITEM NAME / DESCRIPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray70
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. RCC M25 Beam Concrete") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CarbonBlue60,
                            unfocusedBorderColor = CarbonGray30,
                            focusedContainerColor = CarbonWhite,
                            unfocusedContainerColor = CarbonGray10
                        )
                    )
                }

                // Calculation Formula Type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CALCULATION FORMULA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray70
                    )
                    CalculationType.values().forEach { calcType ->
                        val isSelected = selectedCalcType == calcType
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) CarbonBlue10 else CarbonGray10,
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlue60 else CarbonGray30),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCalcType = calcType
                                    if (unit == "cum" || unit == "sqm" || unit == "rmt" || unit == "nos") {
                                        unit = when (calcType) {
                                            CalculationType.VOLUME -> "cum"
                                            CalculationType.AREA, CalculationType.WALL_PLASTER -> "sqm"
                                            CalculationType.RUNNING_LENGTH -> "rmt"
                                            CalculationType.NOS -> "nos"
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = calcType.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CarbonBlue70 else CarbonGray100
                                    )
                                    Text(
                                        text = when (calcType) {
                                            CalculationType.VOLUME -> "Nos × Length × Width × Height/Depth"
                                            CalculationType.AREA -> "Nos × Length × Width"
                                            CalculationType.WALL_PLASTER -> "Nos × Length × Height (- Deductions)"
                                            CalculationType.RUNNING_LENGTH -> "Nos × Length"
                                            CalculationType.NOS -> "Direct item count (Nos)"
                                        },
                                        fontSize = 10.sp,
                                        color = CarbonGray70
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Measurement Unit
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "UNIT OF MEASUREMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray70
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonUnits.take(4).forEach { u ->
                            val isSel = unit.equals(u, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = if (isSel) CarbonBlue60 else CarbonGray10,
                                border = BorderStroke(1.dp, if (isSel) CarbonBlue70 else CarbonGray30),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { unit = u }
                            ) {
                                Text(
                                    text = u,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) CarbonWhite else CarbonGray100,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = CarbonGray70)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), unit.trim(), selectedCalcType)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonBlue60,
                            contentColor = CarbonWhite
                        ),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Save Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
