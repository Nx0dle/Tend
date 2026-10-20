package com.tend.app.data

import androidx.room.Entity

/**
 * A bill month proposal. Pure link: reads the name, amount, category and due day
 * live from [RecurringBill], so editing the definition updates every pending
 * proposal, the calendar, and the reserve at once.
 */
@Entity(tableName = "bill_reminders", primaryKeys = ["billId", "monthKey"])
data class BillReminderOccurrence(
    val billId: String,
    val monthKey: String,
    val status: String = STATUS_PENDING,
    val notifiedAtEpochMillis: Long? = null,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_SKIPPED = "skipped"
    }
}
