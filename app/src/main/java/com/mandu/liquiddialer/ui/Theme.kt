package com.mandu.liquiddialer.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Colour tokens copied from styles.css (:root and [data-theme=...]). */
@Immutable
data class LdPalette(
    val key: String,
    val name: String,
    val bg: Color,
    val bg2: Color,
    val text: Color,
    val muted: Color,
    val glass: Color,
    val glass2: Color,
    val stroke: Color,
    val accent: Color,
    val accent2: Color = Color(0xFFA06BFF),
    val danger: Color = Color(0xFFFF5D6C),
    val ok: Color = Color(0xFF3DDC97),
    val isLight: Boolean = false,
    val blobAlpha: Float = .55f,
    val previewA: Color,
    val previewB: Color,
    val blur: Float = 22f,
    val motion: Boolean = true,
)

fun mix(a: Color, b: Color, aPart: Float): Color = lerp(b, a, aPart)
/** css color-mix(in srgb, c p%, transparent) */
fun Color.pct(p: Float): Color = this.copy(alpha = this.alpha * p)

val ACCENTS = listOf(Color(0xFF5B8CFF), Color(0xFFA06BFF), Color(0xFF2EE6A8), Color(0xFFFF8A5B), Color(0xFFF5C542))

fun paletteFor(theme: String, accent: Color, systemDark: Boolean): LdPalette {
    val t = if (theme == "auto") (if (systemDark) "midnight" else "daylight") else theme
    return when (t) {
        "aurora" -> LdPalette("aurora", "Aurora", Color(0xFF04120F), Color(0xFF071F1A), Color(0xFFEAFFF9), Color(0x99E2FFF6),
            Color(0x12FFFFFF), Color(0x21FFFFFF), Color(0x2EB4FFE6), accent, previewA = Color(0xFF04120F), previewB = Color(0xFF0F5C4A))
        "sunset" -> LdPalette("sunset", "Sunset", Color(0xFF140708), Color(0xFF200D0A), Color(0xFFFFF3EC), Color(0x99FFECE2),
            Color(0x14FFFFFF), Color(0x24FFFFFF), Color(0x33FFCDB4), accent, previewA = Color(0xFF1A0708), previewB = Color(0xFF7A2C1B))
        "daylight" -> LdPalette("daylight", "Daylight", Color(0xFFE9EEFB), Color(0xFFF7F9FF), Color(0xFF0B1224), Color(0x940F172A),
            Color(0x80FFFFFF), Color(0xC7FFFFFF), Color(0xF2FFFFFF), accent, isLight = true, blobAlpha = .38f,
            previewA = Color(0xFFFFFFFF), previewB = Color(0xFFC9D8FF))
        else -> LdPalette("midnight", "Midnight", Color(0xFF070A12), Color(0xFF0B1120), Color(0xFFEEF3FF), Color(0x99E6EEFF),
            Color(0x12FFFFFF), Color(0x21FFFFFF), Color(0x29FFFFFF), accent, previewA = Color(0xFF0B1020), previewB = Color(0xFF1D2B5C))
    }
}

val THEME_KEYS = listOf("midnight", "aurora", "sunset", "daylight")

val LocalLd = staticCompositionLocalOf { paletteFor("midnight", ACCENTS[0], true) }
