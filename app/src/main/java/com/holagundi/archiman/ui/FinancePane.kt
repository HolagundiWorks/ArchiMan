package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.FinanceRepository
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ArchiCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

@HiltViewModel
class FinanceViewModel @Inject constructor(val repo: FinanceRepository, @ApplicationContext private val context: Context): ViewModel() {
    val settings = repo.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val records = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val receipts = repo.receipts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bankRows=records.map { rows -> rows.filter { it.kind=="BANK" }.map { row -> val json=JSONObject(row.json);Triple(row,json,json.getString("date")) }.sortedByDescending { it.third } }.flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val invoices = repo.invoices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var notice by mutableStateOf(""); private set
    var noticeKind by mutableStateOf(com.holagundi.archiman.ui.theme.CarbonNoticeKind.INFO); private set
    var busy by mutableStateOf(false); private set
    var preview by mutableStateOf<List<BankRow>>(emptyList()); private set
    private var previewAccount = ""
    fun action(success: String = "Changes saved.", block: suspend () -> Unit) {
        if (busy) return
        busy = true; notice = ""; noticeKind = com.holagundi.archiman.ui.theme.CarbonNoticeKind.INFO
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() }; if (notice.isBlank()) notice = success; noticeKind = com.holagundi.archiman.ui.theme.CarbonNoticeKind.SUCCESS }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { notice = e.message ?: "Unable to save. Check your entries and retry."; noticeKind = com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR }
            finally { busy = false }
        }
    }
    fun read(uri: Uri, account: String, monthFirst: Boolean) = action("Statement preview ready. No records have been imported.") {
        withContext(Dispatchers.Main) { preview = emptyList() }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) { val size = input.read(buffer); if (size < 0) break; require(output.size() + size <= 5_000_000) { "Statement exceeds 5 MB." }; output.write(buffer, 0, size) }
            output.toByteArray() } ?: error("Cannot read statement.")
        require(bytes.size <= 5_000_000) { "Statement exceeds 5 MB." }
        val text = bytes.toString(Charsets.UTF_8)
        require(!text.startsWith("%PDF") && !text.startsWith("PK")) { "Export your statement as CSV or TSV. PDF and Excel files are not supported." }
        val parsed = BankCsv.parse(text, account, monthFirst)
        withContext(Dispatchers.Main) { preview = parsed; previewAccount = account }
    }
    fun discard() { preview = emptyList() }
    fun importPreview() = action { val count = repo.import(preview, previewAccount); withContext(Dispatchers.Main) { preview = emptyList(); notice = "Imported $count new transactions." } }
}

