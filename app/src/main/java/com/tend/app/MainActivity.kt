package com.tend.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import com.tend.app.data.LedgerEntry
import com.tend.app.data.CategoryBudget
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.MonthlyBudget
import com.tend.app.data.RecurringBill
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.ui.TendViewModel
import com.tend.app.notifications.BillReminderWorker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.app.NotificationManager
import android.content.Context

private val Porcelain = Color(0xFFF3F0E8)
private val Ink = Color(0xFF192821)
private val Forest = Color(0xFF234B3A)
private val Sage = Color(0xFFD8F078)
private val Muted = Color(0xFF68736D)
private val Line = Color(0xFFE2DED3)

class MainActivity : ComponentActivity() {
    private val viewModel: TendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(
            BILL_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BillReminderWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                .setInitialDelay(15, java.util.concurrent.TimeUnit.MINUTES)
                .build(),
        )
        workManager.enqueueUniqueWork(
            BILL_REMINDER_STARTUP_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BillReminderWorker>().build(),
        )
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            var notificationsEnabled by remember { mutableStateOf(hasNotificationAccess(context)) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                notificationsEnabled = granted && hasNotificationAccess(context)
                if (notificationsEnabled) viewModel.refreshBudgetAlerts()
            }
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        notificationsEnabled = hasNotificationAccess(context)
                        if (notificationsEnabled) viewModel.refreshBudgetAlerts()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            val entries by viewModel.entries.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val budgets by viewModel.budgets.collectAsStateWithLifecycle()
            val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
            val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
            val copyPlanMessage by viewModel.copyPlanMessage.collectAsStateWithLifecycle()
            val carryover by viewModel.carryover.collectAsStateWithLifecycle()
            val recurringBills by viewModel.recurringBills.collectAsStateWithLifecycle()
            val pendingBillReminders by viewModel.pendingBillReminders.collectAsStateWithLifecycle()
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Forest,
                    onPrimary = Color.White,
                    background = Porcelain,
                    surface = Color.White,
                    onSurface = Ink,
                    onBackground = Ink,
                    outline = Line,
                ),
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Porcelain) {
                    TendHomeScreen(
                        entries = entries,
                        categories = categories,
                        budgets = budgets,
                        monthlyBudget = monthlyBudget,
                        selectedMonth = selectedMonth,
                        onMonthChange = viewModel::showMonth,
                        onAddEntry = viewModel::addEntry,
                        onAddCategory = viewModel::addCategory,
                        onSetCategoryBudget = viewModel::setCategoryBudget,
                        onSetMonthlyBudget = viewModel::setMonthlyBudget,
                        onCopyPreviousPlan = viewModel::copyPreviousPlan,
                        copyPlanMessage = copyPlanMessage,
                        carryoverCents = carryover,
                        onRenameCategory = viewModel::renameCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        onUpdateEntry = viewModel::updateEntry,
                        onDeleteEntry = viewModel::deleteEntry,
                        notificationsEnabled = notificationsEnabled,
                        onEnableNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                )
                            }
                        },
                        openBillsTab = intent.getBooleanExtra(EXTRA_OPEN_BILLS, false),
                        recurringBills = recurringBills,
                        pendingBillReminders = pendingBillReminders,
                        onSaveRecurringBill = viewModel::saveRecurringBill,
                        onDeleteRecurringBill = viewModel::deleteRecurringBill,
                        onApproveBillReminder = viewModel::approveBillReminder,
                        onSkipBillReminder = viewModel::skipBillReminder,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_BILLS = "com.tend.app.OPEN_BILLS"
        private const val BILL_REMINDER_WORK = "tend_bill_reminders_daily"
        private const val BILL_REMINDER_STARTUP_WORK = "tend_bill_reminders_startup"
    }
}

