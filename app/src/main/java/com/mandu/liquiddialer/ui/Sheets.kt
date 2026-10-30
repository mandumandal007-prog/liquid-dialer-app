package com.mandu.liquiddialer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NumberSheet(number: String, contact: Contact?, st: AppState, act: MainActivity, onClose: () -> Unit, onEdit: (Long?) -> Unit, onNotes: () -> Unit, onLongNumber: (String) -> Unit = {}) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val detail = remember(contact) { contact?.let { Repo.contactDetail(ctx, it.id) } }
    val history = remember(number) { Repo.historyFor(ctx, number) }
    var starred by remember { mutableStateOf(contact?.starred == true) }
    var blocked by remember { mutableStateOf(Repo.isBlocked(ctx, number)) }
    var confirmDelete by remember { mutableStateOf(false) }
    var remindOpen by remember { mutableStateOf(false) }
    var reminderTick by remember { mutableStateOf(0) }
    val reminder = remember(reminderTick) { com.mandu.liquiddialer.data.CallReminders.forNumber(ctx, number) }
    val name = contact?.name ?: if (number.isBlank()) "Private number" else Repo.pretty(number)
    val df = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    val notes = remember(number) { Repo.notesFor(ctx, number) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(name, contact?.photo, 72.dp, 24f, seed = name)
        Spacer(Modifier.height(10.dp))
        LdText(name, 20f, FontWeight.Bold)
        if (contact != null) LdText("${contact.label} · ${Repo.pretty(number)}", 12.5f, color = p.muted,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(onClick = {}, onLongClick = { onLongNumber(number) }).padding(horizontal = 6.dp, vertical = 2.dp))
        else if (number.isNotBlank()) LdText("Number par dabake rakho = options", 11f, color = p.muted, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { onLongNumber(number) }))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionBubble(LdIcons.Phone, "Call", true) { onClose(); act.call(number) }
            ActionBubble(LdIcons.Message, "SMS") { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))) }
            ActionBubble(LdIcons.Message, "WhatsApp") {
                val digits = number.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it }
                try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))) } catch (_: Exception) { }
            }
            if (contact != null) ActionBubble(if (starred) LdIcons.StarFill else LdIcons.Star, if (starred) "Favourite" else "Fav karo", starred) {
                starred = !starred; Repo.setStarred(ctx, contact.id, starred); act.reload()
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    detail?.phones?.filter { Repo.key(it.first) != Repo.key(number) }?.forEach { (n, _) ->
        SheetRow(LdIcons.Phone, Repo.pretty(n), "Doosra number · dabake rakho = options", onLongClick = { onLongNumber(n) }) { onClose(); act.call(n) }
    }
    detail?.emails?.forEach { e -> SheetRow(LdIcons.Message, e, "Email") { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$e"))) } }
    if (contact != null) SheetRow(LdIcons.Edit, "Contact edit karo") { onEdit(contact.id) }
    else if (number.isNotBlank()) SheetRow(LdIcons.Plus, "Contact me add karo") { onEdit(null) }
    if (number.isNotBlank()) {
        SheetRow(LdIcons.Check, "Number copy karo") {
            ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("number", number))
            Toast.makeText(ctx, trNow("Copy ho gaya"), Toast.LENGTH_SHORT).show()
        }
        SheetRow(LdIcons.Chevron, "Number share karo") {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$name: $number"), "Share"))
        }
        SheetRow(LdIcons.Edit, "Notes (${notes.size})") { onNotes() }
        SheetRow(LdIcons.Clock, "Call reminder", reminder?.let { "Set: " + df.format(Date(it.atMs)) }) { remindOpen = !remindOpen }
        if (remindOpen) {
            listOf("30min" to "30 min baad", "1h" to "1 ghante baad", "evening" to "Shaam 6 baje", "morning" to "Kal subah 9 baje").forEach { (preset, label) ->
                SheetRow(LdIcons.Chevron, label) {
                    val at = com.mandu.liquiddialer.data.CallReminders.presetAt(preset, System.currentTimeMillis())
                    com.mandu.liquiddialer.data.CallReminders.set(ctx, number, name, at)
                    reminderTick++; remindOpen = false
                    Toast.makeText(ctx, trNow("Reminder set") + ": " + df.format(Date(at)), Toast.LENGTH_SHORT).show()
                }
            }
            LdText("Samay lagbhag hoga - battery saver thoda der kar sakta hai", 11.5f, color = p.muted,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))
            if (!androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                SheetRow(LdIcons.Block, "Notifications band hain!", "Reminder nahi dikhega - settings kholo", danger = true) {
                    try {
                        ctx.startActivity(Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                            .putExtra("android.provider.extra.APP_PACKAGE", ctx.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) { }
                }
            }
            reminder?.let {
                SheetRow(LdIcons.Block, "Reminder hataye", danger = true) {
                    com.mandu.liquiddialer.data.CallReminders.cancel(ctx, number)
                    reminderTick++; remindOpen = false
                    Toast.makeText(ctx, trNow("Reminder hataya"), Toast.LENGTH_SHORT).show()
                }
            }
        }
        SheetRow(LdIcons.Phone, "Home screen par call button",
            if (com.mandu.liquiddialer.data.DialShortcuts.supported(ctx)) "Button dabate hi call lag jayega" else "Is launcher par shortcut nahi ban sakta") {
            val ok = com.mandu.liquiddialer.data.DialShortcuts.pin(ctx, number, name)
            Toast.makeText(ctx, trNow(if (ok) "Home screen par button ban gaya" else "Shortcut nahi ban saka"), Toast.LENGTH_SHORT).show()
        }
        SheetRow(LdIcons.Block, if (blocked) "Unblock karo" else "Number block karo", if (!st.isDefault) "Default phone app hone par kaam karega" else null, danger = !blocked) {
            if (!Repo.canBlock(ctx)) { act.requestDefault(); return@SheetRow }
            blocked = if (blocked) !Repo.unblock(ctx, number) else Repo.block(ctx, number)
            Toast.makeText(ctx, if (blocked) "Block ho gaya" else "Unblock ho gaya", Toast.LENGTH_SHORT).show()
            act.reload()
        }
    }
    if (history.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LdText("CALL HISTORY", 12f, FontWeight.Bold, p.muted, letter = 1.2f)
            Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable { Repo.deleteRecentsFor(ctx, number); act.reload(); onClose() }.padding(6.dp)) {
                LdText("Delete", 12f, FontWeight.SemiBold, p.danger)
            }
        }
        Spacer(Modifier.height(6.dp))
        GlassCard(radius = 20.dp, padding = 12.dp) {
            history.take(15).forEach { r ->
                val missed = r.type == CallLog.Calls.MISSED_TYPE || r.type == CallLog.Calls.REJECTED_TYPE
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LdIcon(typeIcon(r.type), 15.dp, if (missed) p.danger else p.muted)
                    LdText(typeLabel(r.type), 13f, FontWeight.SemiBold, if (missed) p.danger else p.text, Modifier.weight(1f))
                    LdText(df.format(Date(r.date)) + if (r.duration > 0) "  ·  " + fmtDur(r.duration) else "", 11.5f, color = p.muted)
                }
            }
        }
    }
    if (contact != null) {
        Spacer(Modifier.height(10.dp))
        SheetRow(LdIcons.Trash, "Contact delete karo", danger = true) { confirmDelete = true }
    }
    if (confirmDelete && contact != null) {
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Pakka delete karo?", danger = true) {
            Repo.deleteContact(ctx, contact.id, contact.lookupKey); act.reload(); onClose()
        }
    }
}

