package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.launch

/** Calling-card button materials. GLASS = apna approved 3D glass (default, untouched rendering). */
enum class BtnKind { GLASS, FLAT, SQUIRCLE, OUTLINE }

/** Ek calling-card style: button material + card palette + background treatment.
 *  bgMode: "keep" = user ka callBgStyle (blobs/photo/solid) jaisa hai; "flat" = saaf bg2->bg gradient;
 *  "force" = style ka apna dark gradient (bgTop/bgBottom). */
data class CallCardStyle(
    val key: String,
    val label: String,
    val desc: String,
    val btnKind: BtnKind,
    val bgMode: String = "keep",
    val bgTop: Color? = null,
    val bgBottom: Color? = null,
    val restyle: (LdPalette) -> LdPalette = { it },
)

/** Forced-dark card: text tokens hamesha readable (light theme pe bhi dark bg ke saath clash nahi). */
private fun darkCard(base: LdPalette, bg: Color, bg2: Color, accent: Color, accent2: Color): LdPalette =
    base.copy(bg = bg, bg2 = bg2, text = Color(0xFFEEF3FF), muted = Color(0x99E6EEFF),
        glass = Color(0x12FFFFFF), glass2 = Color(0x21FFFFFF), stroke = Color(0x2EFFFFFF),
        accent = accent, accent2 = accent2, isLight = false)

val CALL_STYLES: List<CallCardStyle> = listOf(
    CallCardStyle("glass", "Liquid Glass", "Apna pehchan wala 3D glass look - default", BtnKind.GLASS),
    CallCardStyle("google", "Google jaisa", "Saaf flat gol buttons, simple plain background", BtnKind.FLAT, bgMode = "flat"),
    CallCardStyle("odialer", "ODialer jaisa", "Soft squircle buttons, halka rang bhara fill", BtnKind.SQUIRCLE),
    CallCardStyle("oneplus", "OnePlus jaisa", "Halka outline gol buttons, minimal look", BtnKind.OUTLINE),
    CallCardStyle("royalgold", "Royal Gold", "Gehra warm background, sona jaise accents", BtnKind.GLASS, bgMode = "force",
        bgTop = Color(0xFF1A140A), bgBottom = Color(0xFF0C0904),
        restyle = { darkCard(it, Color(0xFF0C0904), Color(0xFF1A140A), Color(0xFFD9B44A), Color(0xFFF5D67B)) }),
    CallCardStyle("emerald", "Emerald", "Gehra hara background, nikhra hua emerald accent", BtnKind.GLASS, bgMode = "force",
        bgTop = Color(0xFF06231A), bgBottom = Color(0xFF020D09),
        restyle = { darkCard(it, Color(0xFF020D09), Color(0xFF06231A), Color(0xFF34D399), Color(0xFF6EE7B7)) }),
    CallCardStyle("sunset", "Sunset Glow", "Garam santra-gulabi rang, shaam jaisa ehsaas", BtnKind.GLASS, bgMode = "force",
        bgTop = Color(0xFF241009), bgBottom = Color(0xFF100503),
        restyle = { darkCard(it, Color(0xFF100503), Color(0xFF241009), Color(0xFFFF8A5B), Color(0xFFFFB199)) }),
    CallCardStyle("amoled", "AMOLED Neon", "Bilkul kaala background (battery bachat), neon buttons", BtnKind.OUTLINE, bgMode = "force",
        bgTop = Color(0xFF000000), bgBottom = Color(0xFF000000),
        restyle = { darkCard(it, Color(0xFF000000), Color(0xFF000000), Color(0xFF22E6A8), Color(0xFF7CF5D3)) }),
    CallCardStyle("frosted", "Frosted Milky", "Glass hi, par zyada doodhiya aur soft", BtnKind.GLASS,
        restyle = { it.copy(glassFrost = 2, glassDepth = 1) }),
    CallCardStyle("retropop", "Retro Pop", "Mazedar pink-peela rang khel, gol full buttons", BtnKind.FLAT, bgMode = "force",
        bgTop = Color(0xFF1B0A1E), bgBottom = Color(0xFF0C0510),
        restyle = { darkCard(it, Color(0xFF0C0510), Color(0xFF1B0A1E), Color(0xFFF472B6), Color(0xFFFBBF24)) }),
    CallCardStyle("mono", "Mono Minimal", "Sirf kaala-safed-grey, bilkul shaant", BtnKind.OUTLINE, bgMode = "force",
        bgTop = Color(0xFF101014), bgBottom = Color(0xFF060608),
        restyle = { darkCard(it, Color(0xFF060608), Color(0xFF101014), Color(0xFFB9C0CC), Color(0xFF8A93A3)) }),
)

val LocalCallStyle = staticCompositionLocalOf { CALL_STYLES[0] }

fun callStyleFor(key: String): CallCardStyle = CALL_STYLES.firstOrNull { it.key == key } ?: CALL_STYLES[0]

