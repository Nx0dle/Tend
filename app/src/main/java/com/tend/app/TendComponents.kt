package com.tend.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import com.tend.shared.TendPalette
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PolishLocale = Locale.forLanguageTag("pl-PL")
private val MoneyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(PolishLocale)
private val EntryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", PolishLocale)

/** Formats cents as Polish currency. UI-thread only; the formatter is shared on purpose. */
internal fun formatMoney(cents: Long): String = MoneyFormatter.format(cents / 100.0)

internal fun formatEntryDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(EntryDateFormatter)

private val PolishMonthNames = listOf(
    "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
    "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień",
)

internal fun polishMonthName(month: YearMonth): String =
    PolishMonthNames[month.monthValue - 1].replaceFirstChar { it.titlecase(PolishLocale) } + " ${month.year}"

internal fun categoryColor(categoryId: String?): Color = Color(TendPalette.colorFor(categoryId))

@Composable
internal fun CalendarContent(
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
            .filter { it.type == LedgerEntry.TYPE_EXPENSE }
            .groupBy { Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zone).toLocalDate() }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.amountCents } }
    }
    val monthTotal = expenseTotals.values.sum()
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val cellCount = ((firstOffset + month.lengthOfMonth() + 6) / 7) * 7
    val busiestDay = expenseTotals.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Column(verticalArrangement = Arrangement.spacedBy(TendSpace.large)) {
        TendHeroCard(eyebrow = "WYDATKI MIESIĄCA") {
            HeroAmount(formatMoney(monthTotal))
            Spacer(Modifier.height(TendSpace.large))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Sage.copy(alpha = 0.2f)))
            Spacer(Modifier.height(TendSpace.small))
            Text("${expenseTotals.count { it.value > 0L }} dni z wydatkami", color = Sage.copy(alpha = 0.88f), fontSize = 13.sp)
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TendRadius.card)).background(Color.White).padding(TendSpace.medium),
            verticalArrangement = Arrangement.spacedBy(TendSpace.small),
        ) {
            val weekdays = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { weekday ->
                    Text(weekday, modifier = Modifier.weight(1f), style = TendType.micro, color = Muted, textAlign = TextAlign.Center)
                }
            }
            repeat(cellCount / 7) { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TendSpace.xsmall)) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - firstOffset + 1
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val spent = expenseTotals[date] ?: 0L
                            val isSelected = selectedDate == date
                            val markerAlpha = if (spent > 0L) (0.35f + 0.65f * (spent.toFloat() / busiestDay).coerceIn(0f, 1f)) else 0f
                            Surface(
                                onClick = { selectedDate = date },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(TendRadius.control),
                                color = if (isSelected) DeepForest else Color.Transparent,
                                border = if (date == today && !isSelected) BorderStroke(1.dp, Gold.copy(alpha = 0.8f)) else null,
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(vertical = TendSpace.xsmall),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("$day", color = if (isSelected) Color.White else Ink, fontSize = 14.sp, fontWeight = if (isSelected || date == today) FontWeight.Bold else FontWeight.Medium)
                                    Spacer(Modifier.height(TendSpace.xsmall))
                                    Box(
                                        Modifier.size(if (spent > 0L) 5.dp else 3.dp)
                                            .clip(CircleShape)
                                            .background(if (spent == 0L) Color.Transparent else if (isSelected) Gold else Forest.copy(alpha = markerAlpha)),
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f).height(52.dp))
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = { fadeIn(tween(TendMotion.quick)) togetherWith fadeOut(tween(TendMotion.quick)) },
            label = "calendar-selected-day",
        ) { date ->
            val dayEntries = entries.filter { Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zone).toLocalDate() == date }
            val dayExpenses = dayEntries.filter { it.type == LedgerEntry.TYPE_EXPENSE }.sumOf { it.amountCents }
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TendRadius.card)).background(Color.White).padding(TendSpace.large),
                verticalArrangement = Arrangement.spacedBy(TendSpace.small),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", PolishLocale)).replaceFirstChar { it.titlecase(PolishLocale) },
                        modifier = Modifier.weight(1f),
                        color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (dayExpenses > 0L) Text(formatMoney(dayExpenses), color = Forest, style = TendType.rowAmount)
                }
                if (dayEntries.isEmpty()) {
                    Text("Brak wpisów tego dnia.", style = TendType.caption, color = Muted)
                } else {
                    dayEntries.forEach { entry ->
                        LedgerEntryRow(
                            entry = entry,
                            categoryName = categories.firstOrNull { it.id == entry.categoryId }?.name,
                            onClick = { onEntryClick(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TendBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Forest,
    containerColor: Color = Sage,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(TendRadius.small),
    ) {
        Text(
            text = text,
            style = TendType.badge,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TendSpace.small, vertical = 2.dp),
        )
    }
}

@Composable
internal fun TendSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TendSpace.small),
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = TendType.sectionTitle,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            badge?.invoke()
        }
        action?.invoke()
    }
}

/** Deep forest summary surface used by Start, Calendar, and Budget. */
@Composable
internal fun TendHeroCard(
    eyebrow: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TendRadius.card),
        colors = CardDefaults.cardColors(containerColor = DeepForest),
    ) {
        Column(Modifier.padding(TendSpace.xlarge)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(eyebrow, style = TendType.heroLabel, color = OnHeroMuted)
                Box(Modifier.size(8.dp).clip(CircleShape).background(Sage))
            }
            Spacer(Modifier.height(TendSpace.xsmall))
            content()
        }
    }
}

