package com.mandu.liquiddialer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.ContactControls
import com.mandu.liquiddialer.data.Repo

/** Never-block list: ye numbers unknown aur spam-series blocking ke bawajood ring karenge.
 * Private/hidden numbers isme aa hi nahi sakte. Sirf app ki screening pe effect; system block list nahi chhuti. */
@Composable fun NeverBlockPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    var tick by remember { mutableIntStateOf(0) }
    val ids = remember(tick) { ContactControls.blockNeverList(ctx) }
    PageFrame("Kabhi block mat karo", onBack) {
        GlassCard {
            LdText("Ye numbers unknown aur spam-series blocking ke bawajood ring karenge. Private / hidden number isme nahi aa sakta (number hota hi nahi). System ki block list pe iska effect nahi. List sirf is phone pe rehti hai.", 12f, color = p.muted, maxLines = 6, modifier = Modifier.testTag("neverblock_note"))
        }
        Spacer(Modifier.height(12.dp))
        if (ids.isEmpty()) {
            EmptyState(LdIcons.Check, "Koi number nahi", "Block history me kisi row ko kholo aur 'Kabhi block mat karo' dabao")
        } else {
            LdText("${ids.size} numbers", 12f, color = p.muted, modifier = Modifier.testTag("neverblock_count"))
            Spacer(Modifier.height(8.dp))
            ids.forEach { idn ->
                val disp = idn.removePrefix("local:")
                GlassRow(onClick = {}, modifier = Modifier.testTag("neverblock_row")) {
                    Avatar(disp, null, seed = idn)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        LdText(Repo.pretty(disp), 14.5f, FontWeight.SemiBold, maxLines = 2)
                        LdText(Repo.lookupName(ctx, disp)?.first ?: "Contacts me nahi", 11.5f, color = p.muted, maxLines = 1)
                    }
                    Box(Modifier.buttonGlass(p, RoundedCornerShape(12.dp)).clickable { ContactControls.setBlockNever(ctx, disp, false); tick++ }.padding(8.dp).testTag("neverblock_remove")) {
                        LdText("Hatao", 12.5f, FontWeight.SemiBold, p.accent)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
