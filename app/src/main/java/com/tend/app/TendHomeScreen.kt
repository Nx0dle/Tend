package com.tend.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.CategoryBudget
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill
import java.time.YearMonth
import kotlinx.coroutines.delay

/**
 * App shell: shared header, month navigation, tab content host, and bottom navigation.
 * Screen-specific state and dialogs live in [StartTab] and [BudgetTab]; entry editing dialogs
 * stay here because both Start and Calendar open them.
 */
@Composable
internal fun TendHomeScreen(
    entries: List<LedgerEntry>,
    currentEntries: List<LedgerEntry>,
    categories: List<ExpenseCategory>,
    budgets: List<CategoryBudget>,
    currentBudgets: List<CategoryBudget>,
    monthlyBudget: MonthlyBudget?,
    currentMonthlyBudget: MonthlyBudget?,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    userMessage: String?,
    onDismissMessage: () -> Unit,
    onAddEntry: (Long, String, String?) -> Unit,
    onAddCategory: (String) -> Unit,
    onSetCategoryBudget: (String, Long) -> Unit,
    onSetMonthlyBudget: (Long) -> Unit,
    onClearMonthlyBudget: () -> Unit,
    onCopyPreviousPlan: () -> Unit,
    carryoverCents: Long,
    currentCarryoverCents: Long,
    carryoverEnabled: Boolean,
    onSetCarryoverEnabled: (Boolean) -> Unit,
    onRenameCategory: (ExpenseCategory, String) -> Unit,
    onDeleteCategory: (ExpenseCategory) -> Unit,
    onUpdateEntry: (LedgerEntry, Long, String?) -> Unit,
    onDeleteEntry: (LedgerEntry) -> Unit,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    openBillsTab: Boolean,
    recurringBills: List<RecurringBill>,
    pendingBillReminders: List<BillReminderOccurrence>,
    onSaveRecurringBill: (String?, String, Long, String?, Int) -> Unit,
    onDeleteRecurringBill: (RecurringBill) -> Unit,
    onApproveBillReminder: (BillReminderOccurrence) -> Unit,
    onSkipBillReminder: (BillReminderOccurrence) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(if (openBillsTab) 2 else 0) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var budgetMonth by remember { mutableStateOf(selectedMonth) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    val startScroll = rememberScrollState()
    val calendarScroll = rememberScrollState()
    val budgetScroll = rememberScrollState()

    val currentMonth = YearMonth.now()
    val displayMonth = when (selectedTab) {
        0 -> currentMonth
        1 -> calendarMonth
        else -> budgetMonth
    }
    val changeDisplayedMonth: (YearMonth) -> Unit = { month ->
        if (selectedTab == 1) calendarMonth = month
        if (selectedTab == 2) budgetMonth = month
        onMonthChange(month)
    }
    LaunchedEffect(selectedTab) {
        val targetMonth = when (selectedTab) {
            1 -> calendarMonth
            2 -> budgetMonth
            else -> null
        }
        if (targetMonth != null && targetMonth != selectedMonth) onMonthChange(targetMonth)
    }

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            delay(4000)
            onDismissMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Porcelain)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TendSpace.xlarge).height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(TendRadius.small)).background(Forest), contentAlignment = Alignment.Center) {
                    Text("t", color = Sage, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Text("TEND", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
            if (selectedTab != 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                    MonthArrow("‹", "Poprzedni miesiąc") { changeDisplayedMonth(displayMonth.minusMonths(1)) }
                    MonthArrow("›", "Następny miesiąc") { changeDisplayedMonth(displayMonth.plusMonths(1)) }
                }
            } else {
                Spacer(Modifier.width(TendSize.iconButton * 2 + TendSpace.small).height(44.dp))
            }
        }
        Text(
            polishMonthName(displayMonth),
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = TendSpace.xlarge, vertical = TendSpace.xsmall),
            style = TendType.monthTitle,
            color = Ink,
            maxLines = 1,
            softWrap = false,
        )
        if (userMessage != null) {
            Text(
                userMessage,
                modifier = Modifier.fillMaxWidth().padding(horizontal = TendSpace.xlarge),
                style = TendType.caption,
                color = Muted,
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(selectedTab) {
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            horizontalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (horizontalDrag < -72f) selectedTab = (selectedTab + 1).coerceAtMost(2)
                            if (horizontalDrag > 72f) selectedTab = (selectedTab - 1).coerceAtLeast(0)
                            horizontalDrag = 0f
                        },
                    )
                },
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(TendMotion.standard)) { it / 5 * direction } + fadeIn(tween(TendMotion.standard))) togetherWith
                    (slideOutHorizontally(tween(TendMotion.standard)) { -it / 5 * direction } + fadeOut(tween(TendMotion.quick)))
            },
            label = "main-tab-content",
        ) { tab ->
            val tabMonth = when (tab) {
                0 -> currentMonth
                1 -> calendarMonth
                else -> budgetMonth
            }
            when (tab) {
                1 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(calendarScroll)
                        .padding(horizontal = TendSpace.xlarge, vertical = TendSpace.medium),
                ) {
                    Spacer(Modifier.height(TendSpace.large))
                    CalendarContent(
                        entries = entries,
                        categories = categories,
                        month = tabMonth,
                        onEntryClick = { editingEntry = it },
                    )
                }

                0 -> StartTab(
                    scrollState = startScroll,
                    categories = categories,
                    entries = currentEntries,
                    budgets = currentBudgets,
                    monthlyBudget = currentMonthlyBudget,
                    carryoverCents = currentCarryoverCents,
                    onAddEntry = onAddEntry,
                    onAddCategory = onAddCategory,
                    onEntryClick = { editingEntry = it },
                )

                else -> BudgetTab(
                    scrollState = budgetScroll,
                    month = tabMonth,
                    canEditPlan = !tabMonth.isBefore(currentMonth),
                    isFuturePlan = tabMonth.isAfter(currentMonth),
                    entries = entries,
                    categories = categories,
                    budgets = budgets,
                    monthlyBudget = monthlyBudget,
                    carryoverCents = carryoverCents,
                    carryoverEnabled = carryoverEnabled,
                    notificationsEnabled = notificationsEnabled,
                    onEnableNotifications = onEnableNotifications,
                    onSetCategoryBudget = onSetCategoryBudget,
                    onSetMonthlyBudget = onSetMonthlyBudget,
                    onClearMonthlyBudget = onClearMonthlyBudget,
                    onCopyPreviousPlan = onCopyPreviousPlan,
                    onSetCarryoverEnabled = onSetCarryoverEnabled,
                    onAddCategory = onAddCategory,
                    onRenameCategory = onRenameCategory,
                    onDeleteCategory = onDeleteCategory,
                    recurringBills = recurringBills,
                    pendingBillReminders = pendingBillReminders,
                    onSaveRecurringBill = onSaveRecurringBill,
                    onDeleteRecurringBill = onDeleteRecurringBill,
                    onApproveBillReminder = onApproveBillReminder,
                    onSkipBillReminder = onSkipBillReminder,
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TendSpace.xlarge, vertical = TendSpace.small),
            shape = CircleShape,
            color = DeepForest,
            border = BorderStroke(1.dp, DeepForest),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(TendSpace.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem("Start", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                BottomNavItem("Kalendarz", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                BottomNavItem("Budżet", selectedTab == 2, Modifier.weight(1f)) { selectedTab = 2 }
            }
        }
    }

    editingEntry?.let { entry ->
        EntryEditorDialog(
            entry = entry,
            categories = categories,
            onDismiss = { editingEntry = null },
            onSave = { edited, cents, categoryId ->
                onUpdateEntry(edited, cents, categoryId)
                editingEntry = null
            },
            onDelete = {
                deletingEntry = entry
                editingEntry = null
            },
        )
    }

    deletingEntry?.let { entry ->
        TendDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("Usunąć wpis?") },
            text = { Text("${formatMoney(entry.amountCents)} zostanie usunięte z historii.") },
            confirmButton = {
                TextButton(onClick = { onDeleteEntry(entry); deletingEntry = null }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { deletingEntry = null }) { Text("Anuluj") } },
        )
    }
}
