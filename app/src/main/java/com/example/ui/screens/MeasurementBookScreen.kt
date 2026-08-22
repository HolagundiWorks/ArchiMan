package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.MeasurementEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementBookScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

    var filterFloorId by remember { mutableStateOf<Long?>(null) }
    var filterItemId by remember { mutableStateOf<Long?>(null) }
    var showExportOptionsSheet by remember { mutableStateOf(false) }

    val projectMeasurements = measurements.filter { it.projectId == (currentProject?.id ?: 0L) }
    val filteredMeasurements = projectMeasurements.filter { m ->
        (filterFloorId == null || m.floorId == filterFloorId) &&
        (filterItemId == null || m.itemId == filterItemId)
    }

    // Grouping by Floor -> Room / Location
    val groupedByFloor = filteredMeasurements.groupBy { it.floor.ifBlank { "General Floor" } }
    val totalRecords = filteredMeasurements.size

    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    Scaffold(
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CarbonGray100)
                        }
                        Column {
                            Text(
                                text = "Measurement Book (M-Book)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CarbonGray100
                            )
                            Text(
                                text = "${currentProject?.name ?: "Project"} • Engineering Record",
                                fontSize = 11.sp,
                                color = CarbonBlue60,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Export button
                    Button(
                        onClick = { showExportOptionsSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonBlue60,
                            contentColor = CarbonWhite
                        ),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_mbook_export")
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CarbonWhite)
        ) {
            // Filters bar
            Surface(
                color = CarbonGray10,
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FILTERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray70,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$totalRecords recorded measurements",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonBlue60
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAllSelected = filterFloorId == null && filterItemId == null
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isAllSelected) CarbonGray100 else CarbonWhite,
                            border = BorderStroke(1.dp, if (isAllSelected) CarbonBlack else CarbonGray30),
                            modifier = Modifier.clickable {
                                filterFloorId = null
                                filterItemId = null
                            }
                        ) {
                            Text(
                                text = "All (${projectMeasurements.size})",
                                fontSize = 11.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllSelected) CarbonWhite else CarbonGray100,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        floors.forEach { f ->
                            val isSelected = filterFloorId == f.id
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = if (isSelected) CarbonBlue60 else CarbonWhite,
                                border = BorderStroke(1.dp, if (isSelected) CarbonBlue70 else CarbonGray30),
                                modifier = Modifier.clickable {
                                    filterFloorId = if (filterFloorId == f.id) null else f.id
                                }
                            ) {
                                Text(
                                    text = f.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CarbonWhite else CarbonGray100,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        items.forEach { item ->
                            val isSelected = filterItemId == item.id
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = if (isSelected) CarbonBlue60 else CarbonWhite,
                                border = BorderStroke(1.dp, if (isSelected) CarbonBlue70 else CarbonGray30),
                                modifier = Modifier.clickable {
                                    filterItemId = if (filterItemId == item.id) null else item.id
                                }
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CarbonWhite else CarbonGray100,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Traditional M-Book List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (filteredMeasurements.isEmpty()) {
                    item {
                        Surface(
                            color = CarbonGray10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonGray20),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = CarbonGray70, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("No measurements in M-Book", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarbonGray100)
                                Spacer(Modifier.height(4.dp))
                                Text("Record dimensions from Quick Entry or Project Workspace.", fontSize = 12.sp, color = CarbonGray70)
                            }
                        }
                    }
                } else {
                    groupedByFloor.forEach { (floorName, floorList) ->
                        item {
                            // Floor Header
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = CarbonGray100,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(15.dp), tint = CarbonWhite)
                                        Spacer(Modifier.width(6.dp))
                                        Text(floorName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CarbonWhite, letterSpacing = 0.5.sp)
                                    }
                                    Text("${floorList.size} entries", fontSize = 11.sp, color = CarbonGray30)
                                }
                            }
                        }

                        // Group by Location / Component
                        val groupedByRoom = floorList.groupBy { it.location.ifBlank { "General Space" } }

                        groupedByRoom.forEach { (roomLocation, roomMeasurements) ->
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CarbonWhite,
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, CarbonGray30)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Space Title
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = roomLocation,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = CarbonGray100
                                            )
                                            Text(
                                                text = "${roomMeasurements.size} items",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CarbonBlue60
                                            )
                                        }

                                        Spacer(Modifier.height(6.dp))
                                        HorizontalDivider(color = CarbonGray20)
                                        Spacer(Modifier.height(6.dp))

                                        // Column Headers (M-Book format)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ITEM / DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray70, modifier = Modifier.weight(2.2f))
                                            Text("DIMENSIONS (NOS × L × W × D)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray70, modifier = Modifier.weight(2f))
                                            Text("QUANTITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray70, modifier = Modifier.weight(1.2f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        // Rows
                                        roomMeasurements.forEachIndexed { rIdx, m ->
                                            val dimText = when (m.calculationType) {
                                                CalculationType.RUNNING_LENGTH -> "${m.nos.toInt()} × ${m.length}m"
                                                CalculationType.AREA -> "${m.nos.toInt()} × ${m.length} × ${m.width}"
                                                CalculationType.WALL_PLASTER -> "${m.nos.toInt()} × ${m.length} × ${m.height}${if (m.deduction > 0) " (-${m.deduction})" else ""}"
                                                CalculationType.VOLUME -> "${m.nos.toInt()} × ${m.length} × ${m.width} × ${m.height}"
                                                CalculationType.NOS -> "${m.nos.toInt()} nos"
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (rIdx % 2 == 1) CarbonGray10 else CarbonWhite)
                                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(2.2f)) {
                                                    Text(m.itemName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CarbonGray100)
                                                    if (m.description.isNotBlank() && m.description != m.itemName) {
                                                        Text(m.description, fontSize = 11.sp, color = CarbonGray70)
                                                    }
                                                }
                                                Text(
                                                    text = dimText,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = CarbonGray90,
                                                    modifier = Modifier.weight(2f)
                                                )
                                                Text(
                                                    text = "${String.format(Locale.getDefault(), "%.3f", m.quantity)} ${m.unit}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CarbonBlue60,
                                                    modifier = Modifier.weight(1.2f),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Options Modal Sheet
    if (showExportOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportOptionsSheet = false },
            containerColor = CarbonWhite,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "EXPORT MEASUREMENT BOOK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray100,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Choose your preferred engineering export format for ${currentProject?.name ?: "Current Site"}:",
                    fontSize = 12.sp,
                    color = CarbonGray70
                )

                Spacer(modifier = Modifier.height(4.dp))

                // PDF
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = CarbonGray10,
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showExportOptionsSheet = false
                            ExportHelper.exportAndSharePdf(context, currentProject?.name ?: "Site", filteredMeasurements)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CarbonRed60)
                        Column {
                            Text("PDF Document (.pdf)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarbonGray100)
                            Text("Standard A4 layout with dimension tables & item subtotals", fontSize = 11.sp, color = CarbonGray70)
                        }
                    }
                }

                // XLS
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = CarbonGray10,
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showExportOptionsSheet = false
                            ExportHelper.exportAndShareXls(context, currentProject?.name ?: "Site", filteredMeasurements)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = CarbonGreen60)
                        Column {
                            Text("Excel Spreadsheet (.xls)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarbonGray100)
                            Text("Formatted Excel workbook with styled headers & quantity formulas", fontSize = 11.sp, color = CarbonGray70)
                        }
                    }
                }

                // CSV
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = CarbonGray10,
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showExportOptionsSheet = false
                            ExportHelper.exportAndShareCsv(context, currentProject?.name ?: "Site", filteredMeasurements)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = CarbonBlue60)
                        Column {
                            Text("CSV Data Sheet (.csv)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarbonGray100)
                            Text("Universal raw comma-delimited file with UTF-8 BOM", fontSize = 11.sp, color = CarbonGray70)
                        }
                    }
                }

                // Print
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = CarbonGray10,
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showExportOptionsSheet = false
                            ExportHelper.printMeasurementSheetPdf(context, currentProject?.name ?: "Site", filteredMeasurements)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = CarbonGray80)
                        Column {
                            Text("Print Measurement Book", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarbonGray100)
                            Text("Open system print dialog for wireless/PDF printing", fontSize = 11.sp, color = CarbonGray70)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