@Composable
fun ActionBubble(icon: ImageVector, label: String, on: Boolean = false, onClick: () -> Unit) {
    val p = LocalLd.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IconBtn(icon, 52.dp, 22.dp, on = on, onClick = onClick)
        LdText(label, 11f, FontWeight.SemiBold, p.muted)
    }
}

@Composable
fun SheetRow(icon: ImageVector, title: String, sub: String? = null, danger: Boolean = false, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val p = LocalLd.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).glass(p, RoundedCornerShape(16.dp), 2.dp).combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background((if (danger) p.danger else p.accent).pct(.16f)), contentAlignment = Alignment.Center) {
            LdIcon(icon, 17.dp, if (danger) p.danger else p.accent)
        }
        Column(Modifier.weight(1f)) {
            LdText(title, 14f, FontWeight.SemiBold, if (danger) p.danger else p.text)
            if (sub != null) LdText(sub, 11.5f, color = p.muted, maxLines = 2)
        }
    }
}


/** Long-press menu for any number (Recents, Contacts, contact detail, dialer). */
@Composable
fun NumberMenu(number: String, st: AppState, act: MainActivity, onClose: () -> Unit, onEdit: (Long?) -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val contact = remember(number, st.contacts) { st.contacts.firstOrNull { Repo.key(it.number) == Repo.key(number) } }
    var blocked by remember(number) { mutableStateOf(Repo.isBlocked(ctx, number)) }
    var starred by remember(contact) { mutableStateOf(contact?.starred == true) }
    val inLog = remember(number, st.recents) { st.recents.any { Repo.key(it.number) == Repo.key(number) } }
    val name = contact?.name ?: Repo.pretty(number)
    fun go(i: Intent) { try { ctx.startActivity(i) } catch (e: Exception) { Toast.makeText(ctx, trNow("Koi app nahi mila"), Toast.LENGTH_SHORT).show() } }

    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(name, contact?.photo, 46.dp, 16f, seed = name)
        Column(Modifier.weight(1f)) {
            LdText(name, 16.5f, FontWeight.Bold, maxLines = 1)
            if (contact != null) LdText(Repo.pretty(number), 12.5f, color = p.muted)
        }
    }
    SheetRow(LdIcons.Phone, "Call") { onClose(); act.call(number) }
    SheetRow(LdIcons.Video, "Video call", "ViLTE - SIM/phone support chahiye") { onClose(); act.videoCall(number) }
    SheetRow(LdIcons.Message, "Message (SMS)") { onClose(); go(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))) }
    SheetRow(LdIcons.Message, "WhatsApp") {
        val digits = number.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it.removePrefix("00") }
        onClose(); go(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
    }
    SheetRow(LdIcons.Check, "Number copy karo") {
        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("number", number))
        Toast.makeText(ctx, trNow("Copy ho gaya"), Toast.LENGTH_SHORT).show(); onClose()
    }
    SheetRow(LdIcons.Dialpad, "Call se pehle edit karo") { st.dial = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }; st.tab = "dialer"; onClose() }
    if (contact != null) SheetRow(LdIcons.Edit, "Contact edit karo") { onEdit(contact.id) }
    else SheetRow(LdIcons.Plus, "Contacts me add karo") { onEdit(null) }
    SheetRow(if (starred) LdIcons.StarFill else LdIcons.Star, if (starred) "Favourite se hatao" else "Favourite me add karo",
        if (contact == null) "Pehle contact save hoga" else null) {
        if (contact == null) { onEdit(null); return@SheetRow }
        starred = !starred; Repo.setStarred(ctx, contact.id, starred); act.reload()
        Toast.makeText(ctx, if (starred) "Favourite me add ho gaya" else "Favourite se hat gaya", Toast.LENGTH_SHORT).show()
    }
    SheetRow(LdIcons.Block, if (blocked) "Unblock karo" else "Block karo", if (!st.isDefault) "Default phone app hone par kaam karega" else null, danger = !blocked) {
        if (!Repo.canBlock(ctx)) { act.requestDefault(); return@SheetRow }
        blocked = if (blocked) !Repo.unblock(ctx, number) else Repo.block(ctx, number)
        Toast.makeText(ctx, if (blocked) "Block ho gaya" else "Unblock ho gaya", Toast.LENGTH_SHORT).show()
        act.reload()
    }
    if (inLog) SheetRow(LdIcons.Trash, "Call log se delete karo", danger = true) {
        Repo.deleteRecentsFor(ctx, number); act.reload(); onClose()
        Toast.makeText(ctx, trNow("Call log se hata diya"), Toast.LENGTH_SHORT).show()
    }
    SheetRow(LdIcons.Chevron, "Share karo") {
        onClose(); go(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, if (contact != null) "${contact.name}: $number" else number), "Share"))
    }
}
