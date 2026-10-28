package com.mandu.liquiddialer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.mandu.liquiddialer.LdApp

@Composable
fun LdThemeHost(content: @Composable () -> Unit) {
    val s by LdApp.settings.collectAsState()
    val dark = isSystemInDarkTheme()
    val pal = remember(s.theme, s.accent, s.blur, s.motion, dark) {
        paletteFor(s.theme, Color(s.accent.toInt()), dark).copy(blur = s.blur.toFloat(), motion = s.motion)
    }
    val d = LocalDensity.current
    val scheme = if (pal.isLight) lightColorScheme(primary = pal.accent, background = pal.bg, surface = pal.bg2, onSurface = pal.text, onBackground = pal.text)
                 else darkColorScheme(primary = pal.accent, background = pal.bg, surface = pal.bg2, onSurface = pal.text, onBackground = pal.text)
    CompositionLocalProvider(LocalLd provides pal, LocalDensity provides Density(d.density, d.fontScale * s.fontScale)) {
        MaterialTheme(colorScheme = scheme) { content() }
    }
}
