package com.tend.app

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tend.app.data.BillProposal
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
    var amount by remember(entry.id) { mutableStateOf(String.format(Locale.forLanguageTag("pl-PL"), "%.2f", entry.amountCents / 100.0)) }
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
                if (entry.type == LedgerEntry.TYPE_EXPENSE) {
                    Box {
                        CategoryMenuField(selectedCategory?.name ?: "Bez kategorii") { categoryMenuExpanded = true }
                        CategoryDropdownMenu(
                            expanded = categoryMenuExpanded,
                            categories = categories,
                            onDismiss = { categoryMenuExpanded = false },
                            onSelect = { categoryId = it; categoryMenuExpanded = false },
                        )
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
internal fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    TendDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa kategoria") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa") },
                shape = RoundedCornerShape(TendRadius.control),
                colors = tendFieldColors(),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name); onDismiss() }, enabled = name.isNotBlank()) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
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
    proposals: List<BillProposal>,
    categories: List<ExpenseCategory>,
    onAddBill: () -> Unit,
    onEditBill: (RecurringBill) -> Unit,
    onDeleteBill: (RecurringBill) -> Unit,
    onApproveBill: (RecurringBill) -> Unit,
    onSkipBill: (BillProposal) -> Unit,
) {
    TendSectionHeader(
        title = "Rachunki cykliczne",
        badge = {
            val pendingCount = proposals.count { it.status == BillReminderOccurrence.STATUS_PENDING }
            if (pendingCount > 0) {
                TendBadge("$pendingCount do zatwierdzenia", color = Forest, containerColor = Sage)
            }
        },
        action = { TextButton(onClick = onAddBill) { Text("+ Dodaj", maxLines = 1, softWrap = false) } },
    )
    Spacer(Modifier.height(TendSpace.small))

    if (bills.isEmpty()) {
        Text("Brak rachunków cyklicznych.", style = TendType.micro, color = Muted)
    } else {
        bills.forEach { bill ->
            val proposal = proposals.firstOrNull { it.bill.id == bill.id }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = TendSpace.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            bill.name,
                            modifier = Modifier.weight(1f, fill = false),
                            style = TendType.rowTitle,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when (proposal?.status) {
                            BillReminderOccurrence.STATUS_PENDING -> {
                                Spacer(Modifier.width(TendSpace.small))
                                TendBadge("Do zatwierdzenia", color = NoticeText, containerColor = Gold.copy(alpha = 0.18f))
                            }
                            BillReminderOccurrence.STATUS_APPROVED -> {
                                Spacer(Modifier.width(TendSpace.small))
                                TendBadge("Zatwierdzono", color = Forest, containerColor = Sage)
                            }
                            BillReminderOccurrence.STATUS_SKIPPED -> {
                                Spacer(Modifier.width(TendSpace.small))
                                TendBadge("Pominięto", color = Muted, containerColor = Line.copy(alpha = 0.6f))
                            }
                        }
                    }
                    Text(
                        "${formatMoney(bill.amountCents)} szacunkowo · co miesiąc, dzień ${bill.dayOfMonth}" +
                            (categories.firstOrNull { it.id == bill.categoryId }?.name?.let { " · $it" } ?: ""),
                        style = TendType.caption,
                        color = Muted,
                    )
                    if (proposal?.status == BillReminderOccurrence.STATUS_PENDING) {
                        Row {
                            TextButton(onClick = { onSkipBill(proposal) }) { Text("Pomiń") }
                            TextButton(onClick = { onApproveBill(bill) }) { Text("Zatwierdź") }
                        }
                    }
                }
                TextButton(onClick = { onEditBill(bill) }) { Text("Edytuj") }
                TextButton(onClick = { onDeleteBill(bill) }) { Text("Usuń", color = Danger) }
            }
        }
    }
}

/** Approval sheet: correct the estimated amount, then write today's expense — or skip the month. */
@Composable
internal fun BillApprovalDialog(
    bill: RecurringBill,
    onDismiss: () -> Unit,
    onApprove: (Long) -> Unit,
    onSkip: () -> Unit,
) {
    var amount by remember(bill.id) {
        mutableStateOf(String.format(Locale.forLanguageTag("pl-PL"), "%.2f", bill.amountCents / 100.0))
    }
    TendDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zatwierdź rachunek") },
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
                Text(
                    "„${bill.name}” trafi do księgi jako wydatek na dziś.",
                    style = TendType.caption,
                )
            }
        },
        confirmButton = {
            val cents = parseAmountCents(amount)
            TextButton(
                onClick = { if (cents != null) onApprove(cents) },
                enabled = cents != null && cents > 0L,
            ) { Text("Zatwierdź") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text("Pomiń", color = Danger) }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
    )
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
        mutableStateOf(bill?.amountCents?.let { String.format(Locale.forLanguageTag("pl-PL"), "%.2f", it / 100.0) }.orEmpty())
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
                    label = { Text("Kwota szacowana") },
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
                    CategoryDropdownMenu(
                        expanded = categoryMenuExpanded,
                        categories = categories,
                        onDismiss = { categoryMenuExpanded = false },
                        onSelect = { categoryId = it; categoryMenuExpanded = false },
                    )
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

/** Shared category picker used by the entry editor and bill editor. */
@Composable
private fun CategoryDropdownMenu(
    expanded: Boolean,
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(TendRadius.control),
        containerColor = Color.White,
    ) {
        DropdownMenuItem(text = { Text("Bez kategorii") }, onClick = { onSelect(null) })
        categories.forEach { category ->
            DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelect(category.id) })
        }
    }
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
                    Box(Modifier.size(TendSize.iconButton), contentAlignment = Alignment.Center) {
                        Surface(onClick = onDismissRequest, modifier = Modifier.size(TendSize.iconVisual), shape = CircleShape, color = Sage) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("×", color = DeepForest, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            }
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
                Text(monthLabel, color = Forest, style = TendType.compactLabel)
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
                                Text(if (carryoverEnabled) "Włączone · ${formatMoney(carryoverCents)}" else "Wyłączone", style = TendType.caption, color = Muted)
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
                        Text(if (canCopyPreviousPlan) "Z poprzedniego miesiąca" else "Limity są już ustawione", style = TendType.caption, color = Muted)
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
            Text(label, modifier = Modifier.weight(1f), color = Ink, style = TendType.rowTitle, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
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

/** Shared dropdown-style field. Uses one chevron glyph across the app. */
@Composable
internal fun CategoryMenuField(label: String, onClick: () -> Unit) {
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
            Text(label, modifier = Modifier.weight(1f), style = TendType.rowTitle, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Surface(onClick = onClick, modifier = Modifier.size(TendSize.iconButton), shape = CircleShape, color = Color.White.copy(alpha = 0.75f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(symbol, modifier = Modifier.semantics { contentDescription = description }, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium)
        }
    }
}