@Composable
fun FinancePane(vm: FinanceViewModel = hiltViewModel()) {
    val rows by vm.records.collectAsStateWithLifecycle(); val invoices by vm.invoices.collectAsStateWithLifecycle()
    val receipts by vm.receipts.collectAsStateWithLifecycle()
    val config = rows.firstOrNull { it.key == "settings" }?.json.orEmpty()
    var mode by rememberSaveable(config) { mutableStateOf(if (config.isBlank()) "NONE" else JSONObject(config).getString("mode")) }
    var rate by rememberSaveable(config) { mutableStateOf(if (config.isBlank()) "18" else JSONObject(config).getString("rate")) }
    var state by rememberSaveable(config) { mutableStateOf(if (config.isBlank()) "" else JSONObject(config).getString("state")) }
    var gstin by rememberSaveable(config) { mutableStateOf(if (config.isBlank()) "" else JSONObject(config).getString("gstin")) }
    var account by rememberSaveable { mutableStateOf("") }; var monthFirst by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }; var invoiceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var tds by rememberSaveable { mutableStateOf("") }; var reference by rememberSaveable { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.read(it, account, monthFirst) } }
    val banks by vm.bankRows.collectAsStateWithLifecycle()
    val invoiceNumbers=remember(invoices) { invoices.associate { it.id to it.number } }
    val reversed=remember(receipts) { receipts.mapNotNull { Regex("^REVERSAL:([0-9]+):").find(it.note)?.groupValues?.get(1)?.toLongOrNull() }.toSet() }
    val credits=remember(receipts,reversed) { receipts.filter { it.tdsAmount>0 && it.id !in reversed } }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Taxes & banking", style = MaterialTheme.typography.headlineSmall)
        Text("GST registration", style = MaterialTheme.typography.titleMedium)
        GstMode.entries.forEach { option -> Row { RadioButton(mode == option.name, { mode = option.name }); Text(when(option) { GstMode.NONE -> "No GST registration"; GstMode.COMPOSITION -> "Composition GST"; GstMode.REGULAR -> "Regular GST" }, Modifier.padding(top = 12.dp)) } }
        if (mode != "NONE") { ArchiField(gstin, { gstin = it.uppercase() }, "Supplier GSTIN"); ArchiField(state, { state = it }, "Supplier state code (two digits)") }
        if (mode == "REGULAR") ArchiField(rate, { rate = it }, "Default GST rate % (confirm for your supply)")
        if (mode == "COMPOSITION") Text("Bill of supply: GST is not collected from the client. Your composition levy is a separate business tax obligation. Select only if enrolled and eligible.")
        Text("Changes apply to new invoices. Existing invoice tax snapshots stay unchanged.")
        Button(enabled = !vm.busy, onClick = { vm.action { vm.repo.saveSettings(mode, rate, state, gstin) } }) { Text("Save GST settings") }
        HorizontalDivider()
        Text("Import bank statement", style = MaterialTheme.typography.titleMedium)
        ArchiField(account, { account = it }, "Stable account alias (e.g. Business savings)")
        Row { Checkbox(monthFirst, { monthFirst = it }); Text("Dates use MM/DD/YYYY (default DD/MM/YYYY)") }
        Text("CSV/TSV only, up to 5 MB and 10,000 rows. Use the same account alias for repeat imports. Review repeated transactions without a bank reference; overlapping exports can be ambiguous.")
        Button(enabled = account.isNotBlank() && !vm.busy, onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) }) { Text("Choose statement") }
        if (vm.preview.isNotEmpty()) {
            Text("Review ${vm.preview.size} transactions. Importing does not post receipts.")
            vm.preview.take(30).forEach { Text("${it.date} | ${it.amount} | ${it.description}") }
            if (vm.preview.size > 30) Text("Showing first 30 rows.")
            Button(enabled = !vm.busy, onClick = vm::importPreview) { Text("Import reviewed rows (skip duplicates)") }
            TextButton(onClick = vm::discard) { Text("Discard preview") }
        }
        FinanceNotice(vm)
        Text("TDS credits", style = MaterialTheme.typography.titleMedium)
        if (credits.isEmpty()) Text("No TDS deductions recorded.")
        } }
        items(credits,key={"tds-${it.id}"}) { receipt ->
            ArchiCard { Column(Modifier.padding(12.dp)) {
                Text("${invoiceNumbers[receipt.invoiceId] ?: receipt.invoiceId} | TDS ${receipt.tdsAmount}")
                Text("${receipt.tdsReference} | ${if(receipt.tdsVerified) "Verified against tax records" else "Unverified"}")
                if(!receipt.tdsVerified) VerifyTdsButton(receipt.id,vm)
            } }
        }
        item {
        Text("Reconcile bank transactions", style = MaterialTheme.typography.titleMedium)
        if (banks.isEmpty()) Text("Import a statement to begin.")
        }
        items(banks,key={it.first.key}) { display ->
            val bank=display.first; val json = display.second; val status = json.getString("status")
            ArchiCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${json.getString("date")} | ${json.getString("amount")} | $status")
                    Text("${json.getString("account")} | ${json.getString("description")} | ${json.getString("reference")}")
                    if (status == "UNMATCHED" && json.getString("amount").toBigDecimal().signum() > 0) TextButton(onClick = { selected = bank.key; invoiceId = null; tds = ""; reference = "" }) { Text("Match to invoice") }
                    if (status != "MATCHED") TextButton(onClick = { vm.action { vm.repo.ignore(bank.key) } }) { Text(if (status == "IGNORED") "Return to review" else "Ignore transaction") }
                }
            }
        }
    }
    selected?.let { key ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Review receipt and TDS") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose an issued invoice. Bank credit is the cash receipt. Add only the actual TDS withheld by the client; it remains unverified until checked against tax records.")
                invoices.filter { it.status == "ISSUED" }.forEach { invoice -> TextButton(onClick = { invoiceId = invoice.id }) { Text("${if(invoiceId == invoice.id) "Selected: " else ""}${invoice.number} | ${invoice.title} | total ${invoice.amount}") } }
                if (invoices.none { it.status == "ISSUED" }) Text("No issued invoices available.")
                invoiceId?.let { id -> invoices.firstOrNull { it.id == id }?.let { invoice ->
                    val remaining = InvoiceBalance.outstanding(invoice.amount, receipts.filter { it.invoiceId == id }.map { it.amount + it.tdsAmount })
                    val cash = rows.firstOrNull { it.key == key }?.let { JSONObject(it.json).getString("amount").toBigDecimal() } ?: java.math.BigDecimal.ZERO
                    Text("Outstanding $remaining | bank cash $cash")
                    runCatching { FinanceMath.decimal(tds.ifBlank { "0" }) }.getOrNull()?.let { Text("Balance after cash + TDS: ${remaining - cash - it}") }
                } }
                ArchiField(tds, { tds = it }, "Actual TDS amount (optional)")
                ArchiField(reference, { reference = it }, "TDS deduction advice / reference")
                FinanceNotice(vm)
            }
        }, confirmButton = { TextButton(enabled = invoiceId != null && !vm.busy, onClick = { val id = invoiceId!!; vm.action { vm.repo.match(key, id, tds, reference); withContext(Dispatchers.Main) { selected = null } } }) { Text("Post reviewed receipt") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel") } })
    }
}

