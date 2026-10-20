package com.tend.app

import com.tend.app.data.BillProposal
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.CategoryBudget
import com.tend.app.data.LedgerEntry
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill

/**
 * Money totals shared by the Start (Przegląd) tab, the Budget (Plan) tab, and the watch snapshot.
 * Semantics: the reserve is the live pool remainder — every expense eats exactly one source:
 * a category consumes at least its limit, exceeded spending spills over, uncategorized
 * expenses draw straight from the reserve, and pending recurring-bill proposals
 * reserve their planned amount before approval.
 */
internal data class BudgetTotals(
    val availableCents: Long,
    val allocatedCents: Long,
    val spentCents: Long,
    val reserveCents: Long,
    val hasPool: Boolean,
)

internal fun computeBudgetTotals(
    entries: List<LedgerEntry>,
    budgets: List<CategoryBudget>,
    monthlyBudget: MonthlyBudget?,
    carryoverCents: Long,
    pendingBillsCents: Long,
): BudgetTotals {
    val spentCents = entries.asSequence().filter { it.type == LedgerEntry.TYPE_EXPENSE }.sumOf { it.amountCents }
    val incomeCents = entries.asSequence().filter { it.type == LedgerEntry.TYPE_INCOME }.sumOf { it.amountCents }
    val availableCents = (monthlyBudget?.plannedCents ?: 0L) + incomeCents + carryoverCents
    val uncategorizedCents = entries.asSequence()
        .filter { it.type == LedgerEntry.TYPE_EXPENSE && it.categoryId == null }
        .sumOf { it.amountCents }
    val spentPerCategory = entries.asSequence()
        .filter { it.type == LedgerEntry.TYPE_EXPENSE && it.categoryId != null }
        .groupBy { it.categoryId!! }
        .mapValues { (_, categoryEntries) -> categoryEntries.sumOf { it.amountCents } }
    val categoriesCovered = budgets.map { it.categoryId }.toSet() + spentPerCategory.keys
    val promisedCents = categoriesCovered.sumOf { categoryId ->
        val limit = budgets.firstOrNull { it.categoryId == categoryId }?.amountCents ?: 0L
        maxOf(limit, spentPerCategory[categoryId] ?: 0L)
    }
    return BudgetTotals(
        availableCents = availableCents,
        allocatedCents = budgets.sumOf { it.amountCents },
        spentCents = spentCents,
        reserveCents = availableCents - promisedCents - uncategorizedCents - pendingBillsCents,
        hasPool = monthlyBudget != null || incomeCents != 0L || carryoverCents != 0L,
    )
}

/** Planned amount still reserved by not-yet-handled bill proposals for [month]. */
internal fun pendingBillsCents(
    bills: List<RecurringBill>,
    proposals: List<BillProposal>,
    month: java.time.YearMonth,
): Long {
    val monthKey = month.toString()
    val amountsById = bills.associateBy({ it.id }, { it.amountCents })
    return proposals.asSequence()
        .filter { it.monthKey == monthKey && it.status == BillReminderOccurrence.STATUS_PENDING }
        .sumOf { amountsById[it.bill.id] ?: 0L }
}
