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
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.PrivateNumbers
import com.mandu.liquiddialer.data.Repo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Private contacts: app-local hide switch (default OFF) and the saved list with Show again. */
@Composable fun PrivateNumbersPage(act: MainActivity?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    val ids = remember(tick) { PrivateNumbers.list(ctx) }
    PageFrame("Private contacts", onBack) {
        Box(Modifier.testTag("private_contacts_switch")) {
            SwitchRow("Private contacts", "Default OFF. ON karne par jo numbers neeche list me hain wo is app ki lists se hide rehte hain. OFF karne par sab wapas dikhte hain aur list saved rehti hai.", s.privateContacts, last = true, subMaxLines = 8) { on ->
                scope.launch {
                    try {
                        LdApp.instance.repo.set(K.privateContacts, on)
                        LdApp.settings.first { it.privateContacts == on }
                        error = false
                        act?.reload()
                    } catch (_: Exception) { error = true }
                }
            }
        }
        if (error) LdText("Setting save nahi hui", 12.5f, color = p.danger, maxLines = 3)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            LdText(PrivateNumbers.NOTE, 12.5f, color = p.muted, maxLines = 10, modifier = Modifier.testTag("private_contacts_note"))
        }
        Spacer(Modifier.height(12.dp))
        if (ids.isEmpty()) {
            EmptyState(LdIcons.EyeOff, "Koi private number nahi", "Switch ON karke kisi number ki sheet me 'Make private' dabao")
        } else {
            LdText("${ids.size} private numbers" + if (s.privateContacts) "" else " (switch OFF: abhi sab dikh rahe hain)", 12f, color = p.muted, maxLines = 3, modifier = Modifier.testTag("private_contacts_count"))
            Spacer(Modifier.height(8.dp))
            ids.forEach { idn ->
                val disp = idn.removePrefix("local:")
                GlassRow(onClick = {}, modifier = Modifier.testTag("private_row")) {
                    Avatar(disp, null, seed = idn)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        LdText(Repo.pretty(disp), 14.5f, FontWeight.SemiBold, maxLines = 2)
                        LdText(Repo.lookupName(ctx, disp)?.first ?: "Contacts me nahi", 11.5f, color = p.muted, maxLines = 1)
                    }
                    Box(Modifier.buttonGlass(p, RoundedCornerShape(12.dp)).clickable { PrivateNumbers.set(ctx, disp, false); tick++; act?.reload() }.padding(8.dp).testTag("private_show_again")) {
                        LdText("Show again", 12.5f, FontWeight.SemiBold, p.accent)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** Number sheet action. Not shown at all while the Private contacts switch is OFF. */
@Composable fun PrivateNumberAction(number: String, act: MainActivity?, onDone: () -> Unit = {}) {
    val s by LdApp.settings.collectAsState()
    val ctx = LocalContext.current
    if (!s.privateContacts || PrivateNumbers.id(number).isEmpty()) return
    var tick by remember(number) { mutableIntStateOf(0) }
    val priv = remember(number, tick) { PrivateNumbers.isPrivate(ctx, number) }
    SheetRow(LdIcons.EyeOff, if (priv) "Private se hatao" else "Make private (sirf is app me)",
        if (priv) "Is number ko app ki lists me wapas dikhao" else "Is Dialer app ki lists se hide. Phone ke Contacts aur call log me dikhta rahega",
        modifier = Modifier.testTag("sheet_make_private")) {
        PrivateNumbers.set(ctx, number, !priv); tick++; act?.reload(); onDone()
    }
}
