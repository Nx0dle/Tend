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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
    onClearMonthlyBudget: () -> Unit,
    onCopyPreviousPlan: () -> Unit,
    copyPlanMessage: String?,
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
    var showClearMonthlyBudgetConfirmation by remember { mutableStateOf(false) }
    var showPlanOptions by remember { mutableStateOf(false) }
    var recentEntriesExpanded by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var budgetMonth by remember { mutableStateOf(selectedMonth) }
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
            if (selectedTab != 0) Row(horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                MonthArrow("‹", "Poprzedni miesiąc") { changeDisplayedMonth(displayMonth.minusMonths(1)) }
                MonthArrow("›", "Następny miesiąc") {
                    changeDisplayedMonth(displayMonth.plusMonths(1))
                }
            } else Spacer(Modifier.width(96.dp).height(44.dp))
        }
        Text(
            monthLabel,
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = TendSpace.xlarge, vertical = TendSpace.xsmall),
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
            val canEditTabPlan = !tabMonth.isBefore(currentMonth)
            val isFutureTabPlan = tabMonth.isAfter(currentMonth)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TendSpace.xlarge, vertical = TendSpace.medium),
            ) {
                when (tab) {
                    1 -> {
                        Spacer(Modifier.height(TendSpace.large))
                        CalendarContent(entries = displayEntries, categories = categories, month = tabMonth, onEntryClick = { editingEntry = it })
                    }
                    0 -> {
                        Spacer(Modifier.height(TendSpace.small))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TendRadius.card),
                            colors = CardDefaults.cardColors(containerColor = DeepForest),
                        ) {
                            Column(Modifier.padding(TendSpace.xlarge)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "WYDATKI W MIESIĄCU",
                                        color = Color(0xFFC5D1C5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.4.sp,
                                    )
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Sage))
                                }
                                Spacer(Modifier.height(TendSpace.xsmall))
                                Text(
                                    formatMoney(spentCents),
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-1.2).sp,
                                )
                                Spacer(Modifier.height(TendSpace.large))
                                val categoryShare = allocatedCents.coerceAtMost(availableCents.coerceAtLeast(0L))
                                val reserveShare = reserveCents.coerceAtLeast(0L)
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(TendRadius.small)),
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
                                Spacer(Modifier.height(TendSpace.large))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                                    SummaryMetric("PULA", if (hasBudgetPool) formatMoney(availableCents) else "Nieustawiona", Modifier.weight(1f))
                                    SummaryMetric("LIMITY", formatMoney(allocatedCents), Modifier.weight(1f))
                                    SummaryMetric("REZERWA", if (!hasBudgetPool && allocatedCents == 0L) "—" else formatMoney(reserveCents), Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(Modifier.height(TendSpace.medium))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TendRadius.card),
                            color = Color.White,
                            border = BorderStroke(1.dp, Line),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = TendSpace.xlarge, vertical = TendSpace.large),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                                    Text("DZISIAJ", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                                    val expenseCountLabel = when {
                                        todayExpenses.size == 1 -> "1 wydatek"
                                        todayExpenses.size % 100 in 12..14 -> "${todayExpenses.size} wydatków"
                                        todayExpenses.size % 10 in 2..4 -> "${todayExpenses.size} wydatki"
                                        else -> "${todayExpenses.size} wydatków"
                                    }
                                    TendBadge(expenseCountLabel, color = Forest, containerColor = Sage)
                                }
                                Text(formatMoney(todaySpentCents), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(TendSpace.medium))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TendRadius.card),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Line),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = TendSpace.large, vertical = TendSpace.medium),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Porcelain,
                                        border = BorderStroke(1.dp, Line.copy(alpha = 0.6f)),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(TendSpace.xsmall),
                                            horizontalArrangement = Arrangement.spacedBy(TendSpace.xsmall),
                                        ) {
                                            EntryModeChoice("Wydatek", selected = !isIncome) { isIncome = false }
                                            EntryModeChoice("Przychód", selected = isIncome) { isIncome = true }
                                        }
                                    }

                                    if (!isIncome) {
                                        val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .clip(CircleShape)
                                                .clickable { showCategoryPicker = true },
                                            shape = CircleShape,
                                            color = Sage,
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .height(TendSize.control)
                                                    .padding(horizontal = TendSpace.medium),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                if (selectedCategoryId != null) {
                                                    Box(
                                                        Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(categoryColor(selectedCategoryId)),
                                                    )
                                                }
                                                Text(
                                                    text = selectedCategory?.name ?: "Bez kategorii",
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    color = Forest,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text("▾", color = Forest, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(TendSpace.medium))

                                val amountTextStyle = LocalTextStyle.current.copy(
                                    color = Ink,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.5).sp,
                                    lineHeight = 30.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(TendSpace.medium),
                                ) {
                                    BasicTextField(
                                        value = amount,
                                        onValueChange = { value ->
                                            amount = value.filter { it.isDigit() || it == ',' || it == '.' }
                                            amountError = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        textStyle = amountTextStyle,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            val cents = parseAmountCents(amount)
                                            if (cents != null && cents > 0L) {
                                                onAddEntry(cents, if (isIncome) "income" else "expense", if (isIncome) null else selectedCategoryId)
                                                amount = ""
                                                amountError = false
                                                entrySaved = true
                                            } else {
                                                amountError = true
                                            }
                                        }),
                                        cursorBrush = SolidColor(Forest),
                                        decorationBox = { innerTextField ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.weight(1f, fill = false)) {
                                                    if (amount.isEmpty()) {
                                                        Text("0,00", style = amountTextStyle.copy(color = Color(0xFFABB3AD)))
                                                    }
                                                    innerTextField()
                                                }
                                                Spacer(Modifier.width(6.dp))
                                                Text("zł", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            }
                                        },
                                    )

                                    Surface(
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
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        color = animateColorAsState(
                                            if (entrySaved) Forest else DeepForest,
                                            animationSpec = tween(180),
                                            label = "entry-button-color",
                                        ).value,
                                        shadowElevation = 2.dp,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            AnimatedContent(
                                                targetState = entrySaved,
                                                transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
                                                label = "entry-icon-feedback",
                                            ) { saved ->
                                                if (saved) {
                                                    Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text("+", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (amountError) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Wpisz kwotę większą od zera", color = Danger, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(TendSpace.xlarge))

                        TendSectionHeader("Kategorie")
                        Spacer(Modifier.height(TendSpace.small))
                        if (categories.isEmpty()) {
                            Text("Brak kategorii.", color = Muted, fontSize = 14.sp)
                        } else {
                            categories.forEach { category ->
                                val spent = displayEntries.filter { it.categoryId == category.id && it.type == "expense" }.sumOf { it.amountCents }
                                val limit = displayBudgets.firstOrNull { it.categoryId == category.id }?.amountCents ?: 0L
                                CategoryProgressRow(
                                    category = category,
                                    spentCents = spent,
                                    budgetCents = limit,
                                )
                            }
                        }

                        Spacer(Modifier.height(TendSpace.xlarge))

                        TendSectionHeader(
                            title = "Ostatnie wpisy",
                            badge = {
                                if (displayEntries.isNotEmpty()) {
                                    TendBadge("${displayEntries.size}", color = Muted, containerColor = Line.copy(alpha = 0.5f))
                                }
                            },
                            action = {
                                if (displayEntries.size > 5) {
                                    TextButton(onClick = { recentEntriesExpanded = !recentEntriesExpanded }) {
                                        Text(if (recentEntriesExpanded) "Zwiń" else "Pokaż więcej (${displayEntries.size})", maxLines = 1, softWrap = false)
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.height(TendSpace.small))
                        if (displayEntries.isEmpty()) {
                            Text("Brak wpisów.", color = Muted, fontSize = 13.sp)
                        } else {
                            val visibleEntries = if (recentEntriesExpanded) displayEntries else displayEntries.take(5)
                            visibleEntries.forEach { entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = TendSpace.small).clickable { editingEntry = entry },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        val categoryNameForEntry = categories.firstOrNull { it.id == entry.categoryId }?.name ?: "Bez kategorii"
                                        Text(if (entry.type == "income") "Przychód" else categoryNameForEntry, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formatEntryDate(entry.occurredAtEpochMillis), color = Muted, fontSize = 11.sp)
                                    }
                                    Text("${if (entry.type == "income") "+" else "−"} ${formatMoney(entry.amountCents)}", color = if (entry.type == "income") Forest else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(TendSpace.xxlarge))
                    }
                    else -> {
                        Spacer(Modifier.height(TendSpace.small))
                        TendSectionHeader(
                            title = "Plan miesiąca",
                            action = {
                                if (canEditTabPlan) {
                                    TextButton(onClick = { showPlanOptions = true }) {
                                        Text("Opcje planu", maxLines = 1, softWrap = false)
                                    }
                                }
                            },
                        )
                        if (!canEditTabPlan) {
                            Spacer(Modifier.height(TendSpace.small))
                            TendBadge("Tylko podgląd", color = Forest, containerColor = Sage)
                        } else if (isFutureTabPlan) {
                            Spacer(Modifier.height(TendSpace.small))
                            TendBadge("Planowanie z wyprzedzeniem", color = Forest, containerColor = Sage)
                        }
                        if (canEditTabPlan && !isFutureTabPlan && !notificationsEnabled) {
                            TextButton(onClick = onEnableNotifications) {
                                Text("Włącz powiadomienia o limitach")
                            }
                        }
                        copyPlanMessage?.let { Text(it, color = Muted, fontSize = 12.sp) }
                        Spacer(Modifier.height(TendSpace.medium))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TendRadius.card),
                            colors = CardDefaults.cardColors(containerColor = DeepForest),
                        ) {
                            Column(Modifier.padding(TendSpace.xlarge)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "DO DYSPOZYCJI",
                                        color = Color(0xFFC5D1C5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.4.sp,
                                    )
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Sage))
                                }
                                Spacer(Modifier.height(TendSpace.xsmall))
                                val headlineAmount = if (!hasBudgetPool) "Nieustawiona" else formatMoney(availableCents)
                                Text(headlineAmount, color = Color.White, fontSize = if (headlineAmount == "Nieustawiona") 28.sp else 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
                                Spacer(Modifier.height(TendSpace.large))
                                val categoryShare = allocatedCents.coerceAtMost(availableCents.coerceAtLeast(0L))
                                val reserveShare = reserveCents.coerceAtLeast(0L)
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(TendRadius.small)),
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
                                Spacer(Modifier.height(TendSpace.large))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                                    SummaryMetric("LIMITY", formatMoney(allocatedCents), Modifier.weight(1f))
                                    SummaryMetric("REZERWA", if (!hasBudgetPool && allocatedCents == 0L) "—" else formatMoney(reserveCents), Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(Modifier.height(TendSpace.xlarge))
                        TendSectionHeader(
                            title = "Limity kategorii",
                            action = {
                                if (canEditTabPlan) TextButton(onClick = { showAddCategory = true }) { Text("+ Kategoria", maxLines = 1, softWrap = false) }
                            },
                        )
                        Spacer(Modifier.height(TendSpace.small))
                        if (categories.isEmpty()) {
                            Text("Brak kategorii.", color = Muted, fontSize = 14.sp)
                        } else {
                            categories.forEach { category ->
                                val spent = displayEntries.filter { it.categoryId == category.id && it.type == "expense" }.sumOf { it.amountCents }
                                val limit = displayBudgets.firstOrNull { it.categoryId == category.id }?.amountCents ?: 0L
                                if (!canEditTabPlan) {
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
                                        isFuturePlan = isFutureTabPlan,
                                    )
                                }
                            }
                        }

                        if (tabMonth == currentMonth) {
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
                }
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

    if (showCategoryPicker) {
        TendDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Wybierz kategorię") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TendSpace.small)) {
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
                        shape = RoundedCornerShape(TendRadius.control),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = TendSpace.large, vertical = TendSpace.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TendSpace.small),
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
                    shape = RoundedCornerShape(TendRadius.control),
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

    if (showPlanOptions) {
        BudgetPlanOptionsDialog(
            monthLabel = monthLabel,
            monthlyBudgetCents = displayMonthlyBudget?.plannedCents,
            carryoverEnabled = carryoverEnabled,
            carryoverCents = displayCarryover,
            canCopyPreviousPlan = displayBudgets.isEmpty(),
            onCarryoverChange = onSetCarryoverEnabled,
            onEditPool = { showPlanOptions = false; showMonthlyBudgetDialog = true },
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
                    color = Muted,
                    fontSize = 14.sp,
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
            text = { Text("Wyłączyć przyszłe przypomnienia dla „${bill.name}”?") },
            confirmButton = {
                TextButton(onClick = { onDeleteRecurringBill(bill); deletingRecurringBill = null }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { deletingRecurringBill = null }) { Text("Anuluj") } },
        )
    }
}
