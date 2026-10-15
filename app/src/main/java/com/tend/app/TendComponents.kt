package com.tend.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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

internal fun compactMoney(cents: Long): String =
    NumberFormat.getIntegerInstance(Locale("pl", "PL")).format(cents / 100) + " zł"

@Composable
internal fun CategoryBudgetRow(
    category: ExpenseCategory,
    spentCents: Long,
    budgetCents: Long,
    onEditBudget: () -> Unit,
    onManageCategory: () -> Unit,
) {
    val ratio = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        budgetCents > 0L && spentCents >= budgetCents -> Danger
        ratio >= 0.8f -> Gold
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

internal fun formatEntryDate(epochMillis: Long): String =
    java.text.SimpleDateFormat("d MMM, HH:mm", Locale("pl", "PL")).format(java.util.Date(epochMillis))

internal fun formatMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(cents / 100.0)

@Composable
internal fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFFC2D5C9), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private data class ExpenseSlice(val label: String, val amountCents: Long, val color: Color)

private val ExpenseChartColors = listOf(
    Color(0xFF176451), Color(0xFF397561), Color(0xFF73A58A), Color(0xFFC69A52),
    Color(0xFFD6B36B), Color(0xFFC87963), Color(0xFF69818A), Color(0xFF98A284),
)

private fun categoryColor(categoryId: String?): Color =
    if (categoryId == null) Muted else ExpenseChartColors[(categoryId.hashCode() and Int.MAX_VALUE) % ExpenseChartColors.size]

@Composable
internal fun ExpenseCategorySummary(entries: List<LedgerEntry>, categories: List<ExpenseCategory>) {
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

internal fun polishMonthName(month: YearMonth): String =
    PolishMonthNames[month.monthValue - 1].replaceFirstChar { it.titlecase(Locale("pl", "PL")) } + " ${month.year}"

@Composable
internal fun BottomNavItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
