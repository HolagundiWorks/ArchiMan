package com.holagundi.archiman.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.holagundi.archiman.data.local.entity.LedgerAccountEntity
import com.holagundi.archiman.data.local.entity.LedgerJournalEntity
import com.holagundi.archiman.data.local.entity.LedgerJournalLineEntity
import com.holagundi.archiman.data.local.entity.LedgerPeriodEntity

/** One account's totals up to a date, from posted journals only. */
data class LedgerTrialRow(
    val accountId: Long,
    val code: String,
    val name: String,
    val type: String,
    val debitPaise: Long,
    val creditPaise: Long,
)

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountIgnore(account: LedgerAccountEntity): Long

    @Query("SELECT * FROM ledger_accounts ORDER BY code")
    suspend fun accounts(): List<LedgerAccountEntity>

    @Query("SELECT * FROM ledger_accounts WHERE id = :id")
    suspend fun account(id: Long): LedgerAccountEntity?

    @Insert
    suspend fun insertJournal(journal: LedgerJournalEntity): Long

    @Insert
    suspend fun insertLines(lines: List<LedgerJournalLineEntity>)

    @Query("SELECT * FROM ledger_journals WHERE id = :id")
    suspend fun journal(id: Long): LedgerJournalEntity?

    @Query("SELECT * FROM ledger_journals WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun journalFor(sourceType: String, sourceId: Long): LedgerJournalEntity?

    @Query("SELECT * FROM ledger_journal_lines WHERE journalId = :journalId ORDER BY id")
    suspend fun lines(journalId: Long): List<LedgerJournalLineEntity>

    @Query("SELECT * FROM ledger_journals WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun journalsBetween(from: String, to: String, limit: Int): List<LedgerJournalEntity>

    @Query("SELECT COUNT(*) FROM ledger_journals WHERE number LIKE :prefix")
    suspend fun countNumbered(prefix: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPeriodIgnore(period: LedgerPeriodEntity): Long

    @Query("SELECT id FROM ledger_accounts WHERE code = :code")
    suspend fun accountIdByCode(code: String): Long?

    @Query("SELECT COUNT(*) FROM ledger_periods WHERE fy = :fy AND status = 'closed'")
    suspend fun closedPeriodCount(fy: String): Int

    @Query(
        """
        SELECT a.id AS accountId, a.code AS code, a.name AS name, a.type AS type,
               COALESCE(SUM(x.debitPaise), 0) AS debitPaise,
               COALESCE(SUM(x.creditPaise), 0) AS creditPaise
        FROM ledger_accounts a
        LEFT JOIN (
            SELECT l.accountId AS accountId, l.debitPaise AS debitPaise, l.creditPaise AS creditPaise
            FROM ledger_journal_lines l
            INNER JOIN ledger_journals j ON j.id = l.journalId
            WHERE j.date <= :asOf
        ) x ON x.accountId = a.id
        GROUP BY a.id
        ORDER BY a.code
        """,
    )
    suspend fun trialRows(asOf: String): List<LedgerTrialRow>
}
