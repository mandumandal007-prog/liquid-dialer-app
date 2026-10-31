package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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

/** Side rail: tap ya drag se letter par jump. entries = AlphaIndex.indexOf ka output. */
@Composable
fun AlphaRail(entries: List<Pair<String, Int>>, modifier: Modifier = Modifier, onJump: (Int) -> Unit) {
    val p = LocalLd.current
    var railH by remember { mutableStateOf(1f) }
    fun jumpAt(y: Float) {
        if (entries.isEmpty() || railH <= 0f) return
        val i = ((y / railH) * entries.size).toInt().coerceIn(0, entries.size - 1)
        onJump(entries[i].second)
    }
    Column(
        modifier.width(22.dp)
            .onGloballyPositioned { railH = it.size.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(entries) { detectTapGestures { jumpAt(it.y) } }
            .pointerInput(entries) { detectVerticalDragGestures { change, _ -> change.consume(); jumpAt(change.position.y) } }
            .clip(RoundedCornerShape(11.dp))
            .background(p.glass.copy(alpha = .6f))
            .padding(vertical = 6.dp)
            .testTag("alpha_rail"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        entries.forEach { (letter, _) ->
            LdText(letter, 9.5f, FontWeight.Bold, p.accent, Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
        }
    }
}
