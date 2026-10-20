package com.tend.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tend.app.data.CategoryBudget
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import com.tend.app.data.MonthlyBudget
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Start tab: current-month snapshot plus frictionless expense/income entry.
 * Owns only its entry-form state; everything else comes from the shell.
 */
@Composable
internal fun StartTab(
    scrollState: ScrollState,
    categories: List<ExpenseCategory>,
    entries: List<LedgerEntry>,
    budgets: List<CategoryBudget>,
    monthlyBudget: MonthlyBudget?,
    carryoverCents: Long,
    pendingBillsCents: Long,
    onAddEntry: (Long, String, String?) -> Unit,
    onAddCategory: (String) -> Unit,
    onEntryClick: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf(false) }
    var entrySaved by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var recentEntriesExpanded by remember { mutableStateOf(false) }

    // If the selected category is deleted elsewhere, fall back to "no category".
    LaunchedEffect(categories) {
        if (selectedCategoryId != null && categories.none { it.id == selectedCategoryId }) selectedCategoryId = null
    }
    LaunchedEffect(entrySaved) {
        if (entrySaved) {
            delay(900)
            entrySaved = false
        }
    }

    val totals = computeBudgetTotals(entries, budgets, monthlyBudget, carryoverCents, pendingBillsCents)
    val availableCents = totals.availableCents
    val allocatedCents = totals.allocatedCents
    val reserveCents = totals.reserveCents
    val hasBudgetPool = totals.hasPool
    val today = LocalDate.now()
    val todayExpenses = entries.filter {
        it.type == LedgerEntry.TYPE_EXPENSE &&
            Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
    val todaySpentCents = todayExpenses.sumOf { it.amountCents }

    fun submitEntry() {
        val cents = parseAmountCents(amount)
        if (cents != null && cents > 0L) {
            onAddEntry(cents, if (isIncome) LedgerEntry.TYPE_INCOME else LedgerEntry.TYPE_EXPENSE, if (isIncome) null else selectedCategoryId)
            amount = ""
            amountError = false
            entrySaved = true
        } else {
            amountError = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = TendSpace.xlarge, vertical = TendSpace.medium),
    ) {
        Spacer(Modifier.height(TendSpace.small))
        TendHeroCard(eyebrow = "WYDATKI W MIESIĄCU") {
            HeroAmount(formatMoney(totals.spentCents))
            Spacer(Modifier.height(TendSpace.large))
            AllocationBar(availableCents = availableCents, allocatedCents = allocatedCents, reserveCents = reserveCents)
            Spacer(Modifier.height(TendSpace.large))
            HeroMetrics(
                listOf(
                    HeroMetric("PULA", if (hasBudgetPool) formatMoney(availableCents) else "Nieustawiona"),
                    HeroMetric("LIMITY", formatMoney(allocatedCents)),
                    HeroMetric(
                        "REZERWA",
                        if (!hasBudgetPool && allocatedCents == 0L) "—" else formatMoney(reserveCents),
                        valueColor = if (reserveCents < 0L) Danger else Color.White,
                        alert = if (reserveCents < 0L) "Ostrzeżenie" else null,
                    ),
                ),
            )
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
                    Text("DZISIAJ", color = Forest, style = TendType.compactLabel)
                    TendBadge(expenseCountLabel(todayExpenses.size), color = Forest, containerColor = Sage)
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
                                    style = TendType.compactLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("⌄", color = Forest, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(TendSpace.medium))

                val amountTextStyle = TendType.amount.copy(
                    color = Ink,
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
                        keyboardActions = KeyboardActions(onDone = { submitEntry() }),
                        cursorBrush = SolidColor(Forest),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f, fill = false)) {
                                    if (amount.isEmpty()) {
                                        Text("0,00", style = amountTextStyle.copy(color = Placeholder))
                                    }
                                    innerTextField()
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("zł", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            }
                        },
                    )

                    Surface(
                        onClick = { submitEntry() },
                        modifier = Modifier.size(TendSize.iconButton),
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
                    Spacer(Modifier.height(TendSpace.xsmall))
                    Text("Wpisz kwotę większą od zera", color = Danger, style = TendType.micro)
                }
            }
        }

        Spacer(Modifier.height(TendSpace.xlarge))

        TendSectionHeader("Kategorie")
        Spacer(Modifier.height(TendSpace.small))
        if (categories.isEmpty()) {
            Text("Brak kategorii.", style = TendType.caption, color = Muted)
        } else {
            categories.forEach { category ->
                CategoryRow(
                    category = category,
                    spentCents = entries.filter { it.categoryId == category.id && it.type == LedgerEntry.TYPE_EXPENSE }.sumOf { it.amountCents },
                    budgetCents = budgets.firstOrNull { it.categoryId == category.id }?.amountCents ?: 0L,
                )
            }
        }

        Spacer(Modifier.height(TendSpace.xlarge))

        TendSectionHeader(
            title = "Ostatnie wpisy",
            badge = {
                if (entries.isNotEmpty()) {
                    TendBadge("${entries.size}", color = Muted, containerColor = Line.copy(alpha = 0.5f))
                }
            },
            action = {
                if (entries.size > 5) {
                    TextButton(onClick = { recentEntriesExpanded = !recentEntriesExpanded }) {
                        Text(if (recentEntriesExpanded) "Zwiń" else "Pokaż więcej (${entries.size})", maxLines = 1, softWrap = false)
                    }
                }
            },
        )
        Spacer(Modifier.height(TendSpace.small))
        if (entries.isEmpty()) {
            Text("Brak wpisów.", style = TendType.micro, color = Muted)
        } else {
            val visibleEntries = if (recentEntriesExpanded) entries else entries.take(5)
            visibleEntries.forEach { entry ->
                LedgerEntryRow(
                    entry = entry,
                    categoryName = categories.firstOrNull { it.id == entry.categoryId }?.name,
                    onClick = { onEntryClick(entry) },
                )
            }
        }
        Spacer(Modifier.height(TendSpace.xxlarge))
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
                            Text("Utwórz kategorię", color = Color.White, style = TendType.rowTitle, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onAdd = { onAddCategory(it) },
        )
    }
}

/** Polish count label: "1 wydatek", "2 wydatki", "5 wydatków". */
internal fun expenseCountLabel(count: Int): String = when {
    count == 1 -> "1 wydatek"
    count % 100 in 12..14 -> "$count wydatków"
    count % 10 in 2..4 -> "$count wydatki"
    else -> "$count wydatków"
}
