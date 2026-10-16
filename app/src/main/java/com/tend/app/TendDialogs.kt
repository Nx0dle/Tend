package com.tend.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tend.app.data.BillReminderOccurrence
import com.tend.app.data.ExpenseCategory
import com.tend.app.data.LedgerEntry
import com.tend.app.data.RecurringBill
import java.util.Locale

@Composable
internal fun EntryEditorDialog(
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
    TendDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj wpis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(TendRadius.control),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
                if (entry.type == "expense") {
                    Box {
                        CategoryMenuField(selectedCategory?.name ?: "Bez kategorii") { categoryMenuExpanded = true }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            shape = RoundedCornerShape(TendRadius.control),
                            containerColor = Color.White,
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
                TextButton(onClick = onDelete) { Text("Usuń", color = Danger) }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
    )
}

@Composable
internal fun CategoryEditorDialog(
    category: ExpenseCategory,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    TendDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj kategorię") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa kategorii") },
                shape = RoundedCornerShape(TendRadius.control),
                colors = tendFieldColors(),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(category, name) }, enabled = name.isNotBlank()) { Text("Zapisz") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Usuń", color = Danger) }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
    )
}

@Composable
internal fun RecurringBillsSection(
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
            Text("Rachunki cykliczne", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (pendingReminders.isNotEmpty()) {
                TendBadge("${pendingReminders.size} do zatwierdzenia", color = Forest, containerColor = Sage)
            }
        }
        TextButton(onClick = onAddBill) { Text("+ Dodaj") }
    }
    Spacer(Modifier.height(TendSpace.small))

    if (pendingReminders.isNotEmpty()) {
        pendingReminders.forEach { reminder ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = TendSpace.xsmall),
                shape = RoundedCornerShape(TendRadius.control),
                colors = CardDefaults.cardColors(containerColor = Sage),
            ) {
                Column(Modifier.padding(TendSpace.large)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(reminder.name, color = Ink, fontWeight = FontWeight.SemiBold)
                        TendBadge("Wymaga zgody", color = Forest, containerColor = Color.White)
                    }
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
        Spacer(Modifier.height(TendSpace.medium))
    }

    if (bills.isEmpty()) {
        Text("Brak rachunków cyklicznych.", color = Muted, fontSize = 13.sp)
    } else {
        bills.forEach { bill ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = TendSpace.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(bill.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("${formatMoney(bill.amountCents)} · co miesiąc, dzień ${bill.dayOfMonth}", color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = { onEditBill(bill) }) { Text("Edytuj") }
                TextButton(onClick = { onDeleteBill(bill) }) { Text("Usuń", color = Danger) }
            }
        }
    }
}

@Composable
internal fun RecurringBillEditorDialog(
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
    TendDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill == null) "Nowy rachunek cykliczny" else "Edytuj rachunek") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa rachunku") },
                    shape = RoundedCornerShape(TendRadius.control),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text("Kwota") },
                    suffix = { Text("zł") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(TendRadius.control),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = day,
                    onValueChange = { value -> day = value.filter(Char::isDigit).take(2) },
                    label = { Text("Dzień płatności (1–31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(TendRadius.control),
                    colors = tendFieldColors(),
                    singleLine = true,
                )
                Box {
                    CategoryMenuField(selectedCategory?.name ?: "Bez kategorii") { categoryMenuExpanded = true }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        shape = RoundedCornerShape(TendRadius.control),
                        containerColor = Color.White,
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

internal fun parseAmountCents(amount: String): Long? = runCatching {
    amount.replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it >= java.math.BigDecimal.ZERO }
        ?.movePointRight(2)
        ?.setScale(0, java.math.RoundingMode.HALF_UP)
        ?.longValueExact()
}.getOrNull()


@Composable
internal fun TendDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {},
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(TendRadius.panel),
            color = Porcelain,
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.48f)),
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.padding(TendSpace.xlarge)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.titleLarge.copy(
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp,
                            ),
                        ) { title() }
                    }

                    Spacer(Modifier.width(TendSpace.medium))
                    Surface(onClick = onDismissRequest, shape = CircleShape, color = Sage) {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Text("×", color = DeepForest, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(TendSpace.large))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                Spacer(Modifier.height(TendSpace.large))
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(color = Muted),
                ) { text() }
                Spacer(Modifier.height(TendSpace.xlarge))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