@Composable
internal fun HeroAmount(text: String, compact: Boolean = false) {
    Text(text, style = if (compact) TendType.heroAmountCompact else TendType.heroAmount, color = Color.White)
}

/** Shows how the available pool splits into category limits and the unallocated reserve. */
@Composable
internal fun AllocationBar(
    availableCents: Long,
    allocatedCents: Long,
    reserveCents: Long,
    modifier: Modifier = Modifier,
) {
    val categoryShare = allocatedCents.coerceAtMost(availableCents.coerceAtLeast(0L))
    val reserveShare = reserveCents.coerceAtLeast(0L)
    Row(modifier = modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(TendRadius.small))) {
        if (categoryShare > 0L) {
            Box(Modifier.weight(categoryShare.toFloat()).fillMaxHeight().background(if (reserveCents < 0L) Overspend else Sage))
        }
        if (reserveShare > 0L) {
            Box(Modifier.weight(reserveShare.toFloat()).fillMaxHeight().background(Reserve))
        }
        if (categoryShare == 0L && reserveShare == 0L) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.18f)))
        }
    }
}

@Composable
internal fun HeroMetrics(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
        items.forEach { (label, value) ->
            SummaryMetric(label, value, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = TendType.caption, color = OnHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(TendSpace.xsmall))
        Text(value, style = TendType.metricValue, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Single category row for both Start (read-only) and Budget (editable). Passing [onEditBudget]
 * makes it editable; [onManageCategory] adds the rename/delete affordance.
 */
@Composable
internal fun CategoryRow(
    category: ExpenseCategory,
    spentCents: Long,
    budgetCents: Long,
    modifier: Modifier = Modifier,
    isFuturePlan: Boolean = false,
    onEditBudget: (() -> Unit)? = null,
    onManageCategory: (() -> Unit)? = null,
) {
    val ratio = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        budgetCents > 0L && spentCents >= budgetCents -> Danger
        ratio >= 0.8f -> Gold
        else -> categoryColor(category.id)
    }
    val animatedRatio by animateFloatAsState(ratio, animationSpec = tween(TendMotion.progress), label = "category-budget-progress")
    Column(modifier.fillMaxWidth().padding(vertical = TendSpace.small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(categoryColor(category.id), CircleShape))
                    Spacer(Modifier.width(TendSpace.small))
                    val titleModifier = if (onManageCategory != null) Modifier.weight(1f, fill = false).clickable(onClick = onManageCategory) else Modifier.weight(1f, fill = false)
                    Text(
                        text = if (onManageCategory != null) "${category.name}  ✎" else category.name,
                        modifier = titleModifier,
                        style = TendType.rowTitle,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (budgetCents > 0L && spentCents > budgetCents) {
                        Spacer(Modifier.width(TendSpace.small))
                        TendBadge("Ponad limit", color = Danger, containerColor = Danger.copy(alpha = 0.12f))
                    } else if (budgetCents > 0L && ratio >= 0.8f) {
                        Spacer(Modifier.width(TendSpace.small))
                        TendBadge("80% limitu", color = NoticeText, containerColor = Gold.copy(alpha = 0.18f))
                    } else if (isFuturePlan && budgetCents == 0L && onEditBudget != null) {
                        Spacer(Modifier.width(TendSpace.small))
                        TendBadge("Brak limitu", color = Muted, containerColor = Line.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(TendSpace.xsmall))
                Text(
                    when {
                        isFuturePlan && budgetCents == 0L -> "Nieustawiony limit"
                        isFuturePlan -> "Limit ${formatMoney(budgetCents)}"
                        budgetCents == 0L -> "${formatMoney(spentCents)} wydano · bez limitu"
                        spentCents <= budgetCents -> "${formatMoney(spentCents)} wydano · ${formatMoney(budgetCents - spentCents)} pozostało"
                        else -> "${formatMoney(spentCents)} wydano · ${formatMoney(spentCents - budgetCents)} ponad limit"
                    },
                    style = TendType.caption,
                    color = Muted,
                )
            }
            if (onEditBudget != null) {
                TextButton(onClick = onEditBudget) { Text(if (budgetCents == 0L) "Ustaw limit" else "Edytuj") }
            }
        }
        if (budgetCents > 0L && !isFuturePlan) {
            Spacer(Modifier.height(TendSpace.small))
            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = progressColor,
                trackColor = Line,
            )
        }
    }
}

@Composable
internal fun LedgerEntryRow(
    entry: LedgerEntry,
    categoryName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncome = entry.type == LedgerEntry.TYPE_INCOME
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(TendRadius.small)).clickable(onClick = onClick).padding(vertical = TendSpace.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isIncome) "Przychód" else (categoryName ?: "Bez kategorii"),
                style = TendType.rowTitle,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatEntryDate(entry.occurredAtEpochMillis), style = TendType.micro, color = Muted)
        }
        Text(
            text = "${if (isIncome) "+" else "−"} ${formatMoney(entry.amountCents)}",
            style = TendType.rowAmount,
            color = if (isIncome) Forest else Ink,
        )
    }
}

@Composable
internal fun BottomNavItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Sage else Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) Ink else OnHeroMuted,
                style = TendType.navLabel,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
