package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import com.example.BuildConfig
import com.example.data.local.DATABASE_SCHEMA_VERSION
import com.example.domain.SupportDiagnosticReport
import com.example.domain.SupportDiagnosticSnapshot
import com.example.util.ExportHelper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val measurementSheets by viewModel.measurementSheets.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    var filterFloorId by remember { mutableStateOf<Long?>(null) }
    var filterItemId by remember { mutableStateOf<Long?>(null) }

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

    val projectMeasurements = measurements.filter { it.projectId == (currentProject?.id ?: 0L) }
    val filteredMeasurements = projectMeasurements.filter { m ->
        (filterFloorId == null || m.floorId == filterFloorId) &&
        (filterItemId == null || m.itemId == filterItemId)
    }

    val totalRecords = filteredMeasurements.size
    val itemGroups = filteredMeasurements.groupBy { it.itemName }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonWhite)
    ) {
        // Carbon Header
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
                            text = "AM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = CarbonWhite
                        )
                    }
                    Column {
                        Text(
                            text = "Export Measurement Book",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100
                        )
                        Text(
                            text = "CSV • XLS Excel • PDF",
                            fontSize = 11.sp,
                            color = CarbonBlue60,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Total badge
                Surface(
                    color = CarbonBlue10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonBlue60)
                ) {
                    Text(
                        text = "$totalRecords Entries",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonBlue70,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Context Header
            item {
                Surface(
                    color = CarbonGray10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGray20),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE PROJECT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray70,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentProject?.name ?: "No Project Selected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray100
                            )
                            Text(
                                text = "${currentProject?.siteLocation ?: "Site"} • ${itemGroups.keys.size} distinct work items",
                                fontSize = 12.sp,
                                color = CarbonGray70
                            )
                        }

                        IconButton(
                            onClick = { viewModel.openCanonicalMeasurement() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Change", tint = CarbonBlue60)
                        }
                    }
                }
            }

            // ==========================================
            // EXPORT ACTIONS (CSV, XLS, PDF, PRINT)
            // ==========================================
            item {
                Text(
                    text = "SELECT EXPORT FORMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray70,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. PDF Export
                    CarbonExportTile(
                        title = "Export to PDF (.pdf)",
                        subtitle = "Standard A4 Measurement Book with dimension sheets & item totals",
                        icon = Icons.Default.PictureAsPdf,
                        badge = "READY TO SHARE",
                        badgeColor = CarbonRed60,
                        onClick = {
                            ExportHelper.exportAndSharePdf(
                                context = context,
                                projectName = currentProject?.name ?: "Site",
                                measurements = filteredMeasurements
                            )
                        },
                        testTag = "btn_export_pdf"
                    )

                    // 2. XLS Excel Export
                    CarbonExportTile(
                        title = "Export to Excel (.xls)",
                        subtitle = "Native spreadsheet with formatted tables, column widths & formulas",
                        icon = Icons.Default.TableChart,
                        badge = "MS EXCEL / SHEETS",
                        badgeColor = CarbonGreen60,
                        onClick = {
                            ExportHelper.exportAndShareXls(
                                context = context,
                                projectName = currentProject?.name ?: "Site",
                                measurements = filteredMeasurements
                            )
                        },
                        testTag = "btn_export_xls"
                    )

                    // 3. CSV Export
                    CarbonExportTile(
                        title = "Export to CSV (.csv)",
                        subtitle = "Comma-separated values with UTF-8 BOM for CAD, BIM, ERP & analysis",
                        icon = Icons.Default.Description,
                        badge = "RAW DATA",
                        badgeColor = CarbonBlue60,
                        onClick = {
                            ExportHelper.exportAndShareCsv(
                                context = context,
                                projectName = currentProject?.name ?: "Site",
                                measurements = filteredMeasurements
                            )
                        },
                        testTag = "btn_export_csv"
                    )

                    // 4. Print / PDF System Dialog
                    CarbonExportTile(
                        title = "Print / Save PDF Dialog",
                        subtitle = "Send directly to connected Wi-Fi/Bluetooth printer or system PDF printer",
                        icon = Icons.Default.Print,
                        badge = "A4 PRINT",
                        badgeColor = CarbonGray80,
                        onClick = {
                            ExportHelper.printMeasurementSheetPdf(
                                context = context,
                                projectName = currentProject?.name ?: "Site",
                                measurements = filteredMeasurements
                            )
                        },
                        testTag = "btn_print_pdf"
                    )

                    CarbonExportTile(
                        title = "Share support diagnostics (.json)",
                        subtitle = "Metadata-only health report; excludes names, measurements, descriptions, comments, and photos",
                        icon = Icons.Default.HealthAndSafety,
                        badge = "PRIVACY SAFE",
                        badgeColor = CarbonBlue60,
                        onClick = {
                            val knownSheetIds = measurementSheets.mapTo(hashSetOf()) { it.id }
                            val knownItemIds = items.mapTo(hashSetOf()) { it.id }
                            val report = SupportDiagnosticReport.render(
                                SupportDiagnosticSnapshot(
                                    appVersion = BuildConfig.VERSION_NAME,
                                    versionCode = BuildConfig.VERSION_CODE.toLong(),
                                    schemaVersion = DATABASE_SCHEMA_VERSION,
                                    androidSdk = Build.VERSION.SDK_INT,
                                    generatedAtEpochMs = System.currentTimeMillis(),
                                    projectCount = projects.size,
                                    contractorCount = contractors.size,
                                    workItemCount = items.size,
                                    measurementCount = measurements.size,
                                    sheetCount = measurementSheets.size,
                                    archivedSheetCount = measurementSheets.count { it.archivedAt != null },
                                    rowsWithoutKnownSheet = measurements.count { it.sheetId !in knownSheetIds },
                                    rowsWithoutKnownWorkItem = measurements.count { it.itemId !in knownItemIds },
                                    sheetStatusCounts = measurementSheets.groupingBy { it.status }.eachCount()
                                )
                            )
                            ExportHelper.shareSupportDiagnostics(context, report)
                        },
                        testTag = "btn_support_diagnostics"
                    )
                }
            }

            // Filters
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "FILTER RECORDS FOR EXPORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray70,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isAllSelected = filterFloorId == null && filterItemId == null
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = if (isAllSelected) CarbonGray100 else CarbonGray10,
                        border = BorderStroke(1.dp, if (isAllSelected) CarbonBlack else CarbonGray30),
                        modifier = Modifier.clickable {
                            filterFloorId = null
                            filterItemId = null
                        }
                    ) {
                        Text(
                            text = "All Records (${projectMeasurements.size})",
                            fontSize = 12.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllSelected) CarbonWhite else CarbonGray100,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    floors.forEach { f ->
                        val isSelected = filterFloorId == f.id
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) CarbonBlue60 else CarbonGray10,
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlue70 else CarbonGray30),
                            modifier = Modifier.clickable {
                                filterFloorId = if (filterFloorId == f.id) null else f.id
                            }
                        ) {
                            Text(
                                text = f.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CarbonWhite else CarbonGray100,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items.forEach { item ->
                        val isSelected = filterItemId == item.id
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) CarbonBlue60 else CarbonGray10,
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlue70 else CarbonGray30),
                            modifier = Modifier.clickable {
                                filterItemId = if (filterItemId == item.id) null else item.id
                            }
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CarbonWhite else CarbonGray100,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Summary of quantities to be exported
            item {
                Text(
                    text = "QUANTITY ABSTRACT PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray70,
                    letterSpacing = 0.5.sp
                )
            }

            if (itemGroups.isEmpty()) {
                item {
                    Surface(
                        color = CarbonGray10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray20),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = CarbonGray70, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No measurements found to export", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarbonGray100)
                            Text("Record measurements in Quick Entry first.", fontSize = 12.sp, color = CarbonGray70)
                        }
                    }
                }
            } else {
                items(itemGroups.entries.toList()) { (itemName, list) ->
                    val totalQty = list.sumOf { it.quantity }
                    val unit = list.firstOrNull()?.unit ?: "unit"

                    Surface(
                        color = CarbonWhite,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray20),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonGray100
                                )
                                Text(
                                    text = "${list.size} recorded entries across ${list.map { it.floor }.distinct().size} floor(s)",
                                    fontSize = 11.sp,
                                    color = CarbonGray70
                                )
                            }
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.3f", totalQty)} $unit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonBlue60
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CarbonExportTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = CarbonWhite,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, CarbonGray30),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CarbonGray10, RoundedCornerShape(2.dp))
                    .border(1.dp, CarbonGray20, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray100
                    )
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = CarbonGray70,
                    lineHeight = 15.sp
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CarbonGray70, modifier = Modifier.size(20.dp))
        }
    }
}
