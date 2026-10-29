package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.DEFAULT_BOTTOM
import com.mandu.liquiddialer.data.DEFAULT_GRID
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.launch

/** One button on the in-call card. */
data class BtnSpec(val icon: ImageVector, val label: String, val on: Boolean = false, val enabled: Boolean = true, val onClick: () -> Unit)

val GRID_IDS = listOf("mute", "keypad", "record", "hold", "add", "merge", "audio")
val BOTTOM_IDS = listOf("speaker", "end", "notes")

fun btnName(id: String) = when (id) {
    "mute" -> "Mute"; "keypad" -> "Keypad"; "record" -> "Record"; "hold" -> "Hold"; "add" -> "Add call"
    "merge" -> "Merge / Swap"; "audio" -> "Audio route"; "speaker" -> "Speaker"; "end" -> "End (Cut)"; "notes" -> "Notes"; else -> id
}

fun btnIcon(id: String): ImageVector = when (id) {
    "mute" -> LdIcons.Mic; "keypad" -> LdIcons.Dialpad; "record" -> LdIcons.Record; "hold" -> LdIcons.Pause; "add" -> LdIcons.Plus
    "merge" -> LdIcons.Merge; "audio" -> LdIcons.Bluetooth; "speaker" -> LdIcons.Volume; "end" -> LdIcons.PhoneOff; else -> LdIcons.Edit
}

/** "a,b,!c" -> [(a,true),(b,true),(c,false)], always containing every known id exactly once. */
fun parseLayout(s: String, all: List<String>): List<Pair<String, Boolean>> {
    val out = mutableListOf<Pair<String, Boolean>>()
    s.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { t ->
        val id = t.removePrefix("!")
        if (id in all && out.none { it.first == id }) out.add(id to !t.startsWith("!"))
    }
    all.filter { id -> out.none { it.first == id } }.forEach { out.add(it to true) }
    return out.map { if (it.first == "end") it.first to true else it }
}

fun encodeLayout(l: List<Pair<String, Boolean>>) = l.joinToString(",") { (if (it.second) "" else "!") + it.first }

/** Renders the configurable grid + bottom row. Sizes scale with the screen and the user's size choice. */
@Composable
fun CallControlsLayout(grid: List<Pair<String, Boolean>>, bottom: List<Pair<String, Boolean>>, gridScale: Float, bottomScale: Float,
                       spec: (String) -> BtnSpec?, onEnd: () -> Unit, compact: Boolean = false) {
    val h = LocalConfiguration.current.screenHeightDp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val w = maxWidth.value
        val base = minOf(62f, w / 3f * .52f, h * (if (compact) .07f else .082f))
        val gsz = (base * gridScale).dp
        val bsz = (base * bottomScale).dp
        val gap = (gsz.value * .2f).coerceIn(6f, 16f).dp
        val cell = (w / 3f).dp
        Column(verticalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
            val vis = grid.filter { it.second }.mapNotNull { spec(it.first) }
            vis.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    row.forEach { b -> Box(Modifier.width(cell), contentAlignment = Alignment.Center) { CallBtn(b.icon, b.label, b.on, gsz, b.enabled, b.onClick) } }
                }
            }
            Spacer(Modifier.height(gap / 2))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top) {
                bottom.filter { it.second || it.first == "end" }.forEach { (id, _) ->
                    Box(Modifier.width(cell), contentAlignment = Alignment.TopCenter) {
                        if (id == "end") EndCallBtn(bsz * 1.12f, onEnd)
                        else spec(id)?.let { b -> CallBtn(b.icon, b.label, b.on, bsz, b.enabled, b.onClick) }
                    }
                }
            }
        }
    }
}

private fun saveF(k: androidx.datastore.preferences.core.Preferences.Key<String>, v: String) { LdApp.instance.scope.launch { LdApp.instance.repo.set(k, v) } }
private fun saveFl(k: androidx.datastore.preferences.core.Preferences.Key<Float>, v: Float) { LdApp.instance.scope.launch { LdApp.instance.repo.set(k, v) } }

