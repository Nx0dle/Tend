package com.tend.wear

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tend.shared.TendPalette

// Wear mirrors the phone palette from the shared module, so the two surfaces stay in sync.
internal val Porcelain = Color(TendPalette.PORCELAIN)
internal val Ink = Color(TendPalette.INK)
internal val Forest = Color(TendPalette.FOREST)
internal val DeepForest = Color(TendPalette.DEEP_FOREST)
internal val Sage = Color(TendPalette.SAGE)
internal val Gold = Color(TendPalette.GOLD)
internal val Muted = Color(TendPalette.MUTED)
internal val Line = Color(TendPalette.LINE)
internal val Danger = Color(TendPalette.DANGER)
internal val NoticeText = Color(TendPalette.NOTICE)

@Composable
internal fun TendWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest,
            onPrimary = Color.White,
            secondary = Gold,
            onSecondary = Ink,
            background = Porcelain,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink,
            outline = Line,
            error = Danger,
        ),
        content = content,
    )
}