/** Flat solid disc (Google dialer jaisa): koi border/halo nahi, halka vertical shade. */
fun Modifier.flatDisc(p: LdPalette, shape: Shape, on: Boolean, tint: Color): Modifier = this
    .clip(shape)
    .background(
        if (on) Brush.verticalGradient(listOf(tint, mix(tint, Color.Black, .84f)))
        else Brush.verticalGradient(listOf(
            if (p.isLight) Color(0x150F172A) else Color.White.copy(alpha = .13f),
            if (p.isLight) Color(0x0C0F172A) else Color.White.copy(alpha = .08f)))
    )

/** Sirf outline disc (OnePlus jaisa): transparent andar, patla ring; on pe accent fill. */
fun Modifier.outlineDisc(p: LdPalette, shape: Shape, on: Boolean, tint: Color): Modifier = this
    .clip(shape)
    .background(if (on) Brush.verticalGradient(listOf(tint, mix(tint, Color.Black, .84f))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
    .border(1.5.dp, if (on) tint.copy(alpha = .0f) else if (p.isLight) Color(0x380F172A) else Color.White.copy(alpha = .40f), shape)

// ============================= Recents row styles =============================

enum class RecentsRowKind { GLASS, FLAT, OUTLINE, SQUIRCLE }

/** Recents row ka look: shell material + mini call/video buttons ka tint. Key CALL_STYLES se milti hai. */
data class RecentsRowSpec(
    val key: String,
    val label: String,
    val desc: String,
    val kind: RecentsRowKind,
    val tint: Color? = null,
)

val RECENTS_STYLES: List<RecentsRowSpec> = listOf(
    RecentsRowSpec("glass", "Liquid Glass", "Apna default glass card - jaisa ab hai", RecentsRowKind.GLASS),
    RecentsRowSpec("google", "Google jaisa", "Bina card ki saaf list - seedhi aur halki", RecentsRowKind.FLAT),
    RecentsRowSpec("odialer", "ODialer jaisa", "Naram squircle card, halka accent fill", RecentsRowKind.SQUIRCLE),
    RecentsRowSpec("oneplus", "OnePlus jaisa", "Patli outline wala row, minimal", RecentsRowKind.OUTLINE),
    RecentsRowSpec("royalgold", "Royal Gold", "Glass card, sone jaise call buttons", RecentsRowKind.GLASS, Color(0xFFD9B44A)),
    RecentsRowSpec("emerald", "Emerald", "Glass card, emerald call buttons", RecentsRowKind.GLASS, Color(0xFF34D399)),
    RecentsRowSpec("sunset", "Sunset Glow", "Glass card, garam santra call buttons", RecentsRowKind.GLASS, Color(0xFFFF8A5B)),
    RecentsRowSpec("amoled", "AMOLED Neon", "Patla neon outline row, neon call buttons", RecentsRowKind.OUTLINE, Color(0xFF22E6A8)),
    RecentsRowSpec("frosted", "Frosted Milky", "Doodhiya white glass card", RecentsRowKind.GLASS, Color(0xFFEAF2FF)),
    RecentsRowSpec("retropop", "Retro Pop", "Squircle card, pink call buttons", RecentsRowKind.SQUIRCLE, Color(0xFFF472B6)),
    RecentsRowSpec("mono", "Mono Minimal", "Grey outline row, grey call buttons", RecentsRowKind.OUTLINE, Color(0xFFB9C0CC)),
)

fun recentsStyleFor(key: String): RecentsRowSpec = RECENTS_STYLES.firstOrNull { it.key == key } ?: RECENTS_STYLES[0]

/** Recents ka styled row: GlassRow ka bhai, style ke hisaab se shell. GLASS = bilkul GlassRow jaisa. */
@Composable
fun RecentsRowShell(spec: RecentsRowSpec, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, dense: Boolean = false,
                    modifier: Modifier = Modifier, extraVerticalPadding: Dp = 0.dp, tight: Boolean = false,
                    content: @Composable RowScope.() -> Unit) {
    val p = LocalLd.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .985f)
    val shape = if (spec.kind == RecentsRowKind.SQUIRCLE) RoundedCornerShape(14.dp) else RoundedCornerShape(20.dp)
    val shell = when (spec.kind) {
        RecentsRowKind.GLASS -> Modifier.glass(p, shape, 8.dp, .20f)
        RecentsRowKind.FLAT -> Modifier
        RecentsRowKind.OUTLINE -> Modifier
            .clip(shape)
            .background((spec.tint ?: p.accent).copy(alpha = if (p.isLight) .05f else .07f))
            .border(1.dp, (spec.tint ?: p.accent).copy(alpha = if (p.isLight) .30f else .38f), shape)
        RecentsRowKind.SQUIRCLE -> Modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(
                (spec.tint ?: p.accent).copy(alpha = if (p.isLight) .10f else .14f),
                (spec.tint ?: p.accent).copy(alpha = if (p.isLight) .06f else .09f))))
    }
    Row(modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s }.then(shell)
        .combinedClickable(src, null, onLongClick = onLongClick, onClick = onClick)
        .padding(horizontal = if (tight) 10.dp else 12.dp, vertical = (if (dense) 3.dp else 6.dp) + extraVerticalPadding),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (tight) 8.dp else 12.dp), content = content)
}

