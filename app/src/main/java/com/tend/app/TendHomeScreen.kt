package com.tend.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.CategoryBudget
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

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
    onAddEntry: (Long, String, String?) -> Unit,
    onAddCategory: (String) -> Unit,
    onSetCategoryBudget: (String, Long) -> Unit,
    onSetMonthlyBudget: (Long) -> Unit,
    onCopyPreviousPlan: () -> Unit,
    copyPlanMessage: String?,
    carryoverCents: Long,
    currentCarryoverCents: Long,
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
    var amount by remember { mutableStateOf("") }
    var entrySaved by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var isIncome by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(if (openBillsTab) 2 else 0) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var budgetCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var deletingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var deletingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var showRecurringBillEditor by remember { mutableStateOf(false) }
    var editingRecurringBill by remember { mutableStateOf<RecurringBill?>(null) }
    var deletingRecurringBill by remember { mutableStateOf<RecurringBill?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var budgetAmount by remember { mutableStateOf("") }
    var showMonthlyBudgetDialog by remember { mutableStateOf(false) }
    val displayMonth = if (selectedTab == 0) YearMonth.now() else selectedMonth
    val displayEntries = if (selectedTab == 0) currentEntries else entries
    val displayBudgets = if (selectedTab == 0) currentBudgets else budgets
    val displayMonthlyBudget = if (selectedTab == 0) currentMonthlyBudget else monthlyBudget
    val displayCarryover = if (selectedTab == 0) currentCarryoverCents else carryoverCents
    var monthlyBudgetAmount by remember(displayMonthlyBudget?.plannedCents) {
        mutableStateOf(displayMonthlyBudget?.plannedCents?.let { String.format(Locale("pl", "PL"), "%.2f", it / 100.0) } ?: "")
    }
    val monthLabel = polishMonthName(displayMonth)
    val spentCents = displayEntries.asSequence().filter { it.type == "expense" }.sumOf { it.amountCents }
    val incomeCents = displayEntries.asSequence().filter { it.type == "income" }.sumOf { it.amountCents }
    val availableCents = (displayMonthlyBudget?.plannedCents ?: 0L) + incomeCents + displayCarryover
    val allocatedCents = displayBudgets.sumOf { it.amountCents }
    val hasBudgetPool = displayMonthlyBudget != null || incomeCents != 0L || displayCarryover != 0L
    val reserveCents = availableCents - allocatedCents
    val today = LocalDate.now()
    val todayEntries = currentEntries.filter { Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == today }
    val todayExpenses = todayEntries.filter { it.type == "expense" }
    val todaySpentCents = todayExpenses.sumOf { it.amountCents }

    LaunchedEffect(entrySaved) {
        if (entrySaved) {
            delay(900)
            entrySaved = false
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
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Forest), contentAlignment = Alignment.Center) {
                    Text("t", color = Sage, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Text("TEND", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
            if (selectedTab != 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthArrow("‹", "Poprzedni miesiąc") { onMonthChange(selectedMonth.minusMonths(1)) }
                MonthArrow("›", "Następny miesiąc") { onMonthChange(selectedMonth.plusMonths(1)) }
            }
        }
        Text(
            monthLabel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
            color = Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.7).sp,
            maxLines = 1,
            softWrap = false,
        )

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
                (slideInHorizontally(tween(220)) { it / 5 * direction } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 5 * direction } + fadeOut(tween(120)))
            },
            label = "main-tab-content",
        ) { tab ->
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
            if (tab == 1) {
                Spacer(Modifier.height(14.dp))
                CalendarContent(entries = displayEntries, categories = categories, month = selectedMonth, onEntryClick = { editingEntry = it })
            } else {
        if (tab == 0 || (tab == 1 && selectedMonth == YearMonth.now())) {
        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("SZYBKIE DODAWANIE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryModeChoice("Wydatek", selected = !isIncome) { isIncome = false }
                    EntryModeChoice("Przychód", selected = isIncome) { isIncome = true }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { it.isDigit() || it == ',' || it == '.' }
                        amountError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0,00", fontSize = 32.sp, color = Color(0xFFABB3AD)) },
                    suffix = { Text("zł", color = Muted, fontSize = 16.sp) },
                    colors = tendFieldColors(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) ({ Text("Wpisz kwotę większą od zera") }) else null,
                )
                Spacer(Modifier.height(12.dp))
                if (!isIncome) Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Sage, shape = CircleShape, onClick = { showCategoryPicker = true }) {
                        val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
                        Text(
                            selectedCategory?.name ?: "Bez kategorii  ▾",
                            color = Forest,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val cents = parseAmountCents(amount)
                        if (cents != null && cents > 0L) {
                            onAddEntry(cents, if (isIncome) "income" else "expense", if (isIncome) null else selectedCategoryId)
                            amount = ""
                            amountError = false
                            entrySaved = true
                        } else {
                            amountError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                    containerColor = animateColorAsState(
                            if (entrySaved) Forest else Sage,
                            animationSpec = tween(180),
                            label = "entry-button-color",
                        ).value,
                    ),
                ) {
                    AnimatedContent(
                        targetState = entrySaved,
                        transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
                        label = "entry-submit-feedback",
                    ) { saved ->
                        Text(
                            if (saved) "Dodano" else if (isIncome) "Dodaj przychód" else "Dodaj wydatek",
                            color = if (saved) Color.White else Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        }

        if (tab == 0) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("DZISIAJ", color = Forest, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                        Spacer(Modifier.height(3.dp))
                        val expenseCountLabel = when {
                            todayExpenses.size == 1 -> "wydatek"
                            todayExpenses.size % 100 in 12..14 -> "wydatków"
                            todayExpenses.size % 10 in 2..4 -> "wydatki"
                            else -> "wydatków"
                        }
                        Text("${todayExpenses.size} $expenseCountLabel", color = Muted, fontSize = 12.sp)
                    }
                    Text(formatMoney(todaySpentCents), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(if (tab == 2) "Plan miesiąca" else "Twój miesiąc", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (tab == 2) {
            if (selectedMonth == YearMonth.now()) {
                if (!notificationsEnabled) {
                    TextButton(onClick = onEnableNotifications) {
                        Text("Włącz powiadomienia o limitach")
                    }
                }
                TextButton(onClick = { showMonthlyBudgetDialog = true }) {
                    Text(if (displayMonthlyBudget == null) "Ustaw pulę miesiąca" else "Edytuj pulę miesiąca")
                }
                TextButton(
                    onClick = onCopyPreviousPlan,
                    enabled = displayMonthlyBudget == null && displayBudgets.isEmpty(),
                ) { Text("Kopiuj poprzedni podział") }
                copyPlanMessage?.let { Text(it, color = Muted, fontSize = 12.sp) }
            } else {
                Text("Podgląd historycznego planu", color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepForest),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (tab == 2) "PULA MIESIĄCA" else "WYDATKI W MIESIĄCU",
                        color = Color(0xFFC5D1C5),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    )
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Sage))
                }
                Spacer(Modifier.height(4.dp))
                val headlineAmount = if (tab == 2 && !hasBudgetPool) "Nieustawiona" else formatMoney(if (tab == 2) availableCents else spentCents)
                Text(headlineAmount, color = Color.White, fontSize = if (headlineAmount == "Nieustawiona") 28.sp else 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
                Spacer(Modifier.height(16.dp))
                val categoryShare = allocatedCents.coerceAtMost(availableCents.coerceAtLeast(0L))
                val reserveShare = reserveCents.coerceAtLeast(0L)
                Row(
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                ) {
                    if (categoryShare > 0L) {
                        val assignmentColor = if (reserveCents < 0L) Color(0xFFE58669) else Sage
                        Box(Modifier.weight(categoryShare.toFloat()).fillMaxHeight().background(assignmentColor))
                    }
                    if (reserveShare > 0L) {
                        Box(Modifier.weight(reserveShare.toFloat()).fillMaxHeight().background(Color(0xFFD38B61)))
                    }
                    if (categoryShare == 0L && reserveShare == 0L) {
                        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.18f)))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryMetric("PULA", if (hasBudgetPool) formatMoney(availableCents) else "Nieustawiona", Modifier.weight(1f))
                    SummaryMetric("LIMITY", formatMoney(allocatedCents), Modifier.weight(1f))
                    SummaryMetric("REZERWA", if (!hasBudgetPool && allocatedCents == 0L) "—" else formatMoney(reserveCents), Modifier.weight(1f))
                }
            }
        }

        if (tab == 0) {
            Spacer(Modifier.height(22.dp))
            ExpenseCategorySummary(entries = displayEntries, categories = categories)
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (tab == 2) "Limity kategorii" else "Kategorie", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (tab == 2 && selectedMonth == YearMonth.now()) TextButton(onClick = { showAddCategory = true }) { Text("+ Kategoria") }
        }
        Spacer(Modifier.height(10.dp))
        if (categories.isEmpty()) {
            Text("Brak kategorii.", color = Muted, fontSize = 14.sp)
        } else {
            categories.forEach { category ->
                val spent = displayEntries.filter { it.categoryId == category.id && it.type == "expense" }.sumOf { it.amountCents }
                val limit = displayBudgets.firstOrNull { it.categoryId == category.id }?.amountCents ?: 0L
                if (tab == 0 || selectedMonth != YearMonth.now()) {
                    CategoryProgressRow(
                        category = category,
                        spentCents = spent,
                        budgetCents = limit,
                    )
                } else {
                    CategoryBudgetRow(
                        category = category,
                        spentCents = spent,
                        budgetCents = limit,
                        onEditBudget = {
                            budgetCategory = category
                            budgetAmount = if (limit == 0L) "" else String.format(Locale("pl", "PL"), "%.2f", limit / 100.0)
                        },
                        onManageCategory = { editingCategory = category },
                    )
                }
            }
        }

        if (tab == 2 && selectedMonth == YearMonth.now()) {
            Spacer(Modifier.height(26.dp))
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

        if (tab == 0) {
        Spacer(Modifier.height(20.dp))
        Text("Ostatnie wpisy", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (displayEntries.isEmpty()) {
            Text("Brak wpisów.", color = Muted, fontSize = 13.sp)
        } else {
            displayEntries.take(5).forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp).clickable { editingEntry = entry },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        val categoryNameForEntry = categories.firstOrNull { it.id == entry.categoryId }?.name ?: "Bez kategorii"
                        Text(if (entry.type == "income") "Przychód → Rezerwa" else categoryNameForEntry, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(formatEntryDate(entry.occurredAtEpochMillis), color = Muted, fontSize = 11.sp)
                    }
                    Text("${if (entry.type == "income") "+" else "−"} ${formatMoney(entry.amountCents)}", color = if (entry.type == "income") Forest else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        }
        }
        }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            shape = RoundedCornerShape(26.dp),
            color = DeepForest,
            border = BorderStroke(1.dp, DeepForest),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem("Start", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                BottomNavItem("Kalendarz", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                BottomNavItem("Budżet", selectedTab == 2, Modifier.weight(1f)) { selectedTab = 2 }
            }
        }
    }

    if (showCategoryPicker) {
        TendDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Wybierz kategorię") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    CategoryChoiceRow(
                        label = "Bez kategorii",
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null; showCategoryPicker = false },
                    )
                    categories.forEach { category ->
                        CategoryChoiceRow(
                            label = category.name,
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id; showCategoryPicker = false },
                        )
                    }
                    Surface(
                        onClick = { showCategoryPicker = false; showAddCategory = true },
                        color = DeepForest,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("+", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                            Text("Utwórz kategorię", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showAddCategory) {
        TendDialog(
            onDismissRequest = { showAddCategory = false; categoryName = "" },
            title = { Text("Nowa kategoria") },
            text = {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nazwa") },
                    shape = RoundedCornerShape(15.dp),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddCategory(categoryName)
                    categoryName = ""
                    showAddCategory = false
                }, enabled = categoryName.isNotBlank()) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { showAddCategory = false; categoryName = "" }) { Text("Anuluj") } },
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
                    shape = RoundedCornerShape(15.dp),
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

    if (showMonthlyBudgetDialog) {
        TendDialog(
            onDismissRequest = { showMonthlyBudgetDialog = false },
            title = { Text("Budżet na ten miesiąc") },
            text = {
                OutlinedTextField(
                    value = monthlyBudgetAmount,
                    onValueChange = { value -> monthlyBudgetAmount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota do rozdysponowania") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(15.dp),
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

    deletingCategory?.let { category ->
        TendDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Usunąć kategorię?") },
            text = { Text("Wpisy z kategorii „${category.name}” pozostaną w historii jako bez kategorii.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(category)
                    if (selectedCategoryId == category.id) selectedCategoryId = null
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
            text = { Text("Przyszłe przypomnienia dla „${bill.name}” zostaną wyłączone. Już utworzone wpisy pozostaną w historii.") },
            confirmButton = {
                TextButton(onClick = { onDeleteRecurringBill(bill); deletingRecurringBill = null }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { deletingRecurringBill = null }) { Text("Anuluj") } },
        )
    }
}
