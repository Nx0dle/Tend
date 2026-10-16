package com.tend.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.CategoryBudget
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill
import java.time.YearMonth
import java.util.Locale

/**
 * Budget tab: monthly planning. Owns the plan-options, pool, category, and bill dialogs.
 * Read-only for past months; forward for future months.
 */
@Composable
internal fun BudgetTab(
    scrollState: ScrollState,
    month: YearMonth,
    canEditPlan: Boolean,
    isFuturePlan: Boolean,
    entries: List<LedgerEntry>,
    categories: List<ExpenseCategory>,
    budgets: List<CategoryBudget>,
    monthlyBudget: MonthlyBudget?,
    carryoverCents: Long,
    carryoverEnabled: Boolean,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onSetCategoryBudget: (String, Long) -> Unit,
    onSetMonthlyBudget: (Long) -> Unit,
    onClearMonthlyBudget: () -> Unit,
    onCopyPreviousPlan: () -> Unit,
    onSetCarryoverEnabled: (Boolean) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (ExpenseCategory, String) -> Unit,
    onDeleteCategory: (ExpenseCategory) -> Unit,
    recurringBills: List<RecurringBill>,
    pendingBillReminders: List<BillReminderOccurrence>,
    onSaveRecurringBill: (String?, String, Long, String?, Int) -> Unit,
    onDeleteRecurringBill: (RecurringBill) -> Unit,
    onApproveBillReminder: (BillReminderOccurrence) -> Unit,
    onSkipBillReminder: (BillReminderOccurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabel = polishMonthName(month)
    var showPlanOptions by remember { mutableStateOf(false) }
    var showMonthlyBudgetDialog by remember { mutableStateOf(false) }
    var showClearMonthlyBudgetConfirmation by remember { mutableStateOf(false) }
    var monthlyBudgetAmount by remember { mutableStateOf("") }
    var budgetCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var budgetAmount by remember { mutableStateOf("") }
    var showAddCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var deletingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showRecurringBillEditor by remember { mutableStateOf(false) }
    var editingRecurringBill by remember { mutableStateOf<RecurringBill?>(null) }
    var deletingRecurringBill by remember { mutableStateOf<RecurringBill?>(null) }

    val incomeCents = entries.asSequence().filter { it.type == LedgerEntry.TYPE_INCOME }.sumOf { it.amountCents }
    val availableCents = (monthlyBudget?.plannedCents ?: 0L) + incomeCents + carryoverCents
    val allocatedCents = budgets.sumOf { it.amountCents }
    val hasBudgetPool = monthlyBudget != null || incomeCents != 0L || carryoverCents != 0L
    val reserveCents = availableCents - allocatedCents

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = TendSpace.xlarge, vertical = TendSpace.medium),
    ) {
        Spacer(Modifier.height(TendSpace.small))
        TendSectionHeader(
            title = "Plan miesiąca",
            action = {
                if (canEditPlan) {
                    TextButton(onClick = { showPlanOptions = true }) {
                        Text("Opcje planu", maxLines = 1, softWrap = false)
                    }
                }
            },
        )
        if (!canEditPlan) {
            Spacer(Modifier.height(TendSpace.small))
            TendBadge("Tylko podgląd", color = Forest, containerColor = Sage)
        } else if (isFuturePlan) {
            Spacer(Modifier.height(TendSpace.small))
            TendBadge("Planowanie z wyprzedzeniem", color = Forest, containerColor = Sage)
        }
        if (canEditPlan && !isFuturePlan && !notificationsEnabled) {
            TextButton(onClick = onEnableNotifications) {
                Text("Włącz powiadomienia o limitach")
            }
        }
        Spacer(Modifier.height(TendSpace.medium))

        TendHeroCard(eyebrow = "DO DYSPOZYCJI") {
            HeroAmount(if (!hasBudgetPool) "Nieustawiona" else formatMoney(availableCents), compact = !hasBudgetPool)
            Spacer(Modifier.height(TendSpace.large))
            AllocationBar(availableCents = availableCents, allocatedCents = allocatedCents, reserveCents = reserveCents)
            Spacer(Modifier.height(TendSpace.large))
            HeroMetrics(
                listOf(
                    "LIMITY" to formatMoney(allocatedCents),
                    "REZERWA" to if (!hasBudgetPool && allocatedCents == 0L) "—" else formatMoney(reserveCents),
                ),
            )
        }

        Spacer(Modifier.height(TendSpace.xlarge))
        TendSectionHeader(
            title = "Limity kategorii",
            action = {
                if (canEditPlan) TextButton(onClick = { showAddCategory = true }) { Text("+ Kategoria", maxLines = 1, softWrap = false) }
            },
        )
        Spacer(Modifier.height(TendSpace.small))
        if (categories.isEmpty()) {
            Text("Brak kategorii.", style = TendType.caption, color = Muted)
        } else {
            categories.forEach { category ->
                val spent = entries.filter { it.categoryId == category.id && it.type == LedgerEntry.TYPE_EXPENSE }.sumOf { it.amountCents }
                val limit = budgets.firstOrNull { it.categoryId == category.id }?.amountCents ?: 0L
                if (!canEditPlan) {
                    CategoryRow(category = category, spentCents = spent, budgetCents = limit)
                } else {
                    CategoryRow(
                        category = category,
                        spentCents = spent,
                        budgetCents = limit,
                        isFuturePlan = isFuturePlan,
                        onEditBudget = {
                            budgetCategory = category
                            budgetAmount = if (limit == 0L) "" else String.format(Locale.forLanguageTag("pl-PL"), "%.2f", limit / 100.0)
                        },
                        onManageCategory = { editingCategory = category },
                    )
                }
            }
        }

        if (month == YearMonth.now()) {
            Spacer(Modifier.height(TendSpace.xxlarge))
            RecurringBillsSection(
                bills = recurringBills,
                pendingReminders = pendingBillReminders,
                categories = categories,
                onAddBill = {
                    editingRecurringBill = null
                    showRecurringBillEditor = true
                },
                onEditBill = { bill ->
                    editingRecurringBill = bill
                    showRecurringBillEditor = true
                },
                onDeleteBill = { deletingRecurringBill = it },
                onApproveReminder = onApproveBillReminder,
                onSkipReminder = onSkipBillReminder,
            )
        }
        Spacer(Modifier.height(TendSpace.xxlarge))
    }

    if (showPlanOptions) {
        BudgetPlanOptionsDialog(
            monthLabel = monthLabel,
            monthlyBudgetCents = monthlyBudget?.plannedCents,
            carryoverEnabled = carryoverEnabled,
            carryoverCents = carryoverCents,
            canCopyPreviousPlan = budgets.isEmpty(),
            onCarryoverChange = onSetCarryoverEnabled,
            onEditPool = {
                showPlanOptions = false
                monthlyBudgetAmount = monthlyBudget?.plannedCents?.let { String.format(Locale.forLanguageTag("pl-PL"), "%.2f", it / 100.0) } ?: ""
                showMonthlyBudgetDialog = true
            },
            onRemovePool = { showPlanOptions = false; showClearMonthlyBudgetConfirmation = true },
            onCopyPreviousPlan = { showPlanOptions = false; onCopyPreviousPlan() },
            onDismiss = { showPlanOptions = false },
        )
    }

    if (showMonthlyBudgetDialog) {
        TendDialog(
            onDismissRequest = { showMonthlyBudgetDialog = false },
            title = { Text("Pula · $monthLabel") },
            text = {
                OutlinedTextField(
                    value = monthlyBudgetAmount,
                    onValueChange = { value -> monthlyBudgetAmount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(TendRadius.control),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = parseAmountCents(monthlyBudgetAmount)
                    if (cents != null) {
                        onSetMonthlyBudget(cents)
                        showMonthlyBudgetDialog = false
                    }
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { showMonthlyBudgetDialog = false }) { Text("Anuluj") } },
        )
    }

    if (showClearMonthlyBudgetConfirmation) {
        TendDialog(
            onDismissRequest = { showClearMonthlyBudgetConfirmation = false },
            title = { Text("Usunąć pulę?") },
            text = {
                Text(
                    "Limity kategorii dla $monthLabel pozostaną bez zmian.",
                    style = TendType.controlLabel,
                    color = Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearMonthlyBudget()
                    showClearMonthlyBudgetConfirmation = false
                }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { showClearMonthlyBudgetConfirmation = false }) { Text("Anuluj") } },
        )
    }

    budgetCategory?.let { category ->
        TendDialog(
            onDismissRequest = { budgetCategory = null },
            title = { Text("Budżet: ${category.name}") },
            text = {
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { value -> budgetAmount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Miesięczny limit") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(TendRadius.control),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = parseAmountCents(budgetAmount)
                    if (cents != null) {
                        onSetCategoryBudget(category.id, cents)
                        budgetCategory = null
                    }
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { budgetCategory = null }) { Text("Anuluj") } },
        )
    }

    if (showAddCategory) {
        AddCategoryDialog(onDismiss = { showAddCategory = false }, onAdd = onAddCategory)
    }

    editingCategory?.let { category ->
        CategoryEditorDialog(
            category = category,
            onDismiss = { editingCategory = null },
            onSave = { edited, name ->
                onRenameCategory(edited, name)
                editingCategory = null
            },
            onDelete = {
                deletingCategory = category
                editingCategory = null
            },
        )
    }

    deletingCategory?.let { category ->
        TendDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Usunąć kategorię?") },
            text = { Text("Wpisy z kategorii „${category.name}” pozostaną w historii jako bez kategorii.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(category)
                    deletingCategory = null
                }) { Text("Usuń kategorię") }
            },
            dismissButton = { TextButton(onClick = { deletingCategory = null }) { Text("Anuluj") } },
        )
    }

    if (showRecurringBillEditor) {
        RecurringBillEditorDialog(
            bill = editingRecurringBill,
            categories = categories,
            onDismiss = { showRecurringBillEditor = false },
            onSave = { name, amountCents, categoryId, dayOfMonth ->
                onSaveRecurringBill(editingRecurringBill?.id, name, amountCents, categoryId, dayOfMonth)
                showRecurringBillEditor = false
            },
        )
    }

    deletingRecurringBill?.let { bill ->
        TendDialog(
            onDismissRequest = { deletingRecurringBill = null },
            title = { Text("Usunąć rachunek cykliczny?") },
            text = { Text("Wyłączyć przyszłe przypomnienia dla „${bill.name}”?") },
            confirmButton = {
                TextButton(onClick = { onDeleteRecurringBill(bill); deletingRecurringBill = null }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { deletingRecurringBill = null }) { Text("Anuluj") } },
        )
    }
}