@Composable
private fun TendHomeScreen(
    entries: List<LedgerEntry>,
    categories: List<ExpenseCategory>,
    budgets: List<CategoryBudget>,
    monthlyBudget: MonthlyBudget?,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onAddEntry: (Long, String, String?) -> Unit,
    onAddCategory: (String) -> Unit,
    onSetCategoryBudget: (String, Long) -> Unit,
    onSetMonthlyBudget: (Long) -> Unit,
    onCopyPreviousPlan: () -> Unit,
    copyPlanMessage: String?,
    carryoverCents: Long,
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
    var monthlyBudgetAmount by remember(monthlyBudget?.plannedCents) {
        mutableStateOf(monthlyBudget?.plannedCents?.let { String.format(Locale("pl", "PL"), "%.2f", it / 100.0) } ?: "")
    }
    val monthLabel = polishMonthName(selectedMonth)
    val spentCents = entries.asSequence().filter { it.type == "expense" }.sumOf { it.amountCents }
    val incomeCents = entries.asSequence().filter { it.type == "income" }.sumOf { it.amountCents }
    val availableCents = (monthlyBudget?.plannedCents ?: 0L) + incomeCents + carryoverCents
    val allocatedCents = budgets.sumOf { it.amountCents }
    val hasBudgetPool = monthlyBudget != null || incomeCents != 0L || carryoverCents != 0L
    val reserveCents = availableCents - allocatedCents

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                CalendarContent(entries = entries, categories = categories, month = selectedMonth, onEntryClick = { editingEntry = it })
            } else {
        if (tab != 2 && selectedMonth == YearMonth.now()) {
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
                    TextButton(onClick = { isIncome = false }) { Text(if (!isIncome) "● Wydatek" else "Wydatek") }
                    TextButton(onClick = { isIncome = true }) { Text(if (isIncome) "● Przychód" else "Przychód") }
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

        Spacer(Modifier.height(24.dp))
        Text(if (tab == 2) "Budżet miesiąca" else "Twój miesiąc", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (tab == 2) {
            if (!notificationsEnabled) {
                TextButton(onClick = onEnableNotifications) {
                    Text("Włącz powiadomienia o limitach")
                }
            }
            TextButton(onClick = { showMonthlyBudgetDialog = true }) {
                Text(if (monthlyBudget == null) "Ustaw budżet miesiąca" else "Edytuj budżet miesiąca")
            }
            TextButton(
                onClick = onCopyPreviousPlan,
                enabled = monthlyBudget == null && budgets.isEmpty(),
            ) { Text("Kopiuj poprzedni podział") }
            copyPlanMessage?.let { Text(it, color = Muted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Ink),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("WYDATKI ŁĄCZNIE", color = Color(0xFFC5D1C5), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Sage))
                }
                Spacer(Modifier.height(4.dp))
                Text(formatMoney(spentCents), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
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
            ExpenseCategorySummary(entries = entries, categories = categories)
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Kategorie", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (tab == 2) TextButton(onClick = { showAddCategory = true }) { Text("+ Kategoria") }
        }
        Spacer(Modifier.height(10.dp))
        if (categories.isEmpty()) {
            Text("Brak kategorii.", color = Muted, fontSize = 14.sp)
        } else {
            categories.forEach { category ->
                val spent = entries.filter { it.categoryId == category.id && it.type == "expense" }.sumOf { it.amountCents }
                val limit = budgets.firstOrNull { it.categoryId == category.id }?.amountCents ?: 0L
                if (tab == 0) {
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

        if (tab == 2) {
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
        if (entries.isEmpty()) {
            Text("Brak wpisów.", color = Muted, fontSize = 13.sp)
        } else {
            entries.take(5).forEach { entry ->
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
            color = Ink,
            border = BorderStroke(1.dp, Ink),
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
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Wybierz kategorię") },
            text = {
                Column {
                    TextButton(onClick = { selectedCategoryId = null; showCategoryPicker = false }) { Text("Bez kategorii") }
                    categories.forEach { category ->
                        TextButton(onClick = { selectedCategoryId = category.id; showCategoryPicker = false }) {
                            Text(category.name)
                        }
                    }
                    TextButton(onClick = { showCategoryPicker = false; showAddCategory = true }) { Text("+ Dodaj kategorię") }
                }
            },
            confirmButton = {},
        )
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false; categoryName = "" },
            title = { Text("Nowa kategoria") },
            text = {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nazwa") },
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
        AlertDialog(
            onDismissRequest = { budgetCategory = null },
            title = { Text("Budżet: ${category.name}") },
            text = {
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { value -> budgetAmount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Miesięczny limit") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
        AlertDialog(
            onDismissRequest = { showMonthlyBudgetDialog = false },
            title = { Text("Budżet na ten miesiąc") },
            text = {
                OutlinedTextField(
                    value = monthlyBudgetAmount,
                    onValueChange = { value -> monthlyBudgetAmount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota do rozdysponowania") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
        AlertDialog(
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
        AlertDialog(
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
        AlertDialog(
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

@Composable
private fun EntryEditorDialog(
    entry: LedgerEntry,
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onSave: (LedgerEntry, Long, String?) -> Unit,
    onDelete: () -> Unit,
) {
    var amount by remember(entry.id) { mutableStateOf(String.format(Locale("pl", "PL"), "%.2f", entry.amountCents / 100.0)) }
    var categoryId by remember(entry.id) { mutableStateOf(entry.categoryId) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj wpis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                if (entry.type == "expense") {
                    Box {
                        OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                            Text(selectedCategory?.name ?: "Bez kategorii")
                        }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bez kategorii") },
                                onClick = { categoryId = null; categoryMenuExpanded = false },
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = { categoryId = category.id; categoryMenuExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val cents = parseAmountCents(amount)
            TextButton(onClick = { cents?.let { onSave(entry, it, categoryId) } }, enabled = cents != null && cents > 0L) { Text("Zapisz") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Usuń", color = Color(0xFFB96855)) }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
    )
}

@Composable
private fun CategoryEditorDialog(
    category: ExpenseCategory,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj kategorię") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onSave(category, name) }, enabled = name.isNotBlank()) { Text("Zapisz") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Usuń", color = Color(0xFFB96855)) }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
    )
}

@Composable
private fun RecurringBillsSection(
    bills: List<RecurringBill>,
    pendingReminders: List<BillReminderOccurrence>,
    categories: List<ExpenseCategory>,
    onAddBill: () -> Unit,
    onEditBill: (RecurringBill) -> Unit,
    onDeleteBill: (RecurringBill) -> Unit,
    onApproveReminder: (BillReminderOccurrence) -> Unit,
    onSkipReminder: (BillReminderOccurrence) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Rachunki cykliczne", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onAddBill) { Text("+ Dodaj") }
    }
    Spacer(Modifier.height(10.dp))

    if (pendingReminders.isNotEmpty()) {
        Text("Do zatwierdzenia", color = Forest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        pendingReminders.forEach { reminder ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Sage),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(reminder.name, color = Ink, fontWeight = FontWeight.SemiBold)
                    val categoryName = categories.firstOrNull { it.id == reminder.categoryId }?.name
                    Text(
                        listOfNotNull(formatMoney(reminder.amountCents), formatEntryDate(reminder.dueAtEpochMillis), categoryName).joinToString(" · "),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onSkipReminder(reminder) }) { Text("Pomiń") }
                        TextButton(onClick = { onApproveReminder(reminder) }) { Text("Dodaj wydatek") }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (bills.isEmpty()) {
        Text("Brak rachunków cyklicznych.", color = Muted, fontSize = 13.sp)
    } else {
        bills.forEach { bill ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(bill.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("${formatMoney(bill.amountCents)} · co miesiąc, dzień ${bill.dayOfMonth}", color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = { onEditBill(bill) }) { Text("Edytuj") }
                TextButton(onClick = { onDeleteBill(bill) }) { Text("Usuń", color = Color(0xFFB96855)) }
            }
        }
    }
}

@Composable
private fun RecurringBillEditorDialog(
    bill: RecurringBill?,
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onSave: (String, Long, String?, Int) -> Unit,
) {
    var name by remember(bill?.id) { mutableStateOf(bill?.name.orEmpty()) }
    var amount by remember(bill?.id) {
        mutableStateOf(bill?.amountCents?.let { String.format(Locale("pl", "PL"), "%.2f", it / 100.0) }.orEmpty())
    }
    var categoryId by remember(bill?.id) { mutableStateOf(bill?.categoryId) }
    var day by remember(bill?.id) { mutableStateOf((bill?.dayOfMonth ?: 1).toString()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill == null) "Nowy rachunek cykliczny" else "Edytuj rachunek") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, singleLine = true)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = day,
                    onValueChange = { value -> day = value.filter(Char::isDigit).take(2) },
                    label = { Text("Dzień płatności (1–31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Box {
                    OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                        Text(selectedCategory?.name ?: "Bez kategorii")
                    }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Bez kategorii") },
                            onClick = { categoryId = null; categoryMenuExpanded = false },
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { categoryId = category.id; categoryMenuExpanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val cents = parseAmountCents(amount)
            val dueDay = day.toIntOrNull()
            TextButton(
                onClick = { if (cents != null && dueDay != null) onSave(name, cents, categoryId, dueDay) },
                enabled = name.isNotBlank() && cents != null && cents > 0L && dueDay != null && dueDay in 1..31,
            ) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

private fun parseAmountCents(amount: String): Long? = runCatching {
    amount.replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it >= java.math.BigDecimal.ZERO }
        ?.movePointRight(2)
        ?.setScale(0, java.math.RoundingMode.HALF_UP)
        ?.longValueExact()
}.getOrNull()

private fun hasNotificationAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).areNotificationsEnabled()
}

@Composable
private fun CalendarContent(
    entries: List<LedgerEntry>,
    categories: List<ExpenseCategory>,
    month: YearMonth,
    onEntryClick: (LedgerEntry) -> Unit,
) {
    val today = LocalDate.now()
    var selectedDate by remember(month) {
        mutableStateOf(if (month == YearMonth.from(today)) today else month.atDay(1))
    }
    val zone = remember { ZoneId.systemDefault() }
    val expenseTotals = remember(entries, zone) {
        entries.asSequence()
            .filter { it.type == "expense" }
            .groupBy { Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zone).toLocalDate() }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.amountCents } }
    }
    val monthTotal = expenseTotals.values.sum()
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val cellCount = ((firstOffset + month.lengthOfMonth() + 6) / 7) * 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("WYDATKI W MIESIĄCU", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(5.dp))
            Text(formatMoney(monthTotal), color = Ink, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))

            val weekdays = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { weekday ->
                    Text(weekday, modifier = Modifier.weight(1f), color = Muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(Modifier.height(6.dp))
            repeat(cellCount / 7) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - firstOffset + 1
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val spent = expenseTotals[date] ?: 0L
                            val isSelected = selectedDate == date
                            Surface(
                                onClick = { selectedDate = date },
                                modifier = Modifier.weight(1f).padding(2.dp).height(62.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Forest else if (spent > 0L) Sage else Porcelain,
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("$day", color = if (isSelected) Color.White else Ink, fontSize = 14.sp, fontWeight = if (isSelected || date == today) FontWeight.Bold else FontWeight.Normal)
                                    if (spent > 0L) {
                                        Text(
                                            compactMoney(spent),
                                            color = if (isSelected) Color.White else Forest,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f).padding(2.dp).height(62.dp))
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(
        selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("pl", "PL")))
            .replaceFirstChar { it.titlecase(Locale("pl", "PL")) },
        color = Ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val selectedEntries = entries.filter {
        Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zone).toLocalDate() == selectedDate
    }
    if (selectedEntries.isEmpty()) {
        Spacer(Modifier.height(7.dp))
        Text("Brak wpisów tego dnia.", color = Muted, fontSize = 13.sp)
    } else {
        selectedEntries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onEntryClick(entry) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    val category = categories.firstOrNull { it.id == entry.categoryId }?.name ?: "Bez kategorii"
                    Text(if (entry.type == "income") "Przychód → Rezerwa" else category, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(formatEntryDate(entry.occurredAtEpochMillis), color = Muted, fontSize = 11.sp)
                }
                Text(
                    "${if (entry.type == "income") "+" else "−"} ${formatMoney(entry.amountCents)}",
                    color = if (entry.type == "income") Forest else Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun compactMoney(cents: Long): String =
    NumberFormat.getIntegerInstance(Locale("pl", "PL")).format(cents / 100) + " zł"

@Composable
private fun CategoryBudgetRow(
    category: ExpenseCategory,
    spentCents: Long,
    budgetCents: Long,
    onEditBudget: () -> Unit,
    onManageCategory: () -> Unit,
) {
    val ratio = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        budgetCents > 0L && spentCents >= budgetCents -> Color(0xFFB96855)
        ratio >= 0.8f -> Color(0xFFB8893E)
        else -> categoryColor(category.id)
    }
    val animatedRatio by animateFloatAsState(ratio, animationSpec = tween(450), label = "category-budget-progress")
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(categoryColor(category.id), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("${category.name}  ✎", modifier = Modifier.clickable(onClick = onManageCategory), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    if (budgetCents == 0L) "${formatMoney(spentCents)} wydano · bez limitu"
                    else if (spentCents <= budgetCents) "${formatMoney(spentCents)} wydano · ${formatMoney(budgetCents - spentCents)} pozostało z ${formatMoney(budgetCents)}"
                    else "${formatMoney(spentCents)} wydano · ${formatMoney(spentCents - budgetCents)} ponad limit",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = onEditBudget) { Text(if (budgetCents == 0L) "Ustaw limit" else "Edytuj") }
        }
        if (budgetCents > 0L) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = progressColor,
                trackColor = Line,
            )
            if (spentCents >= budgetCents) Text("Limit osiągnięty", color = progressColor, fontSize = 11.sp)
            else if (ratio >= 0.8f) Text("Zbliżasz się do limitu", color = progressColor, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CategoryProgressRow(
    category: ExpenseCategory,
    spentCents: Long,
    budgetCents: Long,
) {
    val ratio = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        budgetCents > 0L && spentCents >= budgetCents -> Color(0xFFB96855)
        ratio >= 0.8f -> Color(0xFFB8893E)
        else -> categoryColor(category.id)
    }
    val animatedRatio by animateFloatAsState(ratio, animationSpec = tween(450), label = "start-category-progress")
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(categoryColor(category.id), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(category.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            if (budgetCents == 0L) "${formatMoney(spentCents)} wydano · bez limitu"
            else if (spentCents <= budgetCents) "${formatMoney(spentCents)} wydano · ${formatMoney(budgetCents - spentCents)} pozostało"
            else "${formatMoney(spentCents)} wydano · ${formatMoney(spentCents - budgetCents)} ponad limit",
            color = Muted,
            fontSize = 12.sp,
        )
        if (budgetCents > 0L) {
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = progressColor,
                trackColor = Line,
            )
            if (spentCents >= budgetCents) Text("Limit osiągnięty", color = progressColor, fontSize = 11.sp)
            else if (ratio >= 0.8f) Text("Zbliżasz się do limitu", color = progressColor, fontSize = 11.sp)
        }
    }
}

private fun formatEntryDate(epochMillis: Long): String =
    java.text.SimpleDateFormat("d MMM, HH:mm", Locale("pl", "PL")).format(java.util.Date(epochMillis))

private fun formatMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(cents / 100.0)

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFFC2D5C9), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private data class ExpenseSlice(val label: String, val amountCents: Long, val color: Color)

private val ExpenseChartColors = listOf(
    Color(0xFF234B3A), Color(0xFF91AE64), Color(0xFFD38B61), Color(0xFF6579A1),
    Color(0xFFD67768), Color(0xFF5F9A91), Color(0xFFB49A62), Color(0xFF9C79A8),
)

private fun categoryColor(categoryId: String?): Color =
    if (categoryId == null) Muted else ExpenseChartColors[(categoryId.hashCode() and Int.MAX_VALUE) % ExpenseChartColors.size]

@Composable
private fun ExpenseCategorySummary(entries: List<LedgerEntry>, categories: List<ExpenseCategory>) {
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val slices = remember(entries, categoryNames) {
        entries.asSequence()
            .filter { it.type == "expense" }
            .groupBy { it.categoryId }
            .map { (categoryId, categoryEntries) ->
                categoryId to categoryEntries.sumOf { it.amountCents }
            }
            .filter { it.second > 0L }
            .sortedByDescending { it.second }
            .map { (categoryId, amount) ->
                ExpenseSlice(
                    label = categoryId?.let { categoryNames[it] } ?: "Bez kategorii",
                    amountCents = amount,
                    color = categoryColor(categoryId),
                )
            }
    }
    val totalCents = slices.sumOf { it.amountCents }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Wydatki według kategorii", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Spacer(Modifier.height(16.dp))
            if (slices.isEmpty()) {
                Text("Brak wydatków.", color = Muted, fontSize = 13.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Canvas(Modifier.size(144.dp)) {
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = 360f * slice.amountCents.toFloat() / totalCents.toFloat()
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                            )
                            startAngle += sweep
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        slices.forEach { slice ->
                            val percent = (slice.amountCents * 100f / totalCents).roundToInt()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(9.dp),
                                    shape = CircleShape,
                                    color = slice.color,
                                ) {}
                                Spacer(Modifier.size(7.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(slice.label, color = Ink, fontSize = 12.sp, maxLines = 1)
                                    Text("${formatMoney(slice.amountCents)} · $percent%", color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PolishMonthNames = listOf(
    "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
    "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień",
)

private fun polishMonthName(month: YearMonth): String =
    PolishMonthNames[month.monthValue - 1].replaceFirstChar { it.titlecase(Locale("pl", "PL")) } + " ${month.year}"

@Composable
private fun BottomNavItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Sage else Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) Ink else Color(0xFFBAC3BA),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun MonthArrow(symbol: String, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.75f)) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Text(symbol, modifier = Modifier.semantics { contentDescription = description }, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium)
        }
    }
}
