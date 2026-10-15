package com.tend.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tend.app.data.LedgerEntry
import com.tend.app.data.CategoryBudget
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.LedgerRepository
import com.tend.app.data.TendDatabase
import com.tend.app.notifications.BudgetAlertNotifier
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TendViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()
    private val _copyPlanMessage = MutableStateFlow<String?>(null)
    val copyPlanMessage = _copyPlanMessage.asStateFlow()
    private val repository = LedgerRepository(TendDatabase.get(application))
    private val budgetAlertNotifier = BudgetAlertNotifier(application)

    val entries = selectedMonth.flatMapLatest { repository.observeMonth(it.toString()) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val categories = repository.observeCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val budgets = selectedMonth.flatMapLatest { repository.observeBudgets(it.toString()) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val monthlyBudget = selectedMonth.flatMapLatest { repository.observeMonthlyBudget(it.toString()) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val carryover = selectedMonth.flatMapLatest { repository.observeCarryover(it.toString()) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0L,
    )

    val recurringBills = repository.observeRecurringBills().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val pendingBillReminders = repository.observePendingBillReminders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun showMonth(month: YearMonth) {
        _selectedMonth.value = month
        _copyPlanMessage.value = null
    }

    fun addEntry(amountCents: Long, type: String, categoryId: String? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val monthKey = YearMonth.now().toString()
            repository.addEntry(
                LedgerEntry(
                    id = UUID.randomUUID().toString(),
                    amountCents = amountCents,
                    categoryId = categoryId,
                    type = type,
                    occurredAtEpochMillis = now,
                    monthKey = monthKey,
                ),
            )
            if (type == "expense" && categoryId != null) notifyBudgetAlerts(monthKey)
        }
    }

    fun addCategory(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.addCategory(ExpenseCategory(id = UUID.randomUUID().toString(), name = cleanName))
        }
    }

    fun renameCategory(category: ExpenseCategory, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch { repository.renameCategory(category.copy(name = cleanName)) }
    }

    fun deleteCategory(category: ExpenseCategory) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun updateEntry(entry: LedgerEntry, amountCents: Long, categoryId: String?) {
        viewModelScope.launch {
            repository.updateEntry(entry.copy(amountCents = amountCents, categoryId = categoryId))
            if (entry.type == "expense" && categoryId != null) notifyBudgetAlerts(entry.monthKey)
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun setCategoryBudget(categoryId: String, amountCents: Long) {
        viewModelScope.launch {
            val monthKey = selectedMonth.value.toString()
            repository.saveBudget(CategoryBudget(categoryId, monthKey, amountCents))
            notifyBudgetAlerts(monthKey)
        }
    }

    fun refreshBudgetAlerts() {
        viewModelScope.launch { notifyBudgetAlerts(selectedMonth.value.toString()) }
    }

    fun saveRecurringBill(
        id: String?,
        name: String,
        amountCents: Long,
        categoryId: String?,
        dayOfMonth: Int,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || amountCents <= 0L || dayOfMonth !in 1..31) return
        viewModelScope.launch {
            repository.saveRecurringBill(
                RecurringBill(
                    id = id ?: UUID.randomUUID().toString(),
                    name = cleanName,
                    amountCents = amountCents,
                    categoryId = categoryId,
                    dayOfMonth = dayOfMonth,
                ),
            )
        }
    }

    fun deleteRecurringBill(bill: RecurringBill) {
        viewModelScope.launch { repository.deleteRecurringBill(bill.id) }
    }

    fun approveBillReminder(reminder: BillReminderOccurrence) {
        viewModelScope.launch {
            repository.approveBillReminder(reminder.billId, reminder.monthKey)?.let { entry ->
                notifyBudgetAlerts(entry.monthKey)
            }
        }
    }

    fun skipBillReminder(reminder: BillReminderOccurrence) {
        viewModelScope.launch { repository.skipBillReminder(reminder.billId, reminder.monthKey) }
    }

    fun setMonthlyBudget(amountCents: Long) {
        viewModelScope.launch {
            repository.saveMonthlyBudget(MonthlyBudget(selectedMonth.value.toString(), amountCents))
        }
    }

    fun copyPreviousPlan() {
        viewModelScope.launch {
            val currentMonth = selectedMonth.value
            val copied = repository.copyPlan(currentMonth.minusMonths(1).toString(), currentMonth.toString())
            _copyPlanMessage.value = if (copied) "Skopiowano budżet z poprzedniego miesiąca." else "Brak planu do skopiowania albo budżet tego miesiąca już istnieje."
        }
    }

    private suspend fun notifyBudgetAlerts(monthKey: String) {
        if (monthKey != YearMonth.now().toString()) return
        if (!budgetAlertNotifier.canNotify()) return
        repository.collectBudgetAlerts(monthKey).forEach { event ->
            val categoryName = categories.value.firstOrNull { it.id == event.categoryId }?.name ?: "Kategoria"
            budgetAlertNotifier.notify(event, categoryName)
        }
    }
}
