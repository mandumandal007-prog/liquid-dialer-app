package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val DIALER_LAYOUTS = listOf("Circles", "Soft tiles", "Wide pills")

/** Static previews: no provider access, bitmap downloads or animation. */
@Composable
fun DialerLayoutChoices(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalLd.current
    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIALER_LAYOUTS.forEachIndexed { i, label ->
                val on = selected == i
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .border(if(on) 1.5.dp else 1.dp, if(on) p.accent else p.stroke, RoundedCornerShape(16.dp))
                    .clickable { onSelect(i) }.testTag("dialer_layout_$i").padding(vertical=12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        repeat(3) {
                            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                repeat(3) {
                                    val shape = if(i==1) RoundedCornerShape(4.dp) else CircleShape
                                    Box(Modifier.width(if(i==2) 20.dp else 16.dp).height(16.dp)
                                        .clip(shape).background(p.accent.copy(alpha=if(on) .2f else .08f))
                                        .border(.8.dp, p.accent.copy(alpha=.7f), shape))
                                }
                            }
                        }
                    }
                    LdText(label, 11f, FontWeight.SemiBold, if(on) p.accent else p.text,
                        modifier=Modifier.padding(top=9.dp), maxLines=2)
                    LdText(if(on) "Selected" else "Choose", 10f, color=p.muted, modifier=Modifier.padding(top=3.dp))
                }
            }
        }
        LdText("Sirf dialer keypad ka shape. Number order, long press aur call screen nahi badalte.",
            11.5f, color=p.muted, maxLines=4, modifier=Modifier.padding(top=10.dp))
    }
}
