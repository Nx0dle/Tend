package com.tend.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

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
            .filter { it.type == "expense" }
            .groupBy { Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zone).toLocalDate() }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.amountCents } }
    }
    val monthTotal = expenseTotals.values.sum()
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val cellCount = ((firstOffset + month.lengthOfMonth() + 6) / 7) * 7

    val selectedEntries = entries.filter {
        Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zone).toLocalDate() == selectedDate
    }
    val busiestDay = expenseTotals.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Column(verticalArrangement = Arrangement.spacedBy(TendSpace.large)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(TendRadius.card),
            colors = CardDefaults.cardColors(containerColor = DeepForest),
        ) {
            Column(Modifier.fillMaxWidth().padding(TendSpace.xlarge)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("WYDATKI MIESIĄCA", color = Color(0xFFC5D1C5), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Sage))
                }
                Spacer(Modifier.height(TendSpace.xsmall))
                Text(formatMoney(monthTotal), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
                Spacer(Modifier.height(TendSpace.large))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Sage.copy(alpha = 0.2f)))
                Spacer(Modifier.height(TendSpace.small))
                Text("${expenseTotals.count { it.value > 0L }} dni z wydatkami", color = Sage.copy(alpha = 0.88f), fontSize = 13.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TendRadius.card)).background(Color.White).padding(TendSpace.medium),
            verticalArrangement = Arrangement.spacedBy(TendSpace.small),
        ) {
            val weekdays = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { weekday ->
                    Text(weekday, modifier = Modifier.weight(1f), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
            val dayExpenses = dayEntries.filter { it.type == "expense" }.sumOf { it.amountCents }
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TendRadius.card)).background(Color.White).padding(TendSpace.large),
                verticalArrangement = Arrangement.spacedBy(TendSpace.medium),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("pl", "PL"))).replaceFirstChar { it.titlecase(Locale("pl", "PL")) },
                        color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    )
                    if (dayExpenses > 0L) Text(formatMoney(dayExpenses), color = Forest, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (dayEntries.isEmpty()) {
                    Text("Brak wpisów tego dnia.", color = Muted, fontSize = 13.sp)
                } else {
                    dayEntries.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(TendRadius.small)).clickable { onEntryClick(entry) }.padding(vertical = TendSpace.xsmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                val category = categories.firstOrNull { it.id == entry.categoryId }?.name ?: "Bez kategorii"
                                Text(if (entry.type == "income") "Przychód" else category, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(formatEntryDate(entry.occurredAtEpochMillis), color = Muted, fontSize = 11.sp)
                            }
                            Text("${if (entry.type == "income") "+" else "−"} ${formatMoney(entry.amountCents)}", color = if (entry.type == "income") Forest else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
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
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = TendSpace.small, vertical = 2.dp),
        )
    }
}

internal fun compactMoney(cents: Long): String =
    NumberFormat.getIntegerInstance(Locale("pl", "PL")).format(cents / 100) + " zł"

