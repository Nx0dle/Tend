package com.tend.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Porcelain = Color(0xFFF5F3EC)
internal val Ink = Color(0xFF18332D)
internal val Forest = Color(0xFF176451)
internal val DeepForest = Color(0xFF103E35)
internal val Sage = Color(0xFFDCEBE2)
internal val Gold = Color(0xFFC69A52)
internal val Muted = Color(0xFF64766F)
internal val Line = Color(0xFFDCE4DE)
internal val Danger = Color(0xFFB6534A)

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
