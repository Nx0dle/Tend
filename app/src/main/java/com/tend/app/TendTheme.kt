package com.tend.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Base palette.
internal val Porcelain = Color(0xFFF5F3EC)
internal val Ink = Color(0xFF18332D)
internal val Forest = Color(0xFF176451)
internal val DeepForest = Color(0xFF103E35)
internal val Sage = Color(0xFFDCEBE2)
internal val Gold = Color(0xFFC69A52)
internal val Muted = Color(0xFF64766F)
internal val Line = Color(0xFFDCE4DE)
internal val Danger = Color(0xFFB6534A)

/**
 * Semantic accents. These carry meaning (hero surfaces, allocation, states), so they live here
 * instead of being written inline. Do not add raw hex colors to screens.
 */
internal val OnHeroMuted = Color(0xFFC5D1C5)
internal val Placeholder = Color(0xFFABB3AD)
internal val Reserve = Color(0xFFD38B61)
internal val Overspend = Color(0xFFE58669)
internal val NoticeText = Color(0xFF8B6420)

/** Canonical layout rhythm shared by screens, dialogs, and calendar. */
internal object TendSpace {
    val xsmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xlarge = 24.dp
    val xxlarge = 32.dp
}

/** Canonical corner shapes. Use control for fields, card for content, panel for hero surfaces. */
internal object TendRadius {
    val small = 8.dp
    val control = 14.dp
    val card = 20.dp
    val panel = 28.dp
}

/** Canonical component heights. Control satisfies the 44 dp minimum touch target. */
internal object TendSize {
    val control = 44.dp
    val iconButton = 44.dp
    /** Visible size for icons that keep a larger [iconButton] touch target. */
    val iconVisual = 32.dp
}

internal object TendMotion {
    const val quick = 140
    const val standard = 220
    const val expressive = 360
    const val progress = 450
}

/**
 * Canonical type roles. Use these for repeated UI text instead of ad-hoc font sizes so screens,
 * dialogs, and cards stay typographically aligned.
 */
internal object TendType {
    val heroLabel = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
    val heroAmount = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
    val heroAmountCompact = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
    val monthTitle = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
    val sectionTitle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val amount = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
    val rowTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val rowAmount = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val controlLabel = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val compactLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(fontSize = 12.sp)
    val micro = TextStyle(fontSize = 11.sp)
    val metricValue = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val badge = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val navLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
internal fun TendTheme(content: @Composable () -> Unit) {
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
