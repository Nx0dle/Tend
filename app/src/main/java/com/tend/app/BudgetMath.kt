package com.tend.app

import com.tend.app.data.CategoryBudget
import com.tend.app.data.LedgerEntry
import com.tend.app.data.MonthlyBudget

/**
 * Money totals shared by the Start (Przegląd) tab, the Budget (Plan) tab, and the watch snapshot.
 * Semantics: the reserve is the live pool remainder — every expense eats exactly one source:
 * a category consumes at least its limit, exceeded spending spills over, and uncategorized
 * expenses draw straight from the reserve.
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
        reserveCents = availableCents - promisedCents - uncategorizedCents,
        hasPool = monthlyBudget != null || incomeCents != 0L || carryoverCents != 0L,
    )
}
