package com.tend.wear

import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.tend.shared.TendPalette
import com.tend.shared.WatchCategory
import com.tend.shared.WatchSnapshot
import com.tend.shared.formatWatchMoney
import com.tend.shared.formatWatchMoneyCompact
import com.tend.shared.limitRatio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick-view Tile: month total plus category progress bars, mirroring the Start tab. A Tile cannot
 * scroll, so it shows as many rows as fit and points to the app for the full list.
 */
class TendTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = CallbackToFutureAdapter.getFuture { completer ->
        serviceScope.launch {
            // Never complete exceptionally: a thrown tile request makes the system render an empty
            // (black) tile. Fall back to the no-data layout instead.
            val snapshot = try {
                WatchSnapshotStore.read(applicationContext)
            } catch (throwable: Throwable) {
                Log.w(TAG, "snapshot read failed", throwable)
                null
            }
            val tile = try {
                buildTile(requestParams, snapshot)
            } catch (throwable: Throwable) {
                Log.e(TAG, "tile build failed", throwable)
                buildTile(requestParams, null)
            }
            completer.set(tile)
        }
        "TendTileService#onTileRequest"
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { completer ->
            completer.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())
            "TendTileService#onTileResourcesRequest"
        }

    private fun buildTile(request: RequestBuilders.TileRequest, snapshot: WatchSnapshot?): TileBuilders.Tile {
        val screenWidthDp = request.deviceConfiguration.screenWidthDp
        val screenHeightDp = request.deviceConfiguration.screenHeightDp
        val contentWidthDp = (screenWidthDp.toFloat() - 2f * H_PADDING_DP).coerceAtLeast(96f)
        val rowCapacity = (((screenHeightDp.toFloat() - HEADER_HEIGHT_DP) / ROW_HEIGHT_DP).toInt()).coerceAtLeast(1)
        val layout = if (snapshot == null) emptyState() else content(snapshot, contentWidthDp, rowCapacity)
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    private fun content(
        snapshot: WatchSnapshot,
        contentWidthDp: Float,
        rowCapacity: Int,
    ): LayoutElementBuilders.LayoutElement {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    // Tiles get no background from the system, and Tend's palette is a light theme
                    // (dark ink on porcelain). Without this the dark text lands on black and the
                    // tile looks empty.
                    .setBackground(solidBackground(TendPalette.PORCELAIN))
                    .setPadding(ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(H_PADDING_DP)).build())
                    .setClickable(openAppClickable())
                    .build(),
            )
        column.addContent(styledText("WYDATKI W MIESIĄCU", TendPalette.MUTED, 10f, bold = true))
        column.addContent(styledText(formatWatchMoney(snapshot.totalSpentCents), TendPalette.INK, 26f, bold = true))
        if (snapshot.hasPool) {
            column.addContent(
                styledText(
                    "Pula ${formatWatchMoneyCompact(snapshot.availableCents)} · Limity ${formatWatchMoneyCompact(snapshot.allocatedCents)}",
                    TendPalette.MUTED,
                    11f,
                ),
            )
        }

        val visible = snapshot.categories.take(rowCapacity)
        if (visible.isEmpty()) {
            column.addContent(styledText("Brak kategorii.", TendPalette.MUTED, 12f))
        } else {
            visible.forEach { column.addContent(categoryRow(it, contentWidthDp)) }
        }
        val hidden = snapshot.categories.size - visible.size
        if (hidden > 0) {
            column.addContent(styledText("+$hidden więcej w apce Tend", TendPalette.MUTED, 11f))
        }
        return column.build()
    }

    private fun categoryRow(
        category: WatchCategory,
        contentWidthDp: Float,
    ): LayoutElementBuilders.LayoutElement {
        val hasLimit = category.limitCents > 0L
        val ratio = category.limitRatio()
        val progressColor = when {
            hasLimit && category.spentCents >= category.limitCents -> TendPalette.DANGER
            ratio >= 0.8f -> TendPalette.GOLD
            else -> category.colorArgb
        }
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(ModifiersBuilders.Padding.Builder().setTop(DimensionBuilders.dp(6f)).build())
                    .build(),
            )
        column.addContent(
            LayoutElementBuilders.Row.Builder()
                .setWidth(DimensionBuilders.expand())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(dot(category.colorArgb))
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.weight(1f))
                        .addContent(styledText(" ${category.name}", TendPalette.INK, 13f, bold = true))
                        .build(),
                )
                .addContent(styledText(amountLabel(category, hasLimit), TendPalette.INK, 12f, bold = true))
                .build(),
        )
        column.addContent(styledText(caption(category, hasLimit), TendPalette.MUTED, 10f))
        if (hasLimit) {
            column.addContent(progressBar(ratio, progressColor, contentWidthDp))
        }
        return column.build()
    }

    private fun progressBar(
        ratio: Float,
        colorArgb: Long,
        widthDp: Float,
    ): LayoutElementBuilders.LayoutElement {
        val track = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(widthDp))
            .setHeight(DimensionBuilders.dp(5f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(background(TendPalette.LINE, 2.5f))
                    .build(),
            )
        if (ratio > 0f) {
            track.addContent(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.dp((widthDp * ratio).coerceAtLeast(2f)))
                    .setHeight(DimensionBuilders.dp(5f))
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setBackground(background(colorArgb, 2.5f))
                            .build(),
                    )
                    .build(),
            )
        }
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(ModifiersBuilders.Padding.Builder().setTop(DimensionBuilders.dp(4f)).build())
                    .build(),
            )
            .addContent(track.build())
            .build()
    }

    private fun emptyState(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(solidBackground(TendPalette.PORCELAIN))
                    .setPadding(ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(H_PADDING_DP)).build())
                    .setClickable(openAppClickable())
                    .build(),
            )
            .addContent(styledText("Tend", TendPalette.FOREST, 16f, bold = true))
            .addContent(styledText("Brak danych z telefonu.", TendPalette.INK, 13f))
            .addContent(styledText("Otwórz Tend na telefonie, aby zsynchronizować.", TendPalette.MUTED, 11f))
            .build()

    private fun dot(argb: Long): LayoutElementBuilders.Box =
        LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(8f))
            .setHeight(DimensionBuilders.dp(8f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(background(argb, 4f))
                    .build(),
            )
            .build()

    private fun solidBackground(argb: Long): ModifiersBuilders.Background =
        ModifiersBuilders.Background.Builder()
            .setColor(ColorBuilders.argb(argb.toInt()))
            .build()

    private fun background(argb: Long, cornerRadiusDp: Float): ModifiersBuilders.Background =
        ModifiersBuilders.Background.Builder()
            .setColor(ColorBuilders.argb(argb.toInt()))
            .setCorner(
                ModifiersBuilders.Corner.Builder()
                    .setRadius(DimensionBuilders.dp(cornerRadiusDp))
                    .build(),
            )
            .build()

    private fun styledText(
        value: String,
        argb: Long,
        sizeSp: Float,
        bold: Boolean = false,
    ): LayoutElementBuilders.Text =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(1)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setColor(ColorBuilders.argb(argb.toInt()))
                    .setSize(DimensionBuilders.sp(sizeSp))
                    .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                    .build(),
            )
            .build()

    private fun openAppClickable(): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setId("open-tend")
            // LaunchAction is the Action that carries an AndroidActivity; the PendingIntent
            // overload of setOnClick throws unless the builder came from a ProtoLayoutScope, which
            // is what kept the whole tile from rendering and left it black.
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()

    private fun amountLabel(category: WatchCategory, hasLimit: Boolean): String =
        if (hasLimit) {
            "${formatWatchMoneyCompact(category.spentCents)} / ${formatWatchMoneyCompact(category.limitCents)}"
        } else {
            formatWatchMoneyCompact(category.spentCents)
        }

    private fun caption(category: WatchCategory, hasLimit: Boolean): String = when {
        !hasLimit -> "bez limitu"
        category.spentCents > category.limitCents ->
            "ponad limit o ${formatWatchMoneyCompact(category.spentCents - category.limitCents)}"
        else -> "zostało ${formatWatchMoneyCompact(category.limitCents - category.spentCents)}"
    }

    private companion object {
        const val TAG = "TendWear"
        const val RESOURCES_VERSION = "1"
        const val FRESHNESS_MILLIS = 60_000L
        const val H_PADDING_DP = 12f
        const val HEADER_HEIGHT_DP = 72f
        const val ROW_HEIGHT_DP = 40f
    }
}
