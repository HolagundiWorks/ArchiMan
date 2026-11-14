package com.holagundi.archiman.domain

import java.math.BigDecimal

/**
 * Double-entry rules ported from MynaHub's ledger (src-tauri/src/ledger.rs `post`).
 * Money is integer paise. A posted journal is immutable; a correction is a reversal.
 */
data class LedgerLine(
    val accountId: Long,
    val debitPaise: Long,
    val creditPaise: Long,
    val memo: String = "",
    val clientId: Long? = null,
    val projectId: Long? = null,
)

object LedgerRules {
    /** Zero-amount lines are dropped before the rules run, as in MynaHub. */
    fun keptLines(lines: List<LedgerLine>): List<LedgerLine> = lines.filter { it.debitPaise > 0 || it.creditPaise > 0 }

    /**
     * Returns the first problem with a journal, or null when it may be posted.
     * [activeAccount] returns null for an unknown account, otherwise whether it is active.
     */
    fun problem(lines: List<LedgerLine>, activeAccount: (Long) -> Boolean?): String? {
        if (lines.any { it.debitPaise < 0 || it.creditPaise < 0 }) return "Amounts cannot be negative."
        val kept = keptLines(lines)
        if (kept.size < 2) return "A journal needs at least two lines."
        if (kept.any { it.debitPaise > 0 && it.creditPaise > 0 }) return "A line cannot be both a debit and a credit."
        val debits = kept.sumOf { it.debitPaise }
        val credits = kept.sumOf { it.creditPaise }
        if (debits != credits) return "The journal does not balance: debits ${formatPaise(debits)} and credits ${formatPaise(credits)}."
        for (line in kept) {
            when (activeAccount(line.accountId)) {
                null -> return "A journal line uses an account that does not exist."
                false -> return "A journal line uses an inactive account."
                true -> Unit
            }
        }
        return null
    }

    /** The mirror image that cancels a journal. */
    fun mirror(lines: List<LedgerLine>): List<LedgerLine> =
        lines.map { it.copy(debitPaise = it.creditPaise, creditPaise = it.debitPaise) }

    /** "1234.5" or "1,234.50" to paise. Returns null when the text is not a non-negative amount with at most two decimals. */
    fun parsePaise(text: String): Long? {
        val cleaned = text.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val amount = cleaned.toBigDecimalOrNull() ?: return null
        if (amount.signum() < 0 || amount.scale() > 2) return null
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.UNNECESSARY).longValueExact()
    }

    fun formatPaise(paise: Long): String = BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
}

/** The standard chart of accounts, copied from MynaHub's 0011_ledger.sql. Codes, names and types match. */
object ChartOfAccounts {
    data class Seed(val code: String, val name: String, val type: String)

    val SEED: List<Seed> = listOf(
        Seed("1000", "Cash in hand", "asset"),
        Seed("1010", "Bank (unassigned)", "asset"),
        Seed("1200", "Accounts receivable", "asset"),
        Seed("1210", "TDS receivable (deducted by clients)", "asset"),
        Seed("1220", "GST input credit", "asset"),
        Seed("1300", "Fixed assets (at cost)", "asset"),
        Seed("1310", "Accumulated depreciation", "asset"),
        Seed("2000", "Accounts payable", "liability"),
        Seed("2100", "GST output - CGST", "liability"),
        Seed("2110", "GST output - SGST", "liability"),
        Seed("2120", "GST output - IGST", "liability"),
        Seed("2130", "GST composition tax payable", "liability"),
        Seed("2200", "TDS payable", "liability"),
        Seed("2300", "Salaries payable", "liability"),
        Seed("2400", "Advances from clients", "liability"),
        Seed("2500", "Income tax payable", "liability"),
        Seed("3000", "Owner capital", "equity"),
        Seed("3100", "Retained earnings", "equity"),
        Seed("4000", "Professional fees", "income"),
        Seed("4100", "Rental income - commercial", "income"),
        Seed("4110", "Rental income - residential", "income"),
        Seed("4200", "Bank interest income", "income"),
        Seed("4900", "Other income", "income"),
        Seed("5000", "Salaries and wages", "expense"),
        Seed("5100", "Office rent", "expense"),
        Seed("5200", "Consultant and professional charges", "expense"),
        Seed("5300", "Travel and conveyance", "expense"),
        Seed("5400", "Printing and stationery", "expense"),
        Seed("5500", "Utilities and internet", "expense"),
        Seed("5600", "Software and subscriptions", "expense"),
        Seed("5700", "Depreciation", "expense"),
        Seed("5800", "Bank charges", "expense"),
        Seed("5900", "Income tax expense", "expense"),
        Seed("5950", "GST composition tax expense", "expense"),
        Seed("5990", "Other expenses", "expense"),
    )

    /** Indian financial year label, e.g. 2026-11-10 is "2026-27" (April to March), as in MynaHub. */
    fun financialYear(date: String): String {
        val year = date.substring(0, 4).toInt()
        val month = date.substring(5, 7).toInt()
        val start = if (month >= 4) year else year - 1
        return "$start-${((start + 1) % 100).toString().padStart(2, '0')}"
    }

    /** First and last day of the financial year that contains [date]. */
    fun fyBounds(date: String): Pair<String, String> {
        val start = financialYear(date).substring(0, 4).toInt()
        return "$start-04-01" to "${start + 1}-03-31"
    }
}
