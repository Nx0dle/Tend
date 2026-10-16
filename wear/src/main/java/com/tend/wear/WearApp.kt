package com.tend.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.tend.shared.WatchCategory
import com.tend.shared.WatchSnapshot
import com.tend.shared.formatWatchMoney
import com.tend.shared.limitRatio
import java.time.YearMonth

/**
 * Wear app screen: a scrollable, phone-identical list of categories. A category with a limit shows a
 * progress bar; one without a limit shows just its amount. The Tile is the glanceable companion.
 *
 * The list is a [ScalingLazyColumn] so it curves with the round display (items shrink and fade
 * towards the edges) and, out of the box, reacts to the rotating/virtual bezel with the familiar
 * Wear haptic tick per item.
 */
@Composable
fun WearApp() {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<WatchSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        snapshot = WatchSnapshotStore.read(context)
        loading = false
    }

    TendWearTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Porcelain) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Forest)
                }
                snapshot == null -> EmptyState(onRetry = { refreshKey++ })
                else -> SnapshotList(snapshot = snapshot!!, onRefresh = { refreshKey++ })
            }
        }
    }
}

@Composable
private fun SnapshotList(snapshot: WatchSnapshot, onRefresh: () -> Unit) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { Header(snapshot) }
        item {
            Text(
                "Kategorie",
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(snapshot.categories) { category -> CategoryRow(category) }
        if (snapshot.categories.isEmpty()) {
            item { Text("Brak kategorii.", color = Muted, fontSize = 12.sp) }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                RefreshButton(onRefresh)
            }
        }
    }
}

@Composable
private fun Header(snapshot: WatchSnapshot) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            "WYDATKI W MIESIĄCU",
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Text(
            formatWatchMoney(snapshot.totalSpentCents),
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            polishMonth(snapshot.monthKey),
            color = Forest,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        if (snapshot.hasPool) {
            Text(
                "Pula ${formatWatchMoney(snapshot.availableCents)} · Limity ${formatWatchMoney(snapshot.allocatedCents)}",
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryRow(category: WatchCategory) {
    val hasLimit = category.limitCents > 0L
    val ratio = category.limitRatio()
    val progressColor = when {
        hasLimit && category.spentCents >= category.limitCents -> Danger
        ratio >= 0.8f -> Gold
        else -> Color(category.colorArgb)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Color(category.colorArgb), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasLimit && category.spentCents > category.limitCents) {
                Spacer(Modifier.width(6.dp))
                Badge("Ponad limit", Danger)
            } else if (hasLimit && ratio >= 0.8f) {
                Spacer(Modifier.width(6.dp))
                Badge("80% limitu", NoticeText)
            }
        }
        Text(
            text = caption(category, hasLimit),
            color = Muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hasLimit) {
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = Line,
            )
        }
    }
}

private fun caption(category: WatchCategory, hasLimit: Boolean): String = when {
    !hasLimit -> "${formatWatchMoney(category.spentCents)} wydano · bez limitu"
    category.spentCents <= category.limitCents ->
        "${formatWatchMoney(category.spentCents)} wydano · ${formatWatchMoney(category.limitCents - category.spentCents)} pozostało"
    else ->
        "${formatWatchMoney(category.spentCents)} wydano · ${formatWatchMoney(category.spentCents - category.limitCents)} ponad limit"
}

@Composable
private fun Badge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    Surface(onClick = onClick, color = Sage, shape = RoundedCornerShape(20.dp)) {
        Text(
            text = "Odśwież",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = Forest,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Brak danych", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Otwórz Tend na telefonie, aby zsynchronizować.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        RefreshButton(onRetry)
    }
}

private val PolishMonths = listOf(
    "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
    "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień",
)

private fun polishMonth(monthKey: String): String = try {
    val month = YearMonth.parse(monthKey)
    "${PolishMonths[month.monthValue - 1]} ${month.year}"
} catch (_: Exception) {
    monthKey
}
