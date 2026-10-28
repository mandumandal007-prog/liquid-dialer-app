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
fun NumberSheet(number: String, contact: Contact?, st: AppState, act: MainActivity, onClose: () -> Unit, onEdit: (Long?) -> Unit, onNotes: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val detail = remember(contact) { contact?.let { Repo.contactDetail(ctx, it.id) } }
    val history = remember(number) { Repo.historyFor(ctx, number) }
    var starred by remember { mutableStateOf(contact?.starred == true) }
    var blocked by remember { mutableStateOf(Repo.isBlocked(ctx, number)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val name = contact?.name ?: if (number.isBlank()) "Private number" else Repo.pretty(number)
    val df = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    val notes = remember(number) { Repo.notesFor(ctx, number) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(name, contact?.photo, 72.dp, 24f, seed = name)
        Spacer(Modifier.height(10.dp))
        LdText(name, 20f, FontWeight.Bold)
        if (contact != null) LdText("${contact.label} · ${Repo.pretty(number)}", 12.5f, color = p.muted)
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
        SheetRow(LdIcons.Phone, Repo.pretty(n), "Doosra number") { onClose(); act.call(n) }
    }
    detail?.emails?.forEach { e -> SheetRow(LdIcons.Message, e, "Email") { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$e"))) } }
    if (contact != null) SheetRow(LdIcons.Edit, "Contact edit karo") { onEdit(contact.id) }
    else if (number.isNotBlank()) SheetRow(LdIcons.Plus, "Contact me add karo") { onEdit(null) }
    if (number.isNotBlank()) {
        SheetRow(LdIcons.Check, "Number copy karo") {
            ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("number", number))
            Toast.makeText(ctx, "Copy ho gaya", Toast.LENGTH_SHORT).show()
        }
        SheetRow(LdIcons.Chevron, "Number share karo") {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$name: $number"), "Share"))
        }
        SheetRow(LdIcons.Edit, "Notes (${notes.size})") { onNotes() }
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
fun SheetRow(icon: ImageVector, title: String, sub: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    val p = LocalLd.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).glass(p, RoundedCornerShape(16.dp), 2.dp).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
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