@Composable
fun TaxInvoiceForm(projectId: Long, vm: FinanceViewModel = hiltViewModel()) {
    var title by rememberSaveable { mutableStateOf("") }; var base by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }; var sac by rememberSaveable { mutableStateOf("") }; var gstin by rememberSaveable { mutableStateOf("") }
    val records by vm.settings.collectAsStateWithLifecycle()
    val config = records.firstOrNull { it.key == "settings" }?.let { JSONObject(it.json) }
    val mode = config?.optString("mode") ?: "NONE"
    Text("New invoice | $mode", style = MaterialTheme.typography.titleMedium)
    ArchiField(title, { title = it }, "Invoice for"); ArchiField(base, { base = it }, "Professional fee before GST")
    if (mode != "NONE") { ArchiField(sac, { sac = it }, "Service SAC (six digits)"); ArchiField(gstin, { gstin = it.uppercase() }, "Client GSTIN (optional)") }
    if (mode == "REGULAR") {
        ArchiField(place, { place = it }, "Place-of-supply state code")
        Text("For property-related architectural services, confirm the property's location as the place of supply.")
    }
    val tax = runCatching { FinanceMath.tax(base, GstMode.valueOf(mode), config?.optString("rate") ?: "18", config?.optString("state").orEmpty(), place) }.getOrNull()
    tax?.let { Text("Fee ${it.base} | CGST ${it.cgst} | SGST ${it.sgst} | IGST ${it.igst} | Total ${it.total}") }
    FinanceNotice(vm)
    Button(enabled = !vm.busy && tax != null && title.isNotBlank(), onClick = { vm.action { vm.repo.createInvoice(projectId, title, base, place, sac, gstin) } }) { Text("Save invoice draft") }
}
@Composable
fun InvoiceTaxSummary(json: String) {
    if (json.isBlank()) return
    val value = JSONObject(json)
    Text(when(value.getString("mode")) { "COMPOSITION" -> "Bill of supply: composition taxable person, not eligible to collect tax on supplies."; "REGULAR" -> "Tax invoice | Fee ${value.getString("base")} | CGST ${value.getString("cgst")} | SGST ${value.getString("sgst")} | IGST ${value.getString("igst")} | Total ${value.getString("total")} | SAC ${value.optString("sac")}"; else -> "No GST charged" })
}
@Composable
fun VerifyTdsButton(receiptId: Long, vm: FinanceViewModel = hiltViewModel()) {
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick = { confirm = true }) { Text("Verify TDS credit") }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Confirm TDS verification") }, text = { Text("Have you checked the deduction against the client's certificate and your tax credit records? This app does not verify tax records automatically.") }, confirmButton = { TextButton(onClick = { vm.action { vm.repo.verifyTds(receiptId) }; confirm = false }) { Text("Mark verified") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}

@Composable
private fun FinanceNotice(vm: FinanceViewModel) {
    if (vm.busy) Text("Working…", modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
    if (vm.notice.isNotBlank()) com.holagundi.archiman.ui.theme.CarbonInlineNotification(if (vm.noticeKind == com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR) "Check your entries" else "Finance update", vm.notice, vm.noticeKind)
}