// ============================= Style Studio (chooser) =============================

private fun saveStyle(key: androidx.datastore.preferences.core.Preferences.Key<String>, v: String) {
    LdApp.instance.scope.launch { LdApp.instance.repo.set(key, v) }
}

@Composable
private fun CardStylePreview(style: CallCardStyle) {
    val base = LocalLd.current
    val sp = style.restyle(base)
    val bgBrush = when (style.bgMode) {
        "force" -> Brush.verticalGradient(listOf(style.bgTop ?: sp.bg2, style.bgBottom ?: sp.bg))
        else -> Brush.verticalGradient(listOf(sp.bg2, sp.bg))
    }
    CompositionLocalProvider(LocalLd provides sp, LocalCallStyle provides style) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(bgBrush)
            .padding(vertical = 14.dp).testTag("card_style_preview"), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar("Asha Verma", null, 54.dp, 17f, seed = "style-preview")
            LdText("Asha Verma", 15f, FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            LdText("00:42", 13f, FontWeight.SemiBold, sp.muted, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CallBtn(LdIcons.Mic, "Mute") {}
                CallBtn(LdIcons.Volume, "Speaker", on = true, flat = true) {}
                CallBtn(LdIcons.Dialpad, "Keypad") {}
            }
            Spacer(Modifier.height(12.dp))
            EndCallBtn(52.dp) {}
        }
    }
}

@Composable
private fun RecentsStylePreview(spec: RecentsRowSpec) {
    val p = LocalLd.current
    Column(Modifier.fillMaxWidth().testTag("recents_style_preview")) {
        RecentsRowShell(spec, onClick = {}, dense = false, extraVerticalPadding = 4.dp, tight = true) {
            Avatar("Asha Verma", null, 38.dp, 13f, seed = "style-preview")
            Column(Modifier.weight(1f)) {
                LdText("Asha Verma", 14.5f, FontWeight.SemiBold, p.text, maxLines = 2)
                LdText("Incoming · 11:04 AM · 2:14 · SIM 1", 11f, color = p.muted, maxLines = 2)
            }
            VideoMini(40.dp, tint = spec.tint) {}
            CallMini(40.dp, tint = spec.tint) {}
        }
    }
}

@Composable
private fun StylePickRow(title: String, desc: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val p = LocalLd.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(if (selected) p.accent.pct(.16f) else Color.Transparent)
        .border(1.dp, if (selected) p.accent else Color.Transparent, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(18.dp).clip(CircleShape)
            .border(2.dp, if (selected) p.accent else p.muted, CircleShape)
            .background(if (selected) p.accent else Color.Transparent, CircleShape))
        Column(Modifier.weight(1f)) {
            LdText(title, 13.5f, FontWeight.SemiBold)
            LdText(desc, 11f, color = p.muted, maxLines = 3)
        }
        if (selected) LdText("Chuna hua", 10.5f, FontWeight.Bold, p.accent)
    }
}

/** Style Studio: 11 calling-card styles + 11 recents styles, live preview ke saath. */
@Composable
fun StyleStudioPage(onBack: () -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val cardStyle = callStyleFor(s.callCardStyle)
    val rowStyle = recentsStyleFor(s.recentsStyle)
    PageFrame("Call & Recents style", onBack) {
        LdText("CALLING CARD", 12f, FontWeight.Bold, p.muted, letter = 1.2f, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        CardStylePreview(cardStyle)
        LdText("Lal cut button har style me elongated pill hi rahega.", 11f, color = p.muted, modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 8.dp))
        GlassCard {
            CALL_STYLES.forEachIndexed { i, st ->
                StylePickRow(st.label + if (st.key == "glass") " (default)" else "", st.desc, st.key == cardStyle.key, "card_style_" + st.key) { saveStyle(K.callCardStyle, st.key) }
                if (i < CALL_STYLES.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        LdText("RECENTS LIST", 12f, FontWeight.Bold, p.muted, letter = 1.2f, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        RecentsStylePreview(rowStyle)
        Spacer(Modifier.height(8.dp))
        GlassCard {
            RECENTS_STYLES.forEachIndexed { i, st ->
                StylePickRow(st.label + if (st.key == "glass") " (default)" else "", st.desc, st.key == rowStyle.key, "recents_style_" + st.key) { saveStyle(K.recentsStyle, st.key) }
                if (i < RECENTS_STYLES.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        LdText("Style turant lagta hai - agli call aur recents list dono me. Aapka glass default waisa ka waisa hai.", 12f, color = p.muted, modifier = Modifier.padding(start = 4.dp))
    }
}