@Composable
fun CallLayoutPage(onBack: () -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val grid = remember(s.layoutGrid) { parseLayout(s.layoutGrid, GRID_IDS) }
    val bottom = remember(s.layoutBottom) { parseLayout(s.layoutBottom, BOTTOM_IDS) }
    var gs by remember(s.gridScale) { mutableFloatStateOf(s.gridScale) }
    var bs by remember(s.bottomScale) { mutableFloatStateOf(s.bottomScale) }
    fun move(l: List<Pair<String, Boolean>>, i: Int, d: Int): List<Pair<String, Boolean>> {
        val j = i + d; if (j !in l.indices) return l
        val m = l.toMutableList(); val t = m[i]; m[i] = m[j]; m[j] = t; return m
    }
    PageFrame("Calling card layout", onBack) {
        LdText("PREVIEW", 12f, FontWeight.Bold, p.muted, letter = 1.2f, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(p.accent.pct(.10f)).glass(p, RoundedCornerShape(28.dp), 0.dp)
            .padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar("Preview", null, 56.dp, 18f, seed = "preview")
            LdText("Call card", 15f, FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            LdText("00:42", 13f, FontWeight.SemiBold, p.muted, modifier = Modifier.padding(bottom = 14.dp))
            CallControlsLayout(grid, bottom, gs, bs, spec = { id -> BtnSpec(btnIcon(id), btnName(id).substringBefore(" /")) {} }, onEnd = {}, compact = true)
        }

        Spacer(Modifier.height(16.dp))
        LdText("UPAR KE BUTTON (grid) · order aur dikhana", 12f, FontWeight.Bold, p.muted, letter = 1f, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        GlassCard {
            grid.forEachIndexed { i, (id, on) ->
                LayoutRow(id, on, i > 0, i < grid.lastIndex, true,
                    onUp = { saveF(K.layoutGrid, encodeLayout(move(grid, i, -1))) },
                    onDown = { saveF(K.layoutGrid, encodeLayout(move(grid, i, 1))) },
                    onToggle = { v -> saveF(K.layoutGrid, encodeLayout(grid.map { if (it.first == id) id to v else it })) })
            }
        }
        SizePicker("Grid button ka size", gs, { gs = it }) { saveFl(K.gridScale, gs) }

        Spacer(Modifier.height(16.dp))
        LdText("NEECHE KI LINE · order", 12f, FontWeight.Bold, p.muted, letter = 1f, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        GlassCard {
            bottom.forEachIndexed { i, (id, on) ->
                LayoutRow(id, on, i > 0, i < bottom.lastIndex, id != "end",
                    onUp = { saveF(K.layoutBottom, encodeLayout(move(bottom, i, -1))) },
                    onDown = { saveF(K.layoutBottom, encodeLayout(move(bottom, i, 1))) },
                    onToggle = { v -> saveF(K.layoutBottom, encodeLayout(bottom.map { if (it.first == id) id to v else it })) })
            }
        }
        SizePicker("Neeche ke button ka size", bs, { bs = it }) { saveFl(K.bottomScale, bs) }

        Spacer(Modifier.height(18.dp))
        GhostButton("Reset to default") { LdApp.instance.scope.launch { LdApp.instance.repo.resetCallLayout() }; gs = 1f; bs = 1f }
        Spacer(Modifier.height(8.dp))
        LdText("Har badlav turant save hota hai aur agli call me dikhega.", 12f, color = p.muted, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun LayoutRow(id: String, on: Boolean, canUp: Boolean, canDown: Boolean, canHide: Boolean, onUp: () -> Unit, onDown: () -> Unit, onToggle: (Boolean) -> Unit) {
    val p = LocalLd.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(p.accent.pct(if (on) .18f else .06f)), contentAlignment = Alignment.Center) {
            LdIcon(btnIcon(id), 17.dp, if (on) p.accent else p.muted)
        }
        LdText(btnName(id), 14f, FontWeight.SemiBold, if (on) p.text else p.muted, Modifier.weight(1f))
        ArrowBtn(true, canUp, onUp)
        ArrowBtn(false, canDown, onDown)
        if (canHide) Switch(on, onToggle, colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = Color.White))
        else Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) { LdText("hamesha", 10.5f, color = p.muted) }
    }
}

@Composable
private fun ArrowBtn(up: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalLd.current
    Box(Modifier.size(32.dp).alpha(if (enabled) 1f else .3f).clip(CircleShape).border(1.dp, p.stroke, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) {
        LdIcon(LdIcons.ChevronDown, 16.dp, p.text, if (up) Modifier.rotate(180f) else Modifier)
    }
}

@Composable
private fun SizePicker(title: String, value: Float, onChange: (Float) -> Unit, onDone: () -> Unit) {
    val p = LocalLd.current
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).glass(p, RoundedCornerShape(20.dp), 2.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LdText(title, 13.5f, FontWeight.SemiBold, modifier = Modifier.weight(1f))
            LdText("${(value * 100).toInt()}%", 12.5f, FontWeight.SemiBold, p.muted)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Small" to .8f, "Medium" to 1f, "Large" to 1.2f).forEach { (l, v) ->
                val sel = kotlin.math.abs(value - v) < .03f
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (sel) p.accent.pct(.30f) else Color.Transparent)
                    .border(1.dp, if (sel) p.accent else p.stroke, RoundedCornerShape(14.dp)).clickable { onChange(v); onDone() }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center) { LdText(l, 12.5f, FontWeight.SemiBold) }
            }
        }
        Slider(value, onChange, valueRange = .7f..1.35f, onValueChangeFinished = onDone,
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent))
    }
}
