package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    var filterContractorId by remember { mutableStateOf<Long?>(null) }
    var selectedReportTab by remember { mutableStateOf(0) } // 0: By Item, 1: By Contractor, 2: By Location

    val currentProject = projects.firstOrNull { it.id == selectedProjectId }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    val filteredMeasurements = measurements.filter { m ->
        (selectedProjectId == null || m.projectId == selectedProjectId) &&
        (filterContractorId == null || m.contractorId == filterContractorId)
    }

    val totalAmount = filteredMeasurements.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBgLight,
        topBar = {
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SleekOutline)
                            .clickable { viewModel.navigateTo(AppScreen.HOME) }
                            .testTag("btn_back_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SleekTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Reports & Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekOutline)
                        .clickable {
                            ExportHelper.exportAndShareCsv(
                                context = context,
                                projectName = currentProject?.name ?: "All Projects",
                                measurements = filteredMeasurements
                            )
                        }
                        .testTag("btn_export_reports_csv"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sleek High Level Total Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SleekPrimaryBlue,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CUMULATIVE WORK VALUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = currencyFormat.format(totalAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${filteredMeasurements.size} items",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tab Selector: By Item / By Contractor / By Location (Sleek pill bar style)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekSurfaceVariant,
                border = BorderStroke(1.dp, SleekOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Item-wise", "Contractor", "Floor / Room").forEachIndexed { index, title ->
                        val isSelected = selectedReportTab == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SleekPrimaryBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedReportTab = index }
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else SleekTextSecondary,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Contractor Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAllSelected = filterContractorId == null
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAllSelected) SleekPrimaryBlue else SleekSurfaceVariant,
                    border = if (isAllSelected) null else BorderStroke(1.dp, SleekOutlineVariant),
                    modifier = Modifier.clickable { filterContractorId = null }
                ) {
                    Text(
                        text = "All Contractors",
                        fontSize = 11.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAllSelected) Color.White else SleekTextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                contractors.forEach { c ->
                    val isSelected = filterContractorId == c.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) SleekPrimaryBlue else SleekSurfaceVariant,
                        border = if (isSelected) null else BorderStroke(1.dp, SleekOutlineVariant),
                        modifier = Modifier.clickable { filterContractorId = c.id }
                    ) {
                        Text(
                            text = c.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else SleekTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Report Content based on selected tab
            when (selectedReportTab) {
                0 -> {
                    // Item-wise Breakdown
                    val itemGroups = filteredMeasurements.groupBy { it.itemName }
                    if (itemGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No measurements to report", color = SleekTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(itemGroups.entries.toList(), key = { it.key }) { (itemName, list) ->
                                val totalQty = list.sumOf { it.quantity }
                                val itemAmt = list.sumOf { it.amount }
                                val first = list.first()

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SleekSurfaceLight,
                                    border = BorderStroke(1.dp, SleekOutline),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = SleekTextPrimary
                                            )
                                            Text(
                                                text = currencyFormat.format(itemAmt),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = SleekPrimaryBlue
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Total Quantity: ${String.format(Locale.getDefault(), "%.2f", totalQty)} ${first.unit}",
                                                fontSize = 12.sp,
                                                color = SleekPrimaryBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${list.size} recordings",
                                                fontSize = 11.sp,
                                                color = SleekTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Contractor Breakdown
                    val contractorGroups = filteredMeasurements.groupBy { it.contractorName }
                    if (contractorGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No measurements to report", color = SleekTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(contractorGroups.entries.toList(), key = { it.key }) { (contractorName, list) ->
                                val contAmt = list.sumOf { it.amount }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SleekSurfaceLight,
                                    border = BorderStroke(1.dp, SleekOutline),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = contractorName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = SleekTextPrimary
                                            )
                                            Text(
                                                text = currencyFormat.format(contAmt),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = SleekSuccess
                                            )
                                        }
                                        Text(
                                            text = "${list.size} measurements across ${list.map { it.itemName }.distinct().size} work items",
                                            fontSize = 12.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Location / Floor Breakdown
                    val locationGroups = filteredMeasurements.groupBy {
                        val loc = listOf(it.floor, it.location).filter { str -> str.isNotBlank() }.joinToString(" - ")
                        if (loc.isBlank()) "Unspecified Location" else loc
                    }

                    if (locationGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No measurements to report", color = SleekTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(locationGroups.entries.toList(), key = { it.key }) { (locName, list) ->
                                val locAmt = list.sumOf { it.amount }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SleekSurfaceLight,
                                    border = BorderStroke(1.dp, SleekOutline),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = locName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = SleekTextPrimary
                                            )
                                            Text(
                                                text = "${list.size} recorded items",
                                                fontSize = 11.sp,
                                                color = SleekTextSecondary
                                            )
                                        }
                                        Text(
                                            text = currencyFormat.format(locAmt),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SleekPrimaryBlue
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
