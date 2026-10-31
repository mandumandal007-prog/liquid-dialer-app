package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
/** Contacts list ka A-Z side rail: pure index helper + Compose rail UI.
 *  Koi permission/setting nahi - sirf ek scroll affordance (Google Dialer jaisi). */
object AlphaIndex {

    /** Rail letter: pehla A-Z Latin letter (uppercase), Devanagari akshar as-is, warna '#'. */
    fun letterOf(name: String): String {
        val c = name.trim().firstOrNull { it.isLetterOrDigit() } ?: return "#"
        return when {
            c.uppercaseChar() in 'A'..'Z' -> c.uppercaseChar().toString()
            c in 'ऀ'..'ॿ' -> c.toString()
            else -> "#"
        }
    }

    /** Displayed names (list order) se distinct letter -> us letter ka pehla LazyColumn item index.
     *  headItems = list ke top fixed items (SectionHead etc.) ka count - index wahi se offset hota hai. */
    fun indexOf(names: List<String>, headItems: Int): List<Pair<String, Int>> {
        val out = LinkedHashMap<String, Int>()
        names.forEachIndexed { i, n -> out.putIfAbsent(letterOf(n), i + headItems) }
        return out.toList()
    }
}

/** Side rail: tap ya drag se letter par jump. entries = AlphaIndex.indexOf ka output.
 *  Har letter ko EXACT uniform slot milta hai: rail available height me fit hota hai
 *  (32+ letters par bhi aakhri letter clip nahi hota), aur tap ka letter mapping
 *  slot division se exact hota hai (pehle uniform content-height division edge
 *  letters par ~2 entry short jump karta tha). Kam letters par slot 18.dp tak cap
 *  hai taaki rail compact hi rahe. */
@Composable
fun AlphaRail(entries: List<Pair<String, Int>>, modifier: Modifier = Modifier, onJump: (Int) -> Unit) {
    val p = LocalLd.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val n = entries.size.coerceAtLeast(1)
        val slotH = (if (constraints.hasBoundedHeight) maxHeight / n else 16.dp).coerceAtMost(18.dp)
        val slotPx = with(density) { slotH.toPx() }
        // Letter size slot se derive: dp->sp conversion fontScale ko dhyan me rakhti hai,
        // isliye bade system font par bhi glyph slot ke andar rehta hai (clip nahi).
        val rawSize = with(density) { (slotH * .5f).toSp() }
        val letterSize = if (rawSize.value > 9.5f) 9.5.sp else rawSize
        fun jumpAt(y: Float) {
            if (entries.isEmpty() || slotPx <= 0f) return
            val i = (y / slotPx).toInt().coerceIn(0, entries.size - 1)
            onJump(entries[i].second)
        }
        Column(
            Modifier.height(slotH * n).width(22.dp)
                .pointerInput(entries, slotPx) { detectTapGestures { jumpAt(it.y) } }
                .pointerInput(entries, slotPx) { detectVerticalDragGestures { change, _ -> change.consume(); jumpAt(change.position.y) } }
                .clip(RoundedCornerShape(11.dp))
                .background(p.glass.copy(alpha = .6f))
                .testTag("alpha_rail"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            entries.forEach { pair ->
                Box(Modifier.height(slotH).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(pair.first, color = p.accent, fontSize = letterSize, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false,
                        style = TextStyle(lineHeight = letterSize * 1.2f,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)))
                }
            }
        }
    }
}
