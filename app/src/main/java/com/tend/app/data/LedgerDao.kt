package com.tend.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE monthKey = :monthKey ORDER BY occurredAtEpochMillis DESC")
    fun observeMonth(monthKey: String): Flow<List<LedgerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LedgerEntry)

    @Update
    suspend fun update(entry: LedgerEntry)

    @Delete
    suspend fun delete(entry: LedgerEntry)

    @Query("SELECT * FROM expense_categories ORDER BY name COLLATE NOCASE")
    fun observeCategories(): Flow<List<ExpenseCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: ExpenseCategory)

    @Update
    suspend fun updateCategory(category: ExpenseCategory)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategory)

    @Query("UPDATE ledger_entries SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromEntries(categoryId: String)

    @Query("DELETE FROM category_budgets WHERE categoryId = :categoryId")
    suspend fun deleteCategoryBudgets(categoryId: String)

    @Query("DELETE FROM category_budget_alerts WHERE categoryId = :categoryId")
    suspend fun deleteCategoryBudgetAlerts(categoryId: String)

    @Query("UPDATE recurring_bills SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromRecurringBills(categoryId: String)

    @Query("UPDATE bill_reminders SET categoryId = NULL WHERE categoryId = :categoryId AND status = 'pending'")
    suspend fun clearCategoryFromPendingBillReminders(categoryId: String)

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey")
    fun observeBudgets(monthKey: String): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBudget(budget: CategoryBudget)

    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMonthlyBudget(budget: MonthlyBudget)

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey")
    suspend fun getBudgets(monthKey: String): List<CategoryBudget>

    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getMonthlyBudget(monthKey: String): MonthlyBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBudgets(budgets: List<CategoryBudget>)

    @Query("SELECT COALESCE(SUM(plannedCents), 0) FROM monthly_budgets WHERE monthKey < :monthKey")
    fun observePlannedBefore(monthKey: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amountCents ELSE -amountCents END), 0) FROM ledger_entries WHERE monthKey < :monthKey")
    fun observeNetEntriesBefore(monthKey: String): Flow<Long>

    @Query("SELECT * FROM category_budgets WHERE categoryId = :categoryId AND monthKey = :monthKey LIMIT 1")
    suspend fun getCategoryBudget(categoryId: String, monthKey: String): CategoryBudget?

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM ledger_entries WHERE categoryId = :categoryId AND monthKey = :monthKey AND type = 'expense'")
    suspend fun getCategoryExpenseTotal(categoryId: String, monthKey: String): Long

    @Query("SELECT * FROM category_budget_alerts WHERE categoryId = :categoryId AND monthKey = :monthKey LIMIT 1")
    suspend fun getCategoryBudgetAlertState(categoryId: String, monthKey: String): CategoryBudgetAlertState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategoryBudgetAlertState(state: CategoryBudgetAlertState)

    @Query("SELECT * FROM recurring_bills ORDER BY dayOfMonth, name COLLATE NOCASE")
    fun observeRecurringBills(): Flow<List<RecurringBill>>

    @Query("SELECT * FROM recurring_bills WHERE enabled = 1")
    suspend fun getEnabledRecurringBills(): List<RecurringBill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecurringBill(bill: RecurringBill)

    @Query("DELETE FROM recurring_bills WHERE id = :billId")
    suspend fun deleteRecurringBill(billId: String)

    @Query("SELECT * FROM bill_reminders WHERE status = 'pending' ORDER BY monthKey DESC, name COLLATE NOCASE")
    fun observePendingBillReminders(): Flow<List<BillReminderOccurrence>>

    @Query("SELECT * FROM bill_reminders WHERE billId = :billId AND monthKey = :monthKey LIMIT 1")
    suspend fun getBillReminder(billId: String, monthKey: String): BillReminderOccurrence?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBillReminder(reminder: BillReminderOccurrence): Long

    @Query("UPDATE bill_reminders SET status = :status WHERE billId = :billId AND monthKey = :monthKey AND status = 'pending'")
    suspend fun updateBillReminderStatus(billId: String, monthKey: String, status: String)
}
