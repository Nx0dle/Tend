package com.tend.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_entries",
    indices = [Index(value = ["monthKey", "occurredAtEpochMillis"])],
)
data class LedgerEntry(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val categoryId: String? = null,
    val type: String,
    val occurredAtEpochMillis: Long,
    val monthKey: String,
)
