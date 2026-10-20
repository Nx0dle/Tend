package com.tend.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.YearMonth

class LedgerRepository(private val database: TendDatabase) {
    private val dao = database.ledgerDao()
    fun observeMonth(monthKey: String): Flow<List<LedgerEntry>> = dao.observeMonth(monthKey)

    fun observeCategories(): Flow<List<ExpenseCategory>> = dao.observeCategories()

    fun observeBudgets(monthKey: String): Flow<List<CategoryBudget>> = dao.observeBudgets(monthKey)

    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudget?> = dao.observeMonthlyBudget(monthKey)

    fun observeCarryover(monthKey: String): Flow<Long> = combine(
        dao.observePlannedBefore(monthKey, YearMonth.now().toString()),
        dao.observeNetEntriesBefore(monthKey, YearMonth.now().toString()),
    ) { plannedBefore, netEntriesBefore -> plannedBefore + netEntriesBefore }

    suspend fun addEntry(entry: LedgerEntry) = dao.insert(entry)

    suspend fun addCategory(category: ExpenseCategory) = dao.insertCategory(category)

    suspend fun renameCategory(category: ExpenseCategory) = dao.updateCategory(category)

    suspend fun deleteCategory(category: ExpenseCategory) = database.withTransaction {
        dao.clearCategoryFromEntries(category.id)
        dao.clearCategoryFromRecurringBills(category.id)
        dao.deleteCategoryBudgets(category.id)
        dao.deleteCategoryBudgetAlerts(category.id)
        dao.deleteCategory(category)
    }

    suspend fun updateEntry(entry: LedgerEntry) = dao.update(entry)

    suspend fun deleteEntry(entry: LedgerEntry) = dao.delete(entry)

    suspend fun saveBudget(budget: CategoryBudget) = dao.saveBudget(budget)

    /** Setting a zero or negative limit clears it, so "no limit" is the absence of a row. */
    suspend fun setCategoryBudget(categoryId: String, monthKey: String, amountCents: Long) {
        if (amountCents <= 0L) {
            dao.deleteCategoryBudget(categoryId, monthKey)
        } else {
            dao.saveBudget(CategoryBudget(categoryId, monthKey, amountCents))
        }
    }

    suspend fun saveMonthlyBudget(budget: MonthlyBudget) = dao.saveMonthlyBudget(budget)

    suspend fun deleteMonthlyBudget(monthKey: String) = dao.deleteMonthlyBudget(monthKey)

    suspend fun copyPlan(sourceMonth: String, targetMonth: String): Boolean = database.withTransaction {
        if (dao.getBudgets(targetMonth).isNotEmpty()) return@withTransaction false
        val sourceBudgets = dao.getBudgets(sourceMonth)
        if (sourceBudgets.isEmpty()) return@withTransaction false
        dao.saveBudgets(sourceBudgets.map { it.copy(monthKey = targetMonth) })
        true
    }

    suspend fun collectBudgetAlerts(monthKey: String): List<BudgetAlertEvent> = database.withTransaction {
        buildList {
            for (budget in dao.getBudgets(monthKey)) {
                if (budget.amountCents <= 0L) continue
                val spent = dao.getCategoryExpenseTotal(budget.categoryId, monthKey)
                val previous = dao.getCategoryBudgetAlertState(budget.categoryId, monthKey)
                    ?: CategoryBudgetAlertState(budget.categoryId, monthKey)
                val warning = !previous.warningSent && spent.toDouble() / budget.amountCents >= 0.8
                val limit = !previous.limitSent && spent >= budget.amountCents
                if (warning || limit) {
                    dao.saveCategoryBudgetAlertState(
                        previous.copy(
                            warningSent = previous.warningSent || warning,
                            limitSent = previous.limitSent || limit,
                        ),
                    )
                    add(BudgetAlertEvent(budget.categoryId, monthKey, dao.getCategory(budget.categoryId)?.name ?: "Kategoria", warning, limit))
                }
            }
        }
    }

    fun observeRecurringBills(): Flow<List<RecurringBill>> = dao.observeRecurringBills()

    fun observePendingBillReminders(): Flow<List<BillReminderOccurrence>> = dao.observePendingBillReminders()

    suspend fun saveRecurringBill(bill: RecurringBill) = dao.saveRecurringBill(bill)

    suspend fun deleteRecurringBill(billId: String) = database.withTransaction {
        dao.deleteBillRemindersForBill(billId)
        dao.deleteRecurringBill(billId)
    }

    /**
     * Ensures the current month has one proposal per enabled bill, auto-skips stale
     * pending proposals from earlier months, and returns the proposals that owe a
     * single notification (pending, due or overdue, never notified this month).
     */
    suspend fun refreshBillOccurrences(today: LocalDate): List<DueBillReminder> = database.withTransaction {
        val monthKey = YearMonth.from(today).toString()
        dao.skipStalePendingBillReminders(monthKey)
        buildList {
            for (bill in dao.getEnabledRecurringBills()) {
                val dueDay = minOf(bill.dayOfMonth, today.lengthOfMonth())
                dao.insertBillReminder(BillReminderOccurrence(billId = bill.id, monthKey = monthKey))
                if (dueDay > today.dayOfMonth) continue
                val reminder = dao.getBillReminder(bill.id, monthKey) ?: continue
                if (reminder.status != BillReminderOccurrence.STATUS_PENDING) continue
                if (reminder.notifiedAtEpochMillis != null) continue
                dao.markBillReminderNotified(bill.id, monthKey, System.currentTimeMillis())
                add(DueBillReminder(bill, monthKey))
            }
        }
    }

    /** Writes the expense today with the corrected amount; the entry is idempotent per bill and month. */
    suspend fun approveBill(billId: String, amountCents: Long?): LedgerEntry? = database.withTransaction {
        val bill = dao.getRecurringBill(billId) ?: return@withTransaction null
        val monthKey = YearMonth.now().toString()
        dao.getBillReminder(billId, monthKey)
            ?.takeIf { it.status == BillReminderOccurrence.STATUS_PENDING }
            ?: return@withTransaction null
        val entry = LedgerEntry(
            id = "bill:${billId}:${monthKey}",
            amountCents = amountCents ?: bill.amountCents,
            categoryId = bill.categoryId,
            type = LedgerEntry.TYPE_EXPENSE,
            occurredAtEpochMillis = System.currentTimeMillis(),
            monthKey = monthKey,
        )
        dao.insert(entry)
        dao.updateBillReminderStatus(billId, monthKey, BillReminderOccurrence.STATUS_APPROVED)
        entry
    }

    suspend fun skipBill(billId: String) {
        dao.updateBillReminderStatus(billId, YearMonth.now().toString(), BillReminderOccurrence.STATUS_SKIPPED)
    }
}
