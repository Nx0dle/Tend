package com.tend.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tend.app.data.LedgerEntry
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.LedgerRepository
import com.tend.app.data.TendDatabase
import com.tend.app.notifications.BudgetAlertNotifier
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LedgerRepository(TendDatabase.get(application))
    private val budgetAlertNotifier = BudgetAlertNotifier(application)
    private val preferences = application.getSharedPreferences("tend_settings", Context.MODE_PRIVATE)

    private val _carryoverEnabled = MutableStateFlow(preferences.getBoolean("carryover_enabled", false))
    val carryoverEnabled = _carryoverEnabled.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    val currentEntries = currentMonthFlow(repository::observeMonth, emptyList())
    val currentBudgets = currentMonthFlow(repository::observeBudgets, emptyList())
    val currentMonthlyBudget = currentMonthFlow(repository::observeMonthlyBudget, null)
    val currentCarryover = carryoverFlow(_currentMonth)

    val entries = selectedMonthFlow(repository::observeMonth, emptyList())
    val budgets = selectedMonthFlow(repository::observeBudgets, emptyList())
    val monthlyBudget = selectedMonthFlow(repository::observeMonthlyBudget, null)
    val carryover = carryoverFlow(_selectedMonth)

    val categories = repository.observeCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
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

    private fun <T> selectedMonthFlow(selector: (String) -> Flow<T>, initialValue: T): StateFlow<T> =
        _selectedMonth.flatMapLatest { selector(it.toString()) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialValue,
        )

    private fun <T> currentMonthFlow(selector: (String) -> Flow<T>, initialValue: T): StateFlow<T> =
        _currentMonth.flatMapLatest { selector(it.toString()) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialValue,
        )

    private fun carryoverFlow(month: StateFlow<YearMonth>): StateFlow<Long> =
        combine(month, carryoverEnabled) { m, enabled -> m to enabled }
            .flatMapLatest { (m, enabled) -> if (enabled) repository.observeCarryover(m.toString()) else flowOf(0L) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0L,
            )

    /** Runs a write and surfaces a single message if it fails, instead of failing silently. */
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _userMessage.value = "Nie udało się zapisać zmian."
            }
        }
    }

    fun showMonth(month: YearMonth) {
        _selectedMonth.value = month
        _userMessage.value = null
    }

    fun dismissMessage() {
        _userMessage.value = null
    }

    fun refreshCurrentMonth() {
        val now = YearMonth.now()
        if (_currentMonth.value != now) _currentMonth.value = now
    }

    fun addEntry(amountCents: Long, type: String, categoryId: String? = null) {
        write {
            val monthKey = YearMonth.now().toString()
            repository.addEntry(
                LedgerEntry(
                    id = UUID.randomUUID().toString(),
                    amountCents = amountCents,
                    categoryId = categoryId,
                    type = type,
                    occurredAtEpochMillis = System.currentTimeMillis(),
                    monthKey = monthKey,
                ),
            )
            if (type == LedgerEntry.TYPE_EXPENSE && categoryId != null) notifyBudgetAlerts(monthKey)
        }
    }

    fun addCategory(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        write { repository.addCategory(ExpenseCategory(id = UUID.randomUUID().toString(), name = cleanName)) }
    }

    fun renameCategory(category: ExpenseCategory, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        write { repository.renameCategory(category.copy(name = cleanName)) }
    }

    fun deleteCategory(category: ExpenseCategory) {
        write { repository.deleteCategory(category) }
    }

    fun updateEntry(entry: LedgerEntry, amountCents: Long, categoryId: String?) {
        write {
            repository.updateEntry(entry.copy(amountCents = amountCents, categoryId = categoryId))
            if (entry.type == LedgerEntry.TYPE_EXPENSE && categoryId != null) notifyBudgetAlerts(entry.monthKey)
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        write { repository.deleteEntry(entry) }
    }

    fun setCategoryBudget(categoryId: String, amountCents: Long) {
        write {
            val monthKey = selectedMonth.value.toString()
            repository.setCategoryBudget(categoryId, monthKey, amountCents)
            notifyBudgetAlerts(monthKey)
        }
    }

    fun refreshBudgetAlerts() {
        viewModelScope.launch { notifyBudgetAlerts(YearMonth.now().toString()) }
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
        write {
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
        write { repository.deleteRecurringBill(bill.id) }
    }

    fun approveBillReminder(reminder: BillReminderOccurrence) {
        write {
            repository.approveBillReminder(reminder.billId, reminder.monthKey)?.let { entry ->
                notifyBudgetAlerts(entry.monthKey)
            }
        }
    }

    fun skipBillReminder(reminder: BillReminderOccurrence) {
        write { repository.skipBillReminder(reminder.billId, reminder.monthKey) }
    }

    /** A zero or negative pool clears the pool row. */
    fun setMonthlyBudget(amountCents: Long) {
        write {
            val monthKey = selectedMonth.value.toString()
            if (amountCents <= 0L) repository.deleteMonthlyBudget(monthKey)
            else repository.saveMonthlyBudget(MonthlyBudget(monthKey, amountCents))
        }
    }

    fun clearMonthlyBudget() {
        write { repository.deleteMonthlyBudget(selectedMonth.value.toString()) }
    }

    fun setCarryoverEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("carryover_enabled", enabled).apply()
        _carryoverEnabled.value = enabled
    }

    fun copyPreviousPlan() {
        write {
            val currentMonth = selectedMonth.value
            val copied = repository.copyPlan(currentMonth.minusMonths(1).toString(), currentMonth.toString())
            _userMessage.value = if (copied) {
                "Skopiowano limity z poprzedniego miesiąca."
            } else {
                "W poprzednim miesiącu nie ma limitów do skopiowania."
            }
        }
    }

    private suspend fun notifyBudgetAlerts(monthKey: String) {
        if (monthKey != YearMonth.now().toString()) return
        if (!budgetAlertNotifier.canNotify()) return
        repository.collectBudgetAlerts(monthKey).forEach(budgetAlertNotifier::notify)
    }
}

