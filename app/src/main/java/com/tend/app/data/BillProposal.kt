package com.tend.app.data

/** A recurring bill joined with one of its month proposals, as shown in the UI. */
data class BillProposal(
    val bill: RecurringBill,
    val monthKey: String,
    val status: String,
) {
    /** Due date clamped to the last day of the month (day 31 → Feb 28/29). */
    fun dueDate(month: java.time.YearMonth): java.time.LocalDate =
        month.atDay(minOf(bill.dayOfMonth, month.lengthOfMonth()))
}

/** A pending bill due (or overdue) today, offered for notification. */
data class DueBillReminder(
    val bill: RecurringBill,
    val monthKey: String,
)
