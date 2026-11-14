package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.data.LedgerRepository
import com.holagundi.archiman.data.TrialBalance
import com.holagundi.archiman.data.local.entity.LedgerJournalEntity
import com.holagundi.archiman.data.local.entity.LedgerJournalLineEntity
import com.holagundi.archiman.domain.ChartOfAccounts
import com.holagundi.archiman.domain.LedgerLine
import com.holagundi.archiman.domain.LedgerRules
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.CarbonInlineNotification
import com.holagundi.archiman.ui.theme.CarbonNoticeKind
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Books: trial balance, this year's journals and manual two-line journals. Rules are the ledger's (see LedgerRepository). */
@HiltViewModel
class LedgerViewModel @Inject constructor(private val ledger: LedgerRepository) : ViewModel() {
    var asOf by mutableStateOf(LocalDate.now().toString())
    var trial by mutableStateOf<TrialBalance?>(null); private set
    var journals by mutableStateOf<List<LedgerJournalEntity>>(emptyList()); private set
    var notice by mutableStateOf<String?>(null); private set
    var noticeKind by mutableStateOf(CarbonNoticeKind.INFO); private set
    var busy by mutableStateOf(false); private set
    val details = mutableStateMapOf<Long, List<LedgerJournalLineEntity>>()
    var openJournal by mutableStateOf<Long?>(null); private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                val (start, end) = ChartOfAccounts.fyBounds(asOf)
                trial = ledger.trialBalance(asOf)
                journals = ledger.journals(start, end)
            } catch (failure: IllegalArgumentException) {
                report(failure.message ?: "Could not load the books.", CarbonNoticeKind.ERROR)
            }
        }
    }

    fun toggleDetails(id: Long) {
        if (openJournal == id) { openJournal = null; return }
        openJournal = id
        if (details[id] == null) viewModelScope.launch { details[id] = withContext(Dispatchers.IO) { ledger.lines(id) } }
    }

    fun post(date: String, narration: String, debitCode: String, creditCode: String, amount: String) = action("Journal posted.") {
        val accounts = ledger.accounts().associateBy { it.code }
        val debit = requireNotNull(accounts[debitCode.trim()]) { "Account code ${debitCode.trim()} is not in the chart." }
        val credit = requireNotNull(accounts[creditCode.trim()]) { "Account code ${creditCode.trim()} is not in the chart." }
        val paise = requireNotNull(LedgerRules.parsePaise(amount)) { "Enter the amount in rupees, for example 1500.00." }
        require(paise > 0) { "Enter an amount above zero." }
        ledger.post(null, date.trim(), narration, listOf(LedgerLine(debit.id, paise, 0), LedgerLine(credit.id, 0, paise)))
    }

    fun reverse(journal: LedgerJournalEntity, reason: String) = action("Journal ${journal.number} reversed.") {
        ledger.reverse(null, journal.id, reason)
    }

    /** Runs one books change off the UI thread; a rejected entry leaves the books untouched and shows why. */
    private fun action(success: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                report(success, CarbonNoticeKind.SUCCESS)
                refresh()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                report(failure.message ?: "Could not save this entry.", CarbonNoticeKind.ERROR)
            } finally {
                busy = false
            }
        }
    }

    private fun report(message: String, kind: CarbonNoticeKind) {
        notice = message
        noticeKind = kind
    }
}

@Composable
fun LedgerPane(vm: LedgerViewModel = hiltViewModel()) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var narration by remember { mutableStateOf("") }
    var debitCode by remember { mutableStateOf("") }
    var creditCode by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Books and ledger", style = MaterialTheme.typography.titleLarge)
            Text("Every posted journal is final. A correction is a reversal.", style = MaterialTheme.typography.bodyMedium)
        }
        vm.notice?.let { message ->
            item { CarbonInlineNotification(title = "Books", body = message, kind = vm.noticeKind) }
        }
        item {
            Text("Trial balance", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ArchiField(vm.asOf, { vm.asOf = it }, "As at (YYYY-MM-DD)", Modifier.fillMaxWidth())
            }
            OutlinedButton(onClick = { vm.refresh() }) { Text("Show trial balance") }
        }
        vm.trial?.let { tb ->
            item {
                Text("Debits ${LedgerRules.formatPaise(tb.debitPaise)} · Credits ${LedgerRules.formatPaise(tb.creditPaise)}")
                Text(if (tb.balanced) "Balanced" else "Not balanced", color = if (tb.balanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            items(tb.rows.filter { it.debitPaise != 0L || it.creditPaise != 0L }, key = { it.accountId }) { row ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${row.code} ${row.name}", Modifier.weight(1f))
                    Text(LedgerRules.formatPaise(row.debitPaise))
                    Text("  ${LedgerRules.formatPaise(row.creditPaise)}")
                }
            }
        }
        item {
            Text("Post a journal", style = MaterialTheme.typography.titleMedium)
            ArchiField(date, { date = it }, "Date (YYYY-MM-DD)")
            ArchiField(narration, { narration = it }, "Narration")
            ArchiField(debitCode, { debitCode = it }, "Debit account code (for example 5300)")
            ArchiField(creditCode, { creditCode = it }, "Credit account code (for example 1010)")
            ArchiField(amount, { amount = it }, "Amount in rupees")
            Button(onClick = {
                vm.post(date, narration, debitCode, creditCode, amount)
                narration = ""; amount = ""
            }) { Text("Post journal") }
        }
        item { Text("Journals this financial year", style = MaterialTheme.typography.titleMedium) }
        items(vm.journals, key = { it.id }) { journal ->
            Column(Modifier.fillMaxWidth()) {
                Text("${journal.number} · ${journal.date} · ${journal.narration}")
                Text(journal.sourceType, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { vm.toggleDetails(journal.id) }) {
                    Text(if (vm.openJournal == journal.id) "Hide lines" else "Show lines")
                }
                if (vm.openJournal == journal.id) {
                    vm.details[journal.id]?.forEach { line ->
                        Text("  account ${line.accountId}  Dr ${LedgerRules.formatPaise(line.debitPaise)}  Cr ${LedgerRules.formatPaise(line.creditPaise)}")
                    }
                    if (journal.sourceType != "reversal") {
                        ArchiField(reason, { reason = it }, "Reason for reversal")
                        Button(onClick = { vm.reverse(journal, reason) }, enabled = reason.isNotBlank()) { Text("Reverse this journal") }
                    }
                }
            }
        }
    }
}
