package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BillEntity
import com.example.data.local.entity.MeasurementEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    var showCreateBillDialog by remember { mutableStateOf(false) }
    var viewingBill by remember { mutableStateOf<BillEntity?>(null) }
    var billMeasurements by remember { mutableStateOf<List<MeasurementEntity>>(emptyList()) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

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
                        text = "Contractor Bills",
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
                        .background(SleekPrimaryContainer)
                        .clickable { showCreateBillDialog = true }
                        .testTag("btn_add_bill_top"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Bill", tint = SleekPrimaryBlue, modifier = Modifier.size(20.dp))
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateBillDialog = true },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                text = { Text("Generate Bill", fontWeight = FontWeight.Bold) },
                containerColor = SleekPrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("fab_generate_bill")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SleekSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = SleekTextTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No Bills Generated Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "Create contractor bills from unbilled site measurements",
                            fontSize = 13.sp,
                            color = SleekTextSecondary
                        )
                        Button(
                            onClick = { showCreateBillDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryBlue),
                            modifier = Modifier.testTag("btn_create_first_bill")
                        ) {
                            Text("Create First Bill")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(bills, key = { it.id }) { bill ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SleekSurfaceLight,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, SleekOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewingBill = bill
                                    coroutineScope.launch {
                                        billMeasurements = viewModel.repository.getMeasurementsForBill(bill.id)
                                    }
                                }
                                .testTag("card_bill_${bill.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = bill.billNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SleekPrimaryBlue
                                        )
                                        Text(
                                            text = sdf.format(Date(bill.date)),
                                            fontSize = 11.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                    Surface(
                                        color = SleekSuccessContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "NET: ${currencyFormat.format(bill.netAmount)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = SleekSuccess,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Divider(color = SleekDivider, thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Contractor: ${bill.contractorName}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = SleekTextPrimary
                                        )
                                        Text(
                                            text = "Project: ${bill.projectName}",
                                            fontSize = 12.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Gross: ${currencyFormat.format(bill.totalAmount)}",
                                            fontSize = 12.sp,
                                            color = SleekTextSecondary
                                        )
                                        if (bill.retentionPercent > 0) {
                                            Text(
                                                text = "Retention: ${bill.retentionPercent}%",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
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

    // CREATE BILL DIALOG
    if (showCreateBillDialog) {
        var billProjId by remember { mutableStateOf(selectedProjectId ?: projects.firstOrNull()?.id ?: 0L) }
        val projectContractors = contractors.filter { it.projectId == billProjId }
        var billContId by remember { mutableStateOf(projectContractors.firstOrNull()?.id ?: contractors.firstOrNull()?.id ?: 0L) }
        var retentionInput by remember { mutableStateOf("0") }
        var notesInput by remember { mutableStateOf("Running Bill") }

        // Find unbilled measurements for this selection
        val unbilled = measurements.filter { it.projectId == billProjId && it.contractorId == billContId && (it.billId == null || it.billId == 0L) }
        val unbilledGross = unbilled.sumOf { it.amount }
        val retentionPct = retentionInput.toDoubleOrNull() ?: 0.0
        val netPayable = unbilledGross - (unbilledGross * retentionPct / 100.0)

        // Group unbilled items to show abstract
        val grouped = unbilled.groupBy { it.itemName }

        Dialog(onDismissRequest = { showCreateBillDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurfaceLight,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generate Contractor Bill",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )

                    // Project selection
                    Text("Select Project:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextSecondary)
                    var projDropOpen by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SleekSurfaceVariant,
                            border = BorderStroke(1.dp, SleekOutlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable { projDropOpen = true }
                        ) {
                            Text(
                                text = projects.firstOrNull { it.id == billProjId }?.name ?: "Select Project",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextPrimary
                            )
                        }
                        DropdownMenu(expanded = projDropOpen, onDismissRequest = { projDropOpen = false }) {
                            projects.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        billProjId = p.id
                                        val conList = contractors.filter { it.projectId == p.id }
                                        if (conList.isNotEmpty()) billContId = conList.first().id
                                        projDropOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Contractor selection
                    Text("Select Contractor:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextSecondary)
                    var contDropOpen by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SleekSurfaceVariant,
                            border = BorderStroke(1.dp, SleekOutlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable { contDropOpen = true }
                        ) {
                            Text(
                                text = contractors.firstOrNull { it.id == billContId }?.name ?: "Select Contractor",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold,
                                color = SleekPrimaryBlue
                            )
                        }
                        DropdownMenu(expanded = contDropOpen, onDismissRequest = { contDropOpen = false }) {
                            contractors.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        billContId = c.id
                                        contDropOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Abstract of Items included
                    Text(
                        text = "APPROVED MEASUREMENTS (${unbilled.size} entries)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    if (unbilled.isEmpty()) {
                        Surface(
                            color = SleekSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SleekOutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No unbilled measurements found for this contractor on this project.",
                                fontSize = 12.sp,
                                color = SleekTextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            grouped.forEach { (itemName, list) ->
                                val totalQty = list.sumOf { it.quantity }
                                val firstRate = list.first().rate
                                val itemAmt = totalQty * firstRate
                                Surface(
                                    color = SleekSurfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SleekOutlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$itemName: ${String.format(Locale.getDefault(), "%.2f", totalQty)} ${list.first().unit} @ ₹$firstRate",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SleekTextPrimary
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.getDefault(), "%,.0f", itemAmt)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = retentionInput,
                            onValueChange = { retentionInput = it },
                            label = { Text("Retention %") },
                            modifier = Modifier.width(110.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Bill Notes") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Summary Total Box
                    Surface(
                        color = SleekPrimaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Work:", fontSize = 12.sp, color = SleekOnPrimaryContainer)
                                Text(currencyFormat.format(unbilledGross), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekOnPrimaryContainer)
                            }
                            if (retentionPct > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Retention ($retentionPct%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    Text("- ${currencyFormat.format(unbilledGross * retentionPct / 100.0)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Divider(color = SleekPrimaryBlue.copy(alpha = 0.2f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NET PAYABLE:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SleekOnPrimaryContainer)
                                Text(currencyFormat.format(netPayable), fontSize = 15.sp, fontWeight = FontWeight.Black, color = SleekOnPrimaryContainer)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateBillDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (unbilled.isNotEmpty()) {
                                    viewModel.generateBill(
                                        projectId = billProjId,
                                        contractorId = billContId,
                                        retentionPercent = retentionPct,
                                        notes = notesInput
                                    )
                                    showCreateBillDialog = false
                                }
                            },
                            enabled = unbilled.isNotEmpty(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryBlue),
                            modifier = Modifier.testTag("btn_confirm_generate_bill")
                        ) {
                            Text("Generate Bill")
                        }
                    }
                }
            }
        }
    }

    // VIEW / PRINT BILL DETAIL DIALOG
    if (viewingBill != null) {
        val bill = viewingBill!!
        val proj = projects.firstOrNull { it.id == bill.projectId }

        Dialog(onDismissRequest = { viewingBill = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurfaceLight,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = bill.billNumber,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimaryBlue
                            )
                            Text(text = "Date: ${sdf.format(Date(bill.date))}", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                        IconButton(onClick = { viewingBill = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SleekTextSecondary)
                        }
                    }

                    Surface(
                        color = SleekSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SleekOutlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Project: ${bill.projectName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekTextPrimary)
                            Text("Contractor: ${bill.contractorName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SleekTextSecondary)
                            if (bill.notes.isNotBlank()) {
                                Text("Notes: ${bill.notes}", fontSize = 11.sp, color = SleekTextTertiary)
                            }
                        }
                    }

                    // Measurement details inside bill
                    Text("ABSTRACT & MEASUREMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextSecondary, letterSpacing = 0.5.sp)
                    if (billMeasurements.isNotEmpty()) {
                        billMeasurements.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${m.itemName} (${String.format(Locale.getDefault(), "%.2f", m.quantity)} ${m.unit})", fontSize = 12.sp, color = SleekTextPrimary)
                                Text("₹${String.format(Locale.getDefault(), "%,.2f", m.amount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            }
                        }
                    }

                    Surface(
                        color = SleekPrimaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NET PAYABLE:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SleekOnPrimaryContainer)
                            Text(currencyFormat.format(bill.netAmount), fontWeight = FontWeight.Black, fontSize = 16.sp, color = SleekOnPrimaryContainer)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteBill(bill)
                                viewingBill = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Bill")
                        }

                        Button(
                            onClick = {
                                ExportHelper.printMeasurementSheetPdf(
                                    context = context,
                                    projectName = proj?.name ?: "Project",
                                    measurements = billMeasurements
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryBlue),
                            modifier = Modifier.testTag("btn_print_bill_pdf")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print / PDF Export")
                        }
                    }
                }
            }
        }
    }
}
