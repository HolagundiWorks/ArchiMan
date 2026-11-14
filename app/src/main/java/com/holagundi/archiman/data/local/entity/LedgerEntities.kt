package com.holagundi.archiman.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** General-ledger account. Accounts are deactivated, never deleted. */
@Entity(
    tableName = "ledger_accounts",
    indices = [Index(value = ["code"], unique = true)],
)
data class LedgerAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val type: String,
    val isSystem: Boolean,
    val active: Boolean,
    val createdAt: Long,
)

/** A posted journal. Posted journals are never changed; a correction is a reversal journal. */
@Entity(
    tableName = "ledger_journals",
    indices = [
        Index(value = ["number"], unique = true),
        Index(value = ["date"]),
        // NULL source ids are distinct in SQLite, so manual journals can repeat.
        Index(value = ["sourceType", "sourceId"], unique = true),
    ],
)
data class LedgerJournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val date: String,
    val narration: String,
    val sourceType: String,
    val sourceId: Long?,
    val createdBy: Long?,
    val createdAt: Long,
)

@Entity(
    tableName = "ledger_journal_lines",
    indices = [Index(value = ["journalId"]), Index(value = ["accountId"])],
)
data class LedgerJournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journalId: Long,
    val accountId: Long,
    val debitPaise: Long,
    val creditPaise: Long,
    val memo: String,
    val clientId: Long?,
    val projectId: Long?,
)

/** A financial year. A closed period rejects new journals dated inside it. */
@Entity(
    tableName = "ledger_periods",
    indices = [Index(value = ["fy"], unique = true)],
)
data class LedgerPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fy: String,
    val startDate: String,
    val endDate: String,
    val status: String,
)
