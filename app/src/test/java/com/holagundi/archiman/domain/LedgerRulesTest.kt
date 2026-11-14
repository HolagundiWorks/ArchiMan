package com.holagundi.archiman.domain

import org.junit.Assert.*
import org.junit.Test

class LedgerRulesTest {
    private val accounts = mapOf(1L to true, 2L to true, 3L to false)
    private fun active(id: Long): Boolean? = accounts[id]

    @Test fun balancedJournalIsAccepted() {
        val lines = listOf(LedgerLine(1, 100_00, 0), LedgerLine(2, 0, 100_00))
        assertNull(LedgerRules.problem(lines, ::active))
    }

    @Test fun unbalancedJournalReportsBothTotals() {
        val lines = listOf(LedgerLine(1, 100_00, 0), LedgerLine(2, 0, 99_50))
        assertEquals("The journal does not balance: debits 100.00 and credits 99.50.", LedgerRules.problem(lines, ::active))
    }

    @Test fun zeroLinesAreDroppedBeforeTheTwoLineRule() {
        val lines = listOf(LedgerLine(1, 100_00, 0), LedgerLine(2, 0, 0), LedgerLine(2, 0, 100_00))
        assertNull(LedgerRules.problem(lines, ::active))
        assertEquals("A journal needs at least two lines.", LedgerRules.problem(listOf(LedgerLine(1, 5_00, 0), LedgerLine(2, 0, 0)), ::active))
    }

    @Test fun lineCannotBeDebitAndCredit() {
        assertEquals("A line cannot be both a debit and a credit.", LedgerRules.problem(listOf(LedgerLine(1, 1_00, 1_00), LedgerLine(2, 5_00, 0)), ::active))
    }

    @Test fun unknownAndInactiveAccountsAreRejected() {
        assertEquals("A journal line uses an account that does not exist.", LedgerRules.problem(listOf(LedgerLine(9, 1_00, 0), LedgerLine(2, 0, 1_00)), ::active))
        assertEquals("A journal line uses an inactive account.", LedgerRules.problem(listOf(LedgerLine(3, 1_00, 0), LedgerLine(2, 0, 1_00)), ::active))
    }

    @Test fun negativeAmountsAreRejected() {
        assertEquals("Amounts cannot be negative.", LedgerRules.problem(listOf(LedgerLine(1, -1_00, 0), LedgerLine(2, 0, -1_00)), ::active))
    }

    @Test fun reversalMirrorsEveryLineAndCancelsTheJournal() {
        val original = listOf(LedgerLine(1, 250_00, 0, memo = "fee"), LedgerLine(2, 0, 250_00))
        val mirror = LedgerRules.mirror(original)
        assertEquals(listOf(0L, 250_00L), mirror.map { it.debitPaise })
        assertEquals(listOf(250_00L, 0L), mirror.map { it.creditPaise })
        assertEquals("fee", mirror[0].memo)
        assertNull(LedgerRules.problem(mirror, ::active))
    }

    @Test fun parsesAmountsToPaise() {
        assertEquals(123450L, LedgerRules.parsePaise("1,234.50"))
        assertEquals(100L, LedgerRules.parsePaise("1"))
        assertNull(LedgerRules.parsePaise("1.005"))
        assertNull(LedgerRules.parsePaise("-5"))
        assertNull(LedgerRules.parsePaise("abc"))
        assertNull(LedgerRules.parsePaise(""))
    }

    @Test fun financialYearMatchesMynaHub() {
        assertEquals("2026-27", ChartOfAccounts.financialYear("2026-11-10"))
        assertEquals("2025-26", ChartOfAccounts.financialYear("2026-03-31"))
        assertEquals("2026-04-01" to "2027-03-31", ChartOfAccounts.fyBounds("2026-04-01"))
    }

    @Test fun chartOfAccountsIsCompleteAndUniquelyCoded() {
        assertEquals(ChartOfAccounts.SEED.size, ChartOfAccounts.SEED.map { it.code }.toSet().size)
        assertEquals(35, ChartOfAccounts.SEED.size)
        assertTrue(ChartOfAccounts.SEED.all { it.type in setOf("asset", "liability", "equity", "income", "expense") })
    }
}
