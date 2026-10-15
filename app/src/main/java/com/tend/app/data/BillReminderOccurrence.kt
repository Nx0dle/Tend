package com.tend.app.data

import androidx.room.Entity

@Entity(tableName = "bill_reminders", primaryKeys = ["billId", "monthKey"])
data class BillReminderOccurrence(
    val billId: String,
    val monthKey: String,
    val name: String,
    val amountCents: Long,
    val categoryId: String?,
    val dueAtEpochMillis: Long,
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_SKIPPED = "skipped"
    }
}
