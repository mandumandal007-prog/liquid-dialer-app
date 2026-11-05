package com.mandu.liquiddialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.BlockLog
import com.mandu.liquiddialer.data.Repo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** App ki screening se roki gayi calls (private/unknown/series) ka history page.
 * Manually blocked (system block list) numbers ki calls yahan NAHI aati - wo Recents me dikhti hain. */
@Composable fun BlockHistoryPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    var list by remember { mutableStateOf(BlockLog.list(ctx)) }
    var confirmClear by remember { mutableStateOf(false) }
    PageFrame("Block history", onBack) {
        GlassCard {
            LdText("Sirf app ki screening se roki gayi calls (private / unknown / spam series). Manually blocked numbers ki calls Recents me dikhti hain. List sirf is phone pe rehti hai, 200 tak.", 12f, color = p.muted, maxLines = 6, modifier = Modifier.testTag("blockhistory_note"))
        }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            EmptyState(LdIcons.Block, "Koi call block nahi hui", "Unknown / private / series blocking on karne pe roki gayi calls yahan dikhengi")
        } else {
            LdText("${list.size} roki gayi calls", 12f, color = p.muted, modifier = Modifier.testTag("blockhistory_count"))
            Spacer(Modifier.height(8.dp))
            list.forEach { e ->
                GlassRow(onClick = {}, modifier = Modifier.testTag("blockhistory_row")) {
                    Avatar(if (e.display.isBlank()) "?" else e.display, null, seed = e.idn.ifBlank { "private" })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        LdText(if (e.display.isBlank()) "Private / hidden number" else Repo.pretty(e.display), 14.5f, FontWeight.SemiBold, maxLines = 1)
                        LdText(
                            when (e.reason) {
                                BlockLog.REASON_PRIVATE -> "Private number block"
                                BlockLog.REASON_UNKNOWN -> "Unknown number block"
                                else -> "Spam series block"
                            } + " · " + SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(e.ts)),
                            11.5f, color = p.muted, maxLines = 2
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (confirmClear) {
                GlassCard {
                    LdText("Poori block history mit jayegi. Blocking rules nahi badlenge.", 13f, FontWeight.SemiBold, maxLines = 3, modifier = Modifier.testTag("blockhistory_confirm_text"))
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Box(Modifier.weight(1f)) { PrimaryButton("Haan, saaf karo", modifier = Modifier.testTag("blockhistory_confirm_yes")) { BlockLog.clear(ctx); list = emptyList(); confirmClear = false } }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) { PrimaryButton("Rehne do", modifier = Modifier.testTag("blockhistory_confirm_no")) { confirmClear = false } }
                    }
                }
            } else {
                PrimaryButton("History saaf karo", modifier = Modifier.testTag("blockhistory_clear")) { confirmClear = true }
            }
        }
    }
}
