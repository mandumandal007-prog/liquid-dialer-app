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

private val LocalBaseFontScale=staticCompositionLocalOf<Float?>{null}

@Composable
fun LdThemeHost(content: @Composable () -> Unit) {
    val s by LdApp.settings.collectAsState()
    LdThemeContent(s,content)
}

@Composable
fun LdThemeContent(s:com.mandu.liquiddialer.data.LdSettings,content:@Composable ()->Unit){
    val dark = isSystemInDarkTheme()
    val pal = remember(s.theme, s.accent, s.blur, s.motion, s.glassDepth, s.glassFrost, s.cleanGlass, dark) {
        paletteFor(s.theme, Color(s.accent.toInt()), dark).copy(blur = if(s.cleanGlass)0f else s.blur.toFloat(), motion = s.motion && !s.cleanGlass, glassDepth = if(s.cleanGlass)1 else s.glassDepth, glassFrost = if(s.cleanGlass)2 else s.glassFrost)
    }
    val d = LocalDensity.current
    val baseFont=LocalBaseFontScale.current ?: d.fontScale
    val scheme = if (pal.isLight) lightColorScheme(primary = pal.accent, background = pal.bg, surface = pal.bg2, onSurface = pal.text, onBackground = pal.text)
                 else darkColorScheme(primary = pal.accent, background = pal.bg, surface = pal.bg2, onSurface = pal.text, onBackground = pal.text)
    CompositionLocalProvider(LocalBaseFontScale provides baseFont, LocalQuietRows provides s.quietRows, LocalRefinedHeaders provides s.refinedHeaders, LocalLang provides s.lang, LocalLd provides pal, LocalBold provides s.boldText, LocalFontStyle provides s.fontStyle, LocalIconStyle provides s.iconStyle, LocalDensity provides Density(d.density, baseFont * s.fontScale)) {
        MaterialTheme(colorScheme = scheme) { content() }
    }
}
