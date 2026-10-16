package com.tend.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val Porcelain = Color(0xFFF5F3EC)
internal val Ink = Color(0xFF18332D)
internal val Forest = Color(0xFF176451)
internal val DeepForest = Color(0xFF103E35)
internal val Sage = Color(0xFFDCEBE2)
internal val Gold = Color(0xFFC69A52)
internal val Muted = Color(0xFF64766F)
internal val Line = Color(0xFFDCE4DE)
internal val Danger = Color(0xFFB6534A)

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

/** Canonical component heights. Control is the shared height for compact pill controls. */
internal object TendSize {
    val control = 40.dp
}

internal object TendMotion {
    const val quick = 140
    const val standard = 220
    const val expressive = 360
    const val progress = 450
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