internal fun BudgetPlanOptionsDialog(
    monthLabel: String,
    monthlyBudgetCents: Long?,
    carryoverEnabled: Boolean,
    carryoverCents: Long,
    canCopyPreviousPlan: Boolean,
    onCarryoverChange: (Boolean) -> Unit,
    onEditPool: () -> Unit,
    onRemovePool: () -> Unit,
    onCopyPreviousPlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    TendDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opcje planu") },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TendSpace.medium),
            ) {
                Text(monthLabel, color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(TendRadius.control),
                    border = BorderStroke(1.dp, Line),
                ) {
                    Column(Modifier.padding(TendSpace.large), verticalArrangement = Arrangement.spacedBy(TendSpace.xsmall)) {
                        Text(if (monthlyBudgetCents == null) "Pula miesiąca" else "Pula · ${formatMoney(monthlyBudgetCents)}", color = Ink, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(TendSpace.small)) {
                            TextButton(onClick = onEditPool) { Text(if (monthlyBudgetCents == null) "Ustaw pulę" else "Zmień") }
                            if (monthlyBudgetCents != null) TextButton(onClick = onRemovePool) { Text("Usuń") }
                        }
                    }
                }
                Surface(
                    color = if (carryoverEnabled) Sage.copy(alpha = .52f) else Color.White,
                    shape = RoundedCornerShape(TendRadius.control),
                    border = BorderStroke(1.dp, if (carryoverEnabled) Forest.copy(alpha = .28f) else Line),
                ) {
                    Column(Modifier.padding(TendSpace.large)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Przenoszenie salda", color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(if (carryoverEnabled) "Włączone · ${formatMoney(carryoverCents)}" else "Wyłączone", color = Muted, fontSize = 12.sp)
                            }
                            Switch(checked = carryoverEnabled, onCheckedChange = onCarryoverChange)
                        }
                    }
                }
                Surface(
                    onClick = onCopyPreviousPlan,
                    enabled = canCopyPreviousPlan,
                    color = Color.White,
                    shape = RoundedCornerShape(TendRadius.control),
                    border = BorderStroke(1.dp, Line),
                ) {
                    Column(Modifier.fillMaxWidth().padding(TendSpace.large)) {
                        Text("Skopiuj limity kategorii", color = if (canCopyPreviousPlan) Ink else Muted, fontWeight = FontWeight.SemiBold)
                        Text(if (canCopyPreviousPlan) "Z poprzedniego miesiąca" else "Limity są już ustawione", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Gotowe") } },
    )
}

@Composable
internal fun CategoryChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Sage else Color.White,
        shape = RoundedCornerShape(TendRadius.control),
        border = BorderStroke(1.dp, if (selected) Forest.copy(alpha = 0.45f) else Line),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TendSpace.large, vertical = TendSpace.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TendSpace.medium),
        ) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(if (selected) Gold else Sage))
            Text(label, modifier = Modifier.weight(1f), color = Ink, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
            if (selected) Text("✓", color = Forest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun EntryModeChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) DeepForest else Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .height(TendSize.control)
                .padding(horizontal = TendSpace.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Muted,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CategoryMenuField(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TendRadius.control),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TendSpace.large, vertical = TendSpace.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("⌄", color = Forest, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun tendFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = Forest,
    unfocusedBorderColor = Line,
    focusedLabelColor = Forest,
    unfocusedLabelColor = Muted,
    cursorColor = Forest,
)

@Composable
internal fun MonthArrow(symbol: String, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(44.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.75f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(symbol, modifier = Modifier.semantics { contentDescription = description }, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium)
        }
    }
}
