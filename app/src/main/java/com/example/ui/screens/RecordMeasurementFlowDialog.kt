@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import com.example.ui.icons.CarbonIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.ContractorEntity
import com.example.data.local.entity.ContractorQualifiedItemEntity
import com.example.data.local.entity.FloorEntity
import com.example.ui.viewmodel.SiteViewModel
import com.example.domain.WorkCatalog

enum class RecordFlowStep {
    SELECT_CONTRACTOR,
    SELECT_ITEM,
    SELECT_FLOOR
}

@Composable
fun RecordMeasurementFlowDialog(
    viewModel: SiteViewModel,
    projectId: Long,
    onDismiss: () -> Unit,
    onLaunchDedicatedMeasurement: (contractorId: Long, itemName: String, uom: String, calcType: CalculationType, floorId: Long, floorName: String) -> Unit
) {
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val allQualifiedItems by viewModel.allQualifiedItems.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(RecordFlowStep.SELECT_CONTRACTOR) }
    var selectedContractor by remember { mutableStateOf<ContractorEntity?>(null) }
    var selectedQualifiedItem by remember { mutableStateOf<ContractorQualifiedItemEntity?>(null) }
    var selectedFloor by remember { mutableStateOf<FloorEntity?>(null) }
    var expandedWorkType by remember { mutableStateOf<String?>(null) }

    val availableContractorItems = remember(selectedContractor, allQualifiedItems) {
        if (selectedContractor != null) {
            allQualifiedItems.filter { it.contractorId == selectedContractor!!.id }
        } else emptyList()
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Record Measurement",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (currentStep) {
                                RecordFlowStep.SELECT_CONTRACTOR -> "Step 1 of 3: Select Contractor"
                                RecordFlowStep.SELECT_ITEM -> "Step 2 of 3: Select Qualified Item"
                                RecordFlowStep.SELECT_FLOOR -> "Step 3 of 3: Select Floor / Level"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(CarbonIcons.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Step Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (currentStep != RecordFlowStep.SELECT_CONTRACTOR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (currentStep == RecordFlowStep.SELECT_FLOOR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Selected chips summary if past step 1
                if (selectedContractor != null || selectedQualifiedItem != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedContractor != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text(
                                    text = "Contractor: ${selectedContractor!!.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (selectedQualifiedItem != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = "Item: ${selectedQualifiedItem!!.itemName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Step 1: SELECT CONTRACTOR
                when (currentStep) {
                    RecordFlowStep.SELECT_CONTRACTOR -> {
                        Text(
                            text = "Choose Contractor executing the work:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (contractors.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No contractors available. Please register contractors first from the Contractors tab.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(contractors, key = { it.id }) { contractor ->
                                    val itemCount = allQualifiedItems.count { it.contractorId == contractor.id }
                                    Surface(
                                        color = if (selectedContractor?.id == contractor.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedContractor?.id == contractor.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedContractor = contractor
                                                selectedQualifiedItem = null
                                                expandedWorkType = null
                                                currentStep = RecordFlowStep.SELECT_ITEM
                                            }
                                            .testTag("select_contractor_${contractor.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = CarbonIcons.Engineering,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = contractor.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "$itemCount Qualified Items • ${contractor.contactNo.ifBlank { contractor.phone }}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = CarbonIcons.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 2: SELECT ITEM FROM QUALIFIED ITEMS
                    RecordFlowStep.SELECT_ITEM -> {
                        Text(
                            text = "Select item from ${selectedContractor?.name}'s qualified trades:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (availableContractorItems.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "This contractor has no qualified items registered yet.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = {
                                            // Fallback default item
                                            selectedQualifiedItem = ContractorQualifiedItemEntity(
                                                contractorId = selectedContractor!!.id,
                                                itemName = "Civil Works",
                                                uom = "m²",
                                                calculationType = CalculationType.AREA
                                            )
                                            currentStep = RecordFlowStep.SELECT_FLOOR
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Use General Item", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            val groupedItems = availableContractorItems.groupBy {
                                it.workType.ifBlank { WorkCatalog.classify(selectedContractor?.contractorType.orEmpty(), it.itemName) }
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                groupedItems.forEach { (workType, workItems) ->
                                    item(key = "work_type_$workType") {
                                        val expanded = expandedWorkType == workType
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                expandedWorkType = if (expanded) null else workType
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(workType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("${workItems.size} work item${if (workItems.size == 1) "" else "s"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Icon(
                                                    if (expanded) CarbonIcons.ExpandLess else CarbonIcons.ExpandMore,
                                                    contentDescription = if (expanded) "Collapse work type" else "Expand work type",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    if (expandedWorkType == workType) items(workItems, key = { "qualified_${it.id}" }) { item ->
                                        val isPlaster = item.itemName.contains("plaster", ignoreCase = true)
                                    Surface(
                                        color = if (selectedQualifiedItem?.id == item.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedQualifiedItem?.id == item.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp)
                                            .clickable {
                                                selectedQualifiedItem = item
                                                currentStep = RecordFlowStep.SELECT_FLOOR
                                            }
                                            .testTag("select_item_${item.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaster) CarbonIcons.FormatPaint else CarbonIcons.SquareFoot,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = item.itemName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (isPlaster) {
                                                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                                                Text("Linked to Brickwork", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(2.dp))
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "Formula: ${item.calculationType.displayName} • Unit: ${item.uom}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = CarbonIcons.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }

                    // Step 3: SELECT FLOOR / LEVEL
                    RecordFlowStep.SELECT_FLOOR -> {
                        Text(
                            text = "Select Floor / Level to record measurements for:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (floors.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No floors defined for this project. Level 0 (Ground Floor) will be used.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(floors, key = { it.id }) { floor ->
                                    Surface(
                                        color = if (selectedFloor?.id == floor.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedFloor?.id == floor.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedFloor = floor
                                                // Launch dedicated screen directly!
                                                val contractor = selectedContractor!!
                                                val item = selectedQualifiedItem!!
                                                onLaunchDedicatedMeasurement(
                                                    contractor.id,
                                                    item.itemName,
                                                    item.uom,
                                                    item.calculationType,
                                                    floor.id,
                                                    floor.name
                                                )
                                            }
                                            .testTag("select_floor_${floor.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = CarbonIcons.Layers,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = floor.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("Open Screen", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Icon(
                                                    imageVector = CarbonIcons.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dialog Navigation Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep != RecordFlowStep.SELECT_CONTRACTOR) {
                        OutlinedButton(
                            onClick = {
                                currentStep = when (currentStep) {
                                    RecordFlowStep.SELECT_FLOOR -> RecordFlowStep.SELECT_ITEM
                                    RecordFlowStep.SELECT_ITEM -> RecordFlowStep.SELECT_CONTRACTOR
                                    else -> RecordFlowStep.SELECT_CONTRACTOR
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Back", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