@Composable
internal fun CategoryBudgetRow(
    category: ExpenseCategory,
    spentCents: Long,
    budgetCents: Long,
    onEditBudget: () -> Unit,
    onManageCategory: () -> Unit,
    isFuturePlan: Boolean = false,
) {
    val ratio = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        budgetCents > 0L && spentCents >= budgetCents -> Danger
        ratio >= 0.8f -> Gold
        else -> categoryColor(category.id)
    }
    val animatedRatio by animateFloatAsState(ratio, animationSpec = tween(TendMotion.progress), label = "category-budget-progress")
    Column(Modifier.fillMaxWidth().padding(vertical = TendSpace.small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(categoryColor(category.id), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("${category.name}  ✎", modifier = Modifier.clickable(onClick = onManageCategory), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (budgetCents > 0L && spentCents > budgetCents) {
                        Spacer(Modifier.width(TendSpace.small))
                        TendBadge("Ponad limit", color = Danger, containerColor = Danger.copy(alpha = 0.12f))
                    } else if (budgetCents > 0L && ratio >= 0.8f) {
                        Spacer(Modifier.width(TendSpace.small))
                        TendBadge("80% limitu", color = Color(0xFF8B6420), containerColor = Gold.copy(alpha = 0.18f))
                    } else if (isFuturePlan && budgetCents == 0L) {
                        Spacer(Modifier.width(TendSpace.small))
                        TendBadge("Brak limitu", color = Muted, containerColor = Line.copy(alpha = 0.6f))
                    }
                }
                Text(
                    when {
                        isFuturePlan && budgetCents == 0L -> "Nieustawiony limit"
                        isFuturePlan -> "Limit ${formatMoney(budgetCents)}"
                        budgetCents == 0L -> "${formatMoney(spentCents)} wydano · bez limitu"
                        spentCents <= budgetCents -> "${formatMoney(spentCents)} wydano · ${formatMoney(budgetCents - spentCents)} pozostało"
                        else -> "${formatMoney(spentCents)} wydano · ${formatMoney(spentCents - budgetCents)} ponad limit"
                    },
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = onEditBudget) { Text(if (budgetCents == 0L) "Ustaw limit" else "Edytuj") }
        }
        if (budgetCents > 0L && !isFuturePlan) {
            Spacer(Modifier.height(TendSpace.small))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = progressColor,
                trackColor = Line,
            )
        }
    }
}

@Composable
internal fun CategoryProgressRow(
    category: ExpenseCategory,
    spentCents: Long,
    budgetCents: Long,
) {
    val ratio = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        budgetCents > 0L && spentCents >= budgetCents -> Danger
        ratio >= 0.8f -> Gold
        else -> categoryColor(category.id)
    }
    val animatedRatio by animateFloatAsState(ratio, animationSpec = tween(TendMotion.progress), label = "start-category-progress")
    Column(Modifier.fillMaxWidth().padding(vertical = TendSpace.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(categoryColor(category.id), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(category.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (budgetCents > 0L && spentCents > budgetCents) {
                Spacer(Modifier.width(TendSpace.small))
                TendBadge("Ponad limit", color = Danger, containerColor = Danger.copy(alpha = 0.12f))
            } else if (budgetCents > 0L && ratio >= 0.8f) {
                Spacer(Modifier.width(TendSpace.small))
                TendBadge("80% limitu", color = Color(0xFF8B6420), containerColor = Gold.copy(alpha = 0.18f))
            }
        }
        Spacer(Modifier.height(TendSpace.xsmall))
        Text(
            if (budgetCents == 0L) "${formatMoney(spentCents)} wydano · bez limitu"
            else if (spentCents <= budgetCents) "${formatMoney(spentCents)} wydano · ${formatMoney(budgetCents - spentCents)} pozostało"
            else "${formatMoney(spentCents)} wydano · ${formatMoney(spentCents - budgetCents)} ponad limit",
            color = Muted,
            fontSize = 12.sp,
        )
        if (budgetCents > 0L) {
            Spacer(Modifier.height(TendSpace.small))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = progressColor,
                trackColor = Line,
            )
        }
    }
}

internal fun formatEntryDate(epochMillis: Long): String =
    java.text.SimpleDateFormat("d MMM, HH:mm", Locale("pl", "PL")).format(java.util.Date(epochMillis))

internal fun formatMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(cents / 100.0)

@Composable
internal fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFFC2D5C9), fontSize = 12.sp)
        Spacer(Modifier.height(TendSpace.xsmall))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private val CategoryColors = listOf(
    Color(0xFF176451), Color(0xFF397561), Color(0xFF73A58A), Color(0xFFC69A52),
    Color(0xFFD6B36B), Color(0xFFC87963), Color(0xFF69818A), Color(0xFF98A284),
)

internal fun categoryColor(categoryId: String?): Color =
    if (categoryId == null) Muted else CategoryColors[(categoryId.hashCode() and Int.MAX_VALUE) % CategoryColors.size]
private val PolishMonthNames = listOf(
    "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
    "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień",
)

internal fun polishMonthName(month: YearMonth): String =
    PolishMonthNames[month.monthValue - 1].replaceFirstChar { it.titlecase(Locale("pl", "PL")) } + " ${month.year}"

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
                color = if (selected) Ink else Color(0xFFBAC3BA),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
