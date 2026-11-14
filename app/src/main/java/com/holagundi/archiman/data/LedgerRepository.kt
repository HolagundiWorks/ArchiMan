package com.holagundi.archiman.data

import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.LedgerTrialRow
import com.holagundi.archiman.data.local.entity.LedgerAccountEntity
import com.holagundi.archiman.data.local.entity.LedgerJournalEntity
import com.holagundi.archiman.data.local.entity.LedgerJournalLineEntity
import com.holagundi.archiman.data.local.entity.LedgerPeriodEntity
import com.holagundi.archiman.domain.ChartOfAccounts
import com.holagundi.archiman.domain.LedgerLine
import com.holagundi.archiman.domain.LedgerRules
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class TrialBalance(
    val asOf: String,
    val rows: List<LedgerTrialRow>,
    val debitPaise: Long,
    val creditPaise: Long,
) {
    val balanced: Boolean get() = debitPaise == creditPaise
}

/** Creates the standard chart of accounts when missing: new practices, demo files and upgrades. */
object LedgerChart {
    suspend fun ensure(db: ArchiManDatabase) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            ChartOfAccounts.SEED.forEach { seed ->
                db.ledgerDao().insertAccountIgnore(
                    LedgerAccountEntity(code = seed.code, name = seed.name, type = seed.type, isSystem = true, active = true, createdAt = now),
                )
            }
        }
    }
}

/**
 * Double-entry posting, reversal and the trial balance. Rules follow MynaHub's ledger.rs.
 * Posted journals are never edited; corrections are reversal journals.
 */
@Singleton
class LedgerRepository @Inject constructor(private val db: ArchiManDatabase) {
    private val dao get() = db.ledgerDao()

    suspend fun accounts(): List<LedgerAccountEntity> = dao.accounts()

    suspend fun journals(from: String, to: String, limit: Int = 100): List<LedgerJournalEntity> {
        parseDate(from)
        parseDate(to)
        return dao.journalsBetween(from, to, limit)
    }

    suspend fun lines(journalId: Long): List<LedgerJournalLineEntity> = dao.lines(journalId)

    /** Posts one balanced journal. [sourceId] makes a business event post at most once. */
    suspend fun post(
        actorId: Long?,
        date: String,
        narration: String,
        lines: List<LedgerLine>,
        sourceType: String = "manual",
        sourceId: Long? = null,
    ): Long = db.withTransaction {
        val day = parseDate(date)
        require(narration.isNotBlank()) { "Enter a narration." }
        if (sourceId != null) require(dao.journalFor(sourceType, sourceId) == null) { "This entry has already been posted." }
        val activeById = lines.map { it.accountId }.distinct().associateWith { dao.account(it)?.active }
        LedgerRules.problem(lines) { id -> activeById[id] }?.let { throw IllegalArgumentException(it) }
        val fy = ChartOfAccounts.financialYear(date)
        val (start, end) = ChartOfAccounts.fyBounds(date)
        dao.insertPeriodIgnore(LedgerPeriodEntity(fy = fy, startDate = start, endDate = end, status = "open"))
        require(dao.closedPeriodCount(fy) == 0) { "The accounting period $fy is closed. Reopen it, or post in the current period." }
        val prefix = "JV-${day.year}-"
        val number = prefix + (dao.countNumbered("$prefix%") + 1).toString().padStart(4, '0')
        val journalId = dao.insertJournal(
            LedgerJournalEntity(
                number = number,
                date = date,
                narration = narration.trim(),
                sourceType = sourceType,
                sourceId = sourceId,
                createdBy = actorId,
                createdAt = System.currentTimeMillis(),
            ),
        )
        dao.insertLines(
            LedgerRules.keptLines(lines).map {
                LedgerJournalLineEntity(
                    journalId = journalId,
                    accountId = it.accountId,
                    debitPaise = it.debitPaise,
                    creditPaise = it.creditPaise,
                    memo = it.memo,
                    clientId = it.clientId,
                    projectId = it.projectId,
                )
            },
        )
        journalId
    }

    /** Cancels a journal by posting its mirror image. A journal can be reversed once. */
    suspend fun reverse(actorId: Long?, journalId: Long, reason: String): Long = db.withTransaction {
        require(reason.isNotBlank()) { "Give a reason." }
        val journal = requireNotNull(dao.journal(journalId)) { "Journal not found." }
        require(dao.journalFor("reversal", journalId) == null) { "This journal has already been reversed." }
        val original = dao.lines(journalId).map {
            LedgerLine(it.accountId, it.debitPaise, it.creditPaise, it.memo, it.clientId, it.projectId)
        }
        post(
            actorId = actorId,
            date = LocalDate.now().toString(),
            narration = "Reversal of ${journal.number}: ${reason.trim()}",
            lines = LedgerRules.mirror(original),
            sourceType = "reversal",
            sourceId = journalId,
        )
    }

    /** Totals of posted journals up to [asOf], one row per account. */
    suspend fun trialBalance(asOf: String): TrialBalance = db.withTransaction {
        parseDate(asOf)
        val rows = dao.trialRows(asOf)
        TrialBalance(asOf, rows, rows.sumOf { it.debitPaise }, rows.sumOf { it.creditPaise })
    }

    private fun parseDate(text: String): LocalDate =
        try {
            LocalDate.parse(text)
        } catch (_: Exception) {
            throw IllegalArgumentException("Enter the date as YYYY-MM-DD.")
        }
}
