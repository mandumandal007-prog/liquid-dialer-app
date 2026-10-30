package com.mandu.liquiddialer.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.Preferences
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.RUNTIME_PERMS
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.media.BoostService
import com.mandu.liquiddialer.media.VolumeBoost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private fun <T> save(key: Preferences.Key<T>, v: T) { LdApp.instance.scope.launch { LdApp.instance.repo.set(key, v) } }

/** .page: full-screen with radial accent 16% at top over bg */
@Composable
fun PageFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalLd.current
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize().background(p.bg).clickable(remember { MutableInteractionSource() }, null) {}) {
        BlobBackground()
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(0f to p.accent.pct(.16f), .6f to Color.Transparent, center = Offset(540f, 0f), radius = 1500f)))
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.testTag("page_back")) { IconBtn(LdIcons.ArrowLeft, onClick = onBack) }
                LdText(title, 22f, FontWeight.ExtraBold, modifier = Modifier.weight(1f), letter = -.3f)
                Spacer(Modifier.width(42.dp))
            }
            content()
        }
    }
}

@Composable fun SecTitle(t: String) = LdText(t.uppercase(), 12.5f, FontWeight.ExtraBold, LocalLd.current.muted, Modifier.padding(start = 6.dp, end = 6.dp, top = 22.dp, bottom = 10.dp), letter = 1.4f)

/** .srow inside a .card */
@Composable
fun SRow(title: String, sub: String? = null, icon: ImageVector? = null, last: Boolean = false, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val p = LocalLd.current
    Column {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon != null) Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(p.accent.pct(.16f)), contentAlignment = Alignment.Center) { LdIcon(icon, 19.dp, p.accent) }
            Column(Modifier.weight(1f)) {
                LdText(title, 14f, FontWeight.SemiBold, maxLines = 2)
                if (sub != null) LdText(sub, 11.5f, color = p.muted, maxLines = 3, modifier = Modifier.padding(top = 3.dp))
            }
            if (trailing != null) trailing() else if (onClick != null) LdIcon(LdIcons.Chevron, 16.dp, p.muted.copy(alpha = .5f))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(if (p.isLight) Color(0x120F172A) else Color.White.copy(alpha = .07f)))
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, last: Boolean = false, onChange: (Boolean) -> Unit) =
    SRow(title, sub, last = last, trailing = { LdSwitch(checked, onChange) }, onClick = { onChange(!checked) })

@Composable
fun Page(page: String, st: AppState, act: MainActivity, onBack: () -> Unit, onOpen: (String) -> Unit, editContactId: Long?, editPrefill: String, onOpenNumber: (String) -> Unit) {
    when {
        page == "settings" -> SettingsPage(st, act, onBack, onOpen)
        page == "appearance" -> AppearancePage(onBack)
        page == "volume" -> VolumePage(onBack)
        page == "profile" -> ProfilePage(onBack)
        page == "calllayout" -> CallLayoutPage(onBack)
        page == "editor" -> ContactEditor(editContactId, editPrefill, act, onBack)
        page == "recordings" -> RecordingsPage(onBack)
        page == "notes" -> NotesPage(null, onBack)
        page.startsWith("notes:") -> NotesPage(page.removePrefix("notes:"), onBack)
        page == "stats" -> StatsPage(st, onBack)
        page == "blocked" -> BlockedPage(st, act, onBack)
        page == "speeddial" -> SpeedDialPage(st, onBack)
        page == "radio" -> RadioInfoPage(onBack)
        page == "replies" -> RepliesPage(onBack)
        page == "permissions" -> PermissionsPage(st, act, onBack)
        else -> PageFrame(page, onBack) {}
    }
}

@Composable
private fun SettingsPage(st: AppState, act: MainActivity, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val ctx = LocalContext.current
    var sheet by remember { mutableStateOf<String?>(null) }
    val tick = st.permsTick
    val ringtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        @Suppress("DEPRECATION")
        val uri: Uri? = r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            if (Settings.System.canWrite(ctx)) { RingtoneManager.setActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE, uri); Toast.makeText(ctx, trNow("Ringtone set ho gayi"), Toast.LENGTH_SHORT).show() }
            else { Toast.makeText(ctx, trNow("Pehle 'Modify system settings' allow karo, phir dobara chuno"), Toast.LENGTH_LONG).show()
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))) }
        }
    }
    val exportVcf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-vcard")) { uri ->
        if (uri != null) LdApp.instance.scope.launch {
            val n = exportContacts(ctx, uri)
            withContext(Dispatchers.Main) { Toast.makeText(ctx, if (n > 0) "$n contacts export ho gaye" else "Export nahi hua", Toast.LENGTH_LONG).show() }
        }
    }
    val importVcf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/x-vcard").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: Exception) { Toast.makeText(ctx, trNow("Phone me VCF import karne wala app nahi mila"), Toast.LENGTH_LONG).show() }
    }
    fun open(i: Intent) { try { ctx.startActivity(i) } catch (_: Exception) { act.openAppSettings() } }

    PageFrame("Settings", onBack) {
        // profile card
        Row(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(26.dp)).clickable { onOpen("profile") }.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(s.profileName.ifBlank { "You" }, s.profilePhoto.ifBlank { null }, 58.dp, 19f, seed = "me")
            Column(Modifier.weight(1f)) { LdText(s.profileName.ifBlank { "My profile" }, 17f, FontWeight.Bold); LdText(s.profileNumber.ifBlank { "Naam, photo aur number set karo" }, 12.5f, color = p.muted) }
            LdIcon(LdIcons.Edit, 18.dp, p.muted)
        }
        Spacer(Modifier.height(10.dp))
        GlassCard {
            SRow("Calling card layout", "Calling card, photo, background, buttons - sab customize karo", LdIcons.Dialpad) { onOpen("calllayout") }
            SwitchRow("Swipe se tab badlo", "Recents aur Contacts ke beech ungli ghumao", s.tabSwipe) { save(K.tabSwipe, it) }
        }

        SecTitle("Default app")
        GlassCard {
            SRow(if (st.isDefault) "Default phone app ✓" else "Default phone app banao", if (st.isDefault) "Liquid Dialer hi calls sambhal raha hai" else "Incoming call screen, block aur call controls ke liye zaroori",
                LdIcons.Phone) { if (st.isDefault) act.openDefaultAppsSettings() else act.requestDefault() }
            SRow("Permissions", "${RUNTIME_PERMS.count { act.has(it.first) }}/${RUNTIME_PERMS.size} mili hui".also { tick }, LdIcons.Check, last = true) { onOpen("permissions") }
        }

        SecTitle("Display")
        GlassCard {
            SRow("Theme & appearance", "${paletteFor(s.theme, p.accent, true).name.let { if (s.theme == "auto") "Auto (system)" else it }} · blur ${s.blur}px", LdIcons.Palette) { onOpen("appearance") }
            SRow("App language", LANGS.firstOrNull { it.first == s.lang }?.second ?: "Hinglish", LdIcons.Message) { sheet = "lang" }
            SwitchRow("Favourites section", "Home pe favourite contacts dikhao/chhupao", s.showFavourites) { save(K.showFavourites, it) }
            SwitchRow("Din ke headers", "Recents me Aaj/Kal ke groups", s.groupByDay) { save(K.groupByDay, it) }
            SwitchRow("Call duration dikhao", "Recents me kitni der baat hui", s.recentsShowDuration) { save(K.recentsShowDuration, it) }
            SwitchRow("SIM label dikhao", "Multi-SIM pe recents me SIM 1/2", s.showSimInRecents) { save(K.showSimInRecents, it) }
            SwitchRow("Compact recents", "Choti rows, ek screen pe zyada entries", s.recentsCompact) { save(K.recentsCompact, it) }
            SwitchRow("Blocked calls chhupao", "Blocked number ki entries recents me na dikhein", s.recentsHideBlocked) { save(K.recentsHideBlocked, it) }
            LdText("Recents me kitni entries", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
            Segmented(listOf("100", "300", "500"), listOf(100, 300, 500).indexOf(s.recentLimit).coerceAtLeast(0)) { save(K.recentLimit, listOf(100, 300, 500)[it]) }
            SwitchRow("Exact time", "Recents me '2 min pehle' ki jagah exact samay", s.exactTime) { save(K.exactTime, it) }
            SwitchRow("Keypad letters (ABC)", "Dial pad pe English akshar", s.keypadLetters) { save(K.keypadLetters, it) }
            SwitchRow("Keypad haptic", "Key dabane pe halki vibration", s.hapticKeys) { save(K.hapticKeys, it) }
            LdText("Contact sort", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 10.dp, bottom = 8.dp))
            Segmented(listOf("First name", "Last name"), if (s.sortByLast) 1 else 0) { save(K.sortByLast, it == 1); act.reload() }
            LdText("Naam ka format", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
            Segmented(listOf("First Last", "Last, First"), if (s.lastNameFirst) 1 else 0) { save(K.lastNameFirst, it == 1) }
        }

        SecTitle("Calls")
        GlassCard {
            val sims = st.sims
            val simName = sims.firstOrNull { it.handle.id == s.defaultSim }?.let { "SIM ${it.slot} · ${it.label}" } ?: "Har baar poochho"
            SRow("Default SIM", if (sims.size > 1) simName else if (sims.size == 1) sims[0].label else "SIM nahi mili", LdIcons.Sim) { sheet = "sim" }
            LdText("Incoming call style", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 10.dp, bottom = 8.dp))
            Segmented(listOf("Full screen", "Sirf notification"), if (s.incomingFull) 0 else 1) { save(K.incomingFull, it == 0) }
            LdText("Call uthane ka tareeka", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
            Segmented(listOf("Swipe up", "Tap"), if (s.swipeAnswer) 0 else 1) { save(K.swipeAnswer, it == 0) }
            Spacer(Modifier.height(6.dp))
            if (Build.VERSION.SDK_INT >= 34) SRow("Full screen notification permission", "Lock screen pe poori call screen ke liye", LdIcons.Info) {
                open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + ctx.packageName)))
            }
            SRow("Quick reply messages", "${s.quickReplies.size} messages · call kaat ke SMS", LdIcons.Message) { onOpen("replies") }
            SRow("Call waiting", "Carrier code se on/off/check", LdIcons.Pause) { sheet = "waiting" }
            SRow("Call forwarding", "Doosre number pe call bhejo", LdIcons.PhoneOut) { sheet = "forward" }
            SwitchRow("Proximity sensor", "Kaan ke paas screen band", s.proximity) { save(K.proximity, it) }
            SwitchRow("Call confirm karo", "List me tap karne par pehle poochhega", s.confirmCall) { save(K.confirmCall, it) }
            SwitchRow("+91 apne aap jodo", "10-digit number ke aage +91 lagayega", s.autoPrefix) { save(K.autoPrefix, it) }
            SwitchRow("Number formatting", "Type karte waqt number space ke saath", s.formatNumber) { save(K.formatNumber, it) }
            SwitchRow("Contact ki SIM yaad rakho", "Pichli use ki hui SIM apne aap chunega", s.simMemory) { save(K.simMemory, it) }
            SwitchRow("3 sec ka undo", "Call lagne se pehle rukne ka mauka", s.undoCall) { save(K.undoCall, it) }
            SwitchRow("Busy pe redial suggest", "Call na lage to Dobara call button", s.redialSuggest) { save(K.redialSuggest, it) }
            SwitchRow("Connect pe speaker", "Outgoing call lagte hi speaker on (bluetooth pe nahi)", s.autoSpeaker) { save(K.autoSpeaker, it) }
            SwitchRow("Cut pe confirm", "End button dabate hi poochega", s.confirmBeforeEnd) { save(K.confirmBeforeEnd, it) }
            SwitchRow("Naya number save prompt", "Unknown number ki call ke baad Save/Block", s.postCallSave) { save(K.postCallSave, it) }
            LdText("App khule to pehle dikhao", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
            Segmented(listOf("Recents", "Dialer"), if (s.defaultTab == "dialer") 1 else 0) { save(K.defaultTab, if (it == 1) "dialer" else "recents") }
            SwitchRow("Ongoing call banner", "App me upar call ka bar", s.banner) { save(K.banner, it) }
            SRow("System call settings", "SIM, carrier aur advanced options", LdIcons.Settings, last = true) { open(Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)) }
        }

        SecTitle("Sounds & vibration")
        GlassCard {
            SRow("Ringtone", "Phone ki ringtone chuno", LdIcons.Volume) {
                ringtone.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE))
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Ringtone"))
            }
            SwitchRow("Vibrate jab call uthe", "Outgoing call connect hone par", s.vibrateAnswer) { save(K.vibrateAnswer, it) }
            SwitchRow("Vibrate jab call kate", null, s.vibrateEnd) { save(K.vibrateEnd, it) }
            SwitchRow("Keypad tones", null, s.keyTones) { save(K.keyTones, it) }
            SwitchRow("Flip to mute", "Phone ulta rakho to ringtone band", s.flipToMute) { save(K.flipToMute, it) }
            SwitchRow("Caller ka naam bolo", "Ringing pe naam announce", s.announceCaller) { save(K.announceCaller, it) }
            LdText("Naam kitni baar bolo", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
            Segmented(listOf("1 baar", "2 baar", "3 baar"), (s.announceRepeat - 1).coerceIn(0, 2)) { save(K.announceRepeat, it + 1) }
            SwitchRow("Sirf favourite ka naam bolo", "Announce sirf starred contacts ke liye", s.announceOnlyFav) { save(K.announceOnlyFav, it) }
            SwitchRow("Ring volume max", "Call aane pe ring volume system ka max (usse upar nahi)", s.ringVolumeMax) { save(K.ringVolumeMax, it) }
            SwitchRow("Call volume max", "Call connect hone pe volume system ka max (usse upar boost nahi)", s.callVolumeMax) { save(K.callVolumeMax, it) }
            LdText("Call me minute beep", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
            Segmented(listOf("Off", "1 min", "5 min", "10 min"), when (s.minuteBeep) { 1 -> 1; 5 -> 2; 10 -> 3; else -> 0 }) { save(K.minuteBeep, listOf(0, 1, 5, 10)[it]) }
            SwitchRow("Flash blink on ring", "Call aane pe flashlight chamkegi", s.flashOnRing) { save(K.flashOnRing, it) }
            SwitchRow("Sirf favourite pe flash", "Flash sirf starred contacts pe", s.flashOnlyFav, last = true) { save(K.flashOnlyFav, it) }
        }

        SecTitle("Volume")
        GlassCard {
            SRow("Volume & boost", "Ring, media, call volume ek jagah + media loudness boost", LdIcons.Volume, last = true) { onOpen("volume") }
        }

        SecTitle("Caller pehchan")
        GlassCard {
            SwitchRow("Pichli call ki jankari", "Incoming screen pe pichli call ka time", s.showLastCall) { save(K.showLastCall, it) }
            SwitchRow("Contact ka label", "Mobile/Work aur Favourite badge incoming pe", s.showContactType, last = true) { save(K.showContactType, it) }
        }

        SecTitle("Blocking")
        GlassCard {
            SRow("Blocked numbers", "${st.blocked.size} blocked", LdIcons.Block) { onOpen("blocked") }
            SwitchRow("Unknown numbers block", "Jo contacts me nahi, unki call kat jayegi", s.blockUnknown) { save(K.blockUnknown, it); if (it) act.requestScreeningRole() }
            SwitchRow("Private / hidden numbers block", null, s.blockPrivate, last = true) { save(K.blockPrivate, it); if (it) act.requestScreeningRole() }
        }

        SecTitle("Recording")
        GlassCard {
            SwitchRow("Sirf unknown numbers record", "Auto-record contacts ke bahar ke numbers ke liye", s.recordUnknownOnly) { save(K.recordUnknownOnly, it) }
            SwitchRow("Auto record calls", "Har call apne aap record (Android 10+ pe sirf aapki awaaz, speaker pe dono)", s.autoRecord) {
                save(K.autoRecord, it); if (it && !act.has(android.Manifest.permission.RECORD_AUDIO)) act.askPermission(android.Manifest.permission.RECORD_AUDIO)
            }
            SRow("Recordings", "Suno, share ya delete karo", LdIcons.Record) { onOpen("recordings") }
            SRow("Storage", "Android/data/${ctx.packageName}/files/Music/LiquidDialer", LdIcons.Info, last = true)
        }

        SecTitle("Speed dial & voicemail")
        GlassCard {
            SRow("Speed dial", "${s.speedDial.size} set · Dialer pe 2-9 long-press", LdIcons.Dialpad) { onOpen("speeddial") }
            SRow("Voicemail number", s.voicemail.ifBlank { Repo.voicemailNumber(ctx).ifBlank { "Set nahi" } } + " · 1 long-press", LdIcons.Voicemail) { sheet = "voicemail" }
            SRow("Voicemail call karo", null, LdIcons.Phone, last = true) { act.voicemail() }
        }

        SecTitle("Accessibility")
        GlassCard {
            SRow("TTY mode", "System accessibility screen", LdIcons.Info) { open(Intent(TelecomManager.ACTION_SHOW_CALL_ACCESSIBILITY_SETTINGS)) }
            SRow("Hearing aid (HAC)", "System accessibility screen", LdIcons.Volume, last = true) { open(Intent(TelecomManager.ACTION_SHOW_CALL_ACCESSIBILITY_SETTINGS)) }
        }

        SecTitle("Notifications")
        GlassCard {
            SwitchRow("Missed call alert", "Call back, Message aur Block button ke saath", s.missedAlerts) { save(K.missedAlerts, it) }
            SwitchRow("Missed call reminder", "Har 5 min me yaad dilayega (3 baar tak)", s.missedRemind) { save(K.missedRemind, it) }
            SwitchRow("Call summary notification", "Call khatam hote hi naam + duration ka notification. Lock screen par naam nahi dikhta.", s.callSummary) { save(K.callSummary, it) }
            SRow("Notification settings", null, LdIcons.Settings, last = true) {
                open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }
        }

        SecTitle("Backup")
        GlassCard {
            SRow("Contacts export (VCF)", "Saare contacts ek file me", LdIcons.ArrowLeft) { exportVcf.launch("contacts-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.vcf") }
            SRow("Contacts import (VCF)", "VCF file chuno", LdIcons.Plus, last = true) { importVcf.launch(arrayOf("text/x-vcard", "text/vcard", "text/directory", "*/*")) }
        }

        SecTitle("About")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .88f), p.accent))), contentAlignment = Alignment.Center) {
                    LdIcon(LdIcons.Phone, 22.dp, Color.White)
                }
                Column { LdText("Liquid Dialer", 15f, FontWeight.Bold); LdText("v${com.mandu.liquiddialer.BuildConfigHelper.version}", 12f, color = p.muted) }
            }
            LdText("Asli phone app: calls, contacts, call log, block, recording, notes. Frosted glass design.", 11.5f, color = p.muted, maxLines = 4, modifier = Modifier.padding(top = 8.dp))
        }
        LdText("Made for Mandu", 11f, color = p.muted, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp))
    }

    when (sheet) {
        "lang" -> Sheet({ sheet = null }) {
            LANGS.forEach { (code, name) -> SheetRow(LdIcons.Check, name, if (s.lang == code) "\u2713" else null) { save(K.lang, code); sheet = null } }
        }
        "sim" -> Sheet({ sheet = null }) {
            SheetTitle("Default SIM")
            SheetRow(LdIcons.Sim, "Har baar poochho") { save(K.defaultSim, "ask"); sheet = null }
            st.sims.forEach { sim -> SheetRow(LdIcons.Sim, "SIM ${sim.slot}", sim.label) { save(K.defaultSim, sim.handle.id); sheet = null } }
        }
        "voicemail" -> Sheet({ sheet = null }) {
            var v by remember { mutableStateOf(s.voicemail) }
            SheetTitle("Voicemail number")
            LdText("Khali chhodo to SIM ka voicemail number use hoga: ${Repo.voicemailNumber(ctx).ifBlank { "—" }}", 12f, color = p.muted, maxLines = 3, modifier = Modifier.padding(bottom = 10.dp))
            LdField(v, { v = it }, "Jaise *99# ya number", phone = true)
            PrimaryButton("Save") { save(K.voicemail, v.trim()); sheet = null }
        }
        "waiting" -> Sheet({ sheet = null }) {
            SheetTitle("Call waiting")
            LdText("Ye carrier ke code se hota hai (GSM). Result carrier ka message dikhayega.", 12f, color = p.muted, maxLines = 3, modifier = Modifier.padding(bottom = 10.dp))
            SheetRow(LdIcons.Check, "Status check karo", "*#43#") { act.call("*#43#"); sheet = null }
            SheetRow(LdIcons.Plus, "On karo", "*43#") { act.call("*43#"); sheet = null }
            SheetRow(LdIcons.X, "Off karo", "#43#") { act.call("#43#"); sheet = null }
            SheetRow(LdIcons.Settings, "System call settings") { open(Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)); sheet = null }
        }
        "forward" -> Sheet({ sheet = null }) {
            var n by remember { mutableStateOf("") }
            SheetTitle("Call forwarding")
            LdField(n, { n = it }, "Forward kis number pe", phone = true)
            SheetRow(LdIcons.PhoneOut, "Hamesha forward karo", "**21*number#") { if (n.isNotBlank()) { act.call("**21*${Repo.normalize(n)}#"); sheet = null } }
            SheetRow(LdIcons.PhoneOut, "Busy hone par", "**67*number#") { if (n.isNotBlank()) { act.call("**67*${Repo.normalize(n)}#"); sheet = null } }
            SheetRow(LdIcons.PhoneOut, "Na uthane par", "**61*number#") { if (n.isNotBlank()) { act.call("**61*${Repo.normalize(n)}#"); sheet = null } }
            SheetRow(LdIcons.Check, "Status check karo", "*#21#") { act.call("*#21#"); sheet = null }
            SheetRow(LdIcons.X, "Saari forwarding band", "##002#", danger = true) { act.call("##002#"); sheet = null }
            SheetRow(LdIcons.Settings, "System call settings") { open(Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)); sheet = null }
        }
    }
}

private fun exportContacts(ctx: android.content.Context, out: Uri): Int {
    val keys = mutableListOf<String>()
    ctx.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts.LOOKUP_KEY), null, null, null)?.use {
        while (it.moveToNext()) it.getString(0)?.let { k -> keys += k }
    }
    if (keys.isEmpty()) return 0
    return try {
        ctx.contentResolver.openOutputStream(out)?.use { os ->
            keys.chunked(200).forEach { chunk ->
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(chunk.joinToString(":")))
                ctx.contentResolver.openInputStream(uri)?.use { it.copyTo(os) }
            }
        }
        keys.size
    } catch (e: Exception) { 0 }
}

@Composable
private fun AppearancePage(onBack: () -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    PageFrame("Theme & appearance", onBack) {
        SecTitle("Theme")
        GlassCard {
            val keys = THEME_KEYS + "auto"
            keys.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { k ->
                        val pal = paletteFor(k, p.accent, true)
                        val on = s.theme == k
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (on) p.accent.pct(.14f) else p.glass)
                            .border(1.dp, if (on) p.accent else p.stroke, RoundedCornerShape(18.dp)).clickable { save(K.theme, k) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(
                                if (k == "auto") Brush.linearGradient(listOf(Color(0xFF0B1020), Color(0xFFC9D8FF))) else Brush.linearGradient(listOf(pal.previewA, pal.previewB))
                            ).border(1.dp, p.stroke, RoundedCornerShape(12.dp)))
                            LdText(if (k == "auto") "Auto" else pal.name, 12.5f, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (on) LdIcon(LdIcons.Check, 16.dp, p.accent)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        SecTitle("Accent")
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ACCENTS.forEach { a ->
                    val on = s.accent.toInt() == a.toArgb()
                    Box(Modifier.size(34.dp).graphicsLayer { val sc = if (on) 1.08f else 1f; scaleX = sc; scaleY = sc }
                        .shadow(6.dp, CircleShape, ambientColor = a.copy(alpha = .45f), spotColor = a.copy(alpha = .45f)).clip(CircleShape).background(a).border(2.dp, if (on) p.text else Color.Transparent, CircleShape)
                        .clickable { save(K.accent, a.toArgb().toLong() and 0xFFFFFFFFL) })
                }
            }
        }
        SecTitle("Glass blur")
        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LdText("Blur strength", 12.5f, FontWeight.SemiBold, p.muted); LdText("${s.blur}px", 12.5f, FontWeight.Bold, p.accent)
            }
            var v by remember { mutableFloatStateOf(s.blur.toFloat()) }
            Slider(v, { v = it }, valueRange = 0f..40f, onValueChangeFinished = { save(K.blur, v.toInt()) },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = p.accent, inactiveTrackColor = p.accent.pct(.2f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { LdText("Clear", 10.5f, color = p.muted); LdText("Frosted", 10.5f, color = p.muted) }
        }
        SecTitle("Text size")
        GlassCard {
            val opts = listOf(.9f, 1f, 1.1f, 1.2f)
            Segmented(listOf("Chhota", "Normal", "Bada", "Bahut bada"), opts.indexOfFirst { it == s.fontScale }.coerceAtLeast(1)) { save(K.fontScale, opts[it]) }
            SwitchRow("Text bold karo", "Poori app me moti likhai", s.boldText, last = true) { save(K.boldText, it) }
        }
        SecTitle("Motion")
        GlassCard { SwitchRow("Animations", "Blobs ka chalna aur floating avatar", s.motion, last = true) { save(K.motion, it) } }
    }
}


@Composable
private fun ProfilePage(onBack: () -> Unit) {
    val s by LdApp.settings.collectAsState()
    val ctx = LocalContext.current
    val p = LocalLd.current
    var name by remember { mutableStateOf(s.profileName) }
    var num by remember { mutableStateOf(s.profileNumber) }
    val own = remember { Repo.ownNumbers(ctx) }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            LdApp.instance.scope.launch(Dispatchers.IO) {
                val out = Repo.saveProfilePhoto(ctx, uri)
                if (out != null) LdApp.instance.repo.set(K.profilePhoto, out)
                withContext(Dispatchers.Main) { busy = false; if (out == null) Toast.makeText(ctx, trNow("Photo load nahi hui"), Toast.LENGTH_SHORT).show() }
            }
        }
    }
    fun pickPhoto() = pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    PageFrame("My profile", onBack) {
        Column(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clickable(remember { MutableInteractionSource() }, null) { pickPhoto() }) {
                Avatar(name.ifBlank { "You" }, s.profilePhoto.ifBlank { null }, 112.dp, 34f, glow = true, seed = "me")
                Box(Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(p.accent).border(2.dp, p.bg, CircleShape), contentAlignment = Alignment.Center) {
                    LdIcon(LdIcons.Edit, 16.dp, Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { pickPhoto() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    LdText(if (busy) "Photo set ho rahi hai…" else if (s.profilePhoto.isBlank()) "Gallery se photo chuno" else "Photo badlo", 13f, FontWeight.SemiBold, p.accent)
                }
                if (s.profilePhoto.isNotBlank()) Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                    java.io.File(ctx.filesDir, "profile.png").delete(); save(K.profilePhoto, "")
                }.padding(horizontal = 10.dp, vertical = 6.dp)) { LdText("Photo hatao", 13f, FontWeight.SemiBold, p.danger) }
            }
            LdText("Photo beech se gol (circle) kat ke app me save hoti hai", 11f, color = p.muted)
        }
        GlassCard {
            LdField(name, { name = it }, "Aapka naam")
            Spacer(Modifier.height(8.dp))
            LdField(num, { num = it }, "Aapka number (optional)", phone = true)
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Save") { save(K.profileName, name.trim()); save(K.profileNumber, num.trim()); onBack() }
        }
        Spacer(Modifier.height(14.dp))
        GlassCard {
            LdText("MERE NUMBER (SIM se)", 12f, FontWeight.Bold, p.muted, letter = 1.2f)
            Spacer(Modifier.height(6.dp))
            if (own.isEmpty()) LdText("SIM se number nahi padh paya. Phone permission do, ya kuch SIM number batati hi nahi. Upar khud likh do.", 12.5f, color = p.muted, maxLines = 4)
            own.forEach { n ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LdIcon(LdIcons.Sim, 18.dp, p.accent); LdText(n, 14f, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2)
                }
            }
        }
    }
}

private val PHONE_TYPES = listOf(
    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE to "Mobile",
    ContactsContract.CommonDataKinds.Phone.TYPE_HOME to "Home",
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK to "Work",
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER to "Other",
)

@Composable
private fun ContactEditor(contactId: Long?, prefill: String, act: MainActivity, onBack: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val detail = remember(contactId) { contactId?.let { Repo.contactDetail(ctx, it) } }
    var name by remember { mutableStateOf(detail?.name ?: "") }
    val phones = remember { mutableStateListOf<Pair<String, Int>>().apply {
        addAll(detail?.phones ?: emptyList()); if (isEmpty()) add(prefill to ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) } }
    var email by remember { mutableStateOf(detail?.emails?.firstOrNull() ?: "") }
    var fav by remember { mutableStateOf(detail?.starred ?: false) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var photoPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    val existingPhoto = rememberPhoto(detail?.photo)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) try {
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
            if (bmp != null) {
                val side = minOf(bmp.width, bmp.height)
                val sq = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
                val sc = android.graphics.Bitmap.createScaledBitmap(sq, 512, 512, true)
                val bos = java.io.ByteArrayOutputStream(); sc.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, bos)
                photoBytes = bos.toByteArray(); photoPreview = sc.asImageBitmap()
            }
        } catch (_: Exception) { }
    }
    val canWrite = act.has(android.Manifest.permission.WRITE_CONTACTS)
    PageFrame(if (detail == null) "Naya contact" else "Contact edit", onBack) {
        Box(Modifier.fillMaxWidth().padding(bottom = 18.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                val img = photoPreview ?: existingPhoto
                if (img != null) Image(img, null, Modifier.size(104.dp).clip(CircleShape).border(1.dp, p.stroke, CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                else Avatar(name.ifBlank { "+" }, null, 104.dp, 32f, glow = true, seed = name)
                Box(Modifier.align(Alignment.BottomEnd).size(34.dp).glass(p, CircleShape, 4.dp), contentAlignment = Alignment.Center) { LdIcon(LdIcons.Edit, 16.dp, p.accent) }
            }
        }
        if (!canWrite) GlassCard(Modifier.padding(bottom = 12.dp)) {
            LdText("Contacts likhne ki permission nahi hai", 13f, FontWeight.SemiBold, p.danger)
            Spacer(Modifier.height(8.dp)); GhostButton("Permission do") { act.askPermission(android.Manifest.permission.WRITE_CONTACTS) }
        }
        GlassCard {
            LdText("Naam", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(bottom = 8.dp))
            LdField(name, { name = it }, "Poora naam")
            LdText("Number", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(bottom = 8.dp))
            phones.forEachIndexed { i, (n, t) ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LdField(n, { v -> phones[i] = v to t }, "Phone number", phone = true, modifier = Modifier.weight(1f))
                    val label = PHONE_TYPES.firstOrNull { it.first == t }?.second ?: "Other"
                    Box(Modifier.padding(top = 2.dp).glass(p, RoundedCornerShape(14.dp), 2.dp).clickable {
                        val idx = PHONE_TYPES.indexOfFirst { it.first == t }; phones[i] = n to PHONE_TYPES[(idx + 1) % PHONE_TYPES.size].first
                    }.padding(horizontal = 12.dp, vertical = 12.dp)) { LdText(label, 12.5f, FontWeight.SemiBold, p.accent) }
                    if (phones.size > 1) Box(Modifier.padding(top = 6.dp).size(34.dp).clip(CircleShape).clickable { phones.removeAt(i) }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.X, 16.dp, p.muted) }
                }
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { phones.add("" to ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) }.padding(8.dp)) {
                LdText("+ Aur number", 13f, FontWeight.SemiBold, p.accent)
            }
            LdText("Email", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(top = 6.dp, bottom = 8.dp))
            LdField(email, { email = it }, "email@example.com")
            SwitchRow("Favourite", "Recents me upar dikhega", fav, last = true) { fav = it }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Save", enabled = name.isNotBlank() && phones.any { it.first.isNotBlank() } && canWrite) {
            val ok = Repo.saveContact(ctx, detail, name.trim(), phones.map { it.first.trim() to it.second }, email.trim(), photoBytes, fav)
            Toast.makeText(ctx, if (ok) "Contact save ho gaya" else "Save nahi hua", Toast.LENGTH_SHORT).show()
            if (ok) { act.reload(); onBack() }
        }
        if (detail != null) {
            Spacer(Modifier.height(10.dp))
            var confirm by remember { mutableStateOf(false) }
            if (!confirm) GhostButton("Contact delete karo") { confirm = true }
            else PrimaryButton("Pakka delete karo?", danger = true) {
                if (Repo.deleteContact(ctx, detail.id, detail.lookupKey)) { act.reload(); onBack() }
            }
        }
    }
}

@Composable
private fun RecordingsPage(onBack: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(Repo.recordings(ctx)) }
    var playing by remember { mutableStateOf<File?>(null) }
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) { onDispose { player[0]?.release() } }
    val df = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
    PageFrame("Recordings", onBack) {
        LdText("Note: Android 10+ pe doosre ki awaaz tabhi aati hai jab speaker on ho.", 11.5f, color = p.muted, maxLines = 3, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        if (files.isEmpty()) EmptyState(LdIcons.Record, "Koi recording nahi", "Call ke dauran Record dabao")
        files.forEach { f ->
            val isP = playing == f
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).glass(p, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBtn(if (isP) LdIcons.Pause else LdIcons.Play, 40.dp, 18.dp, on = isP) {
                    player[0]?.release(); player[0] = null
                    if (isP) playing = null else try {
                        player[0] = MediaPlayer().apply { setDataSource(f.absolutePath); prepare(); start(); setOnCompletionListener { playing = null } }
                        playing = f
                    } catch (_: Exception) { Toast.makeText(ctx, trNow("Play nahi hua"), Toast.LENGTH_SHORT).show() }
                }
                Column(Modifier.weight(1f)) {
                    LdText(f.nameWithoutExtension.removePrefix("Call_").substringBeforeLast("_").substringBeforeLast("_").replace("_", " "), 13.5f, FontWeight.SemiBold)
                    LdText(df.format(Date(f.lastModified())) + " · ${f.length() / 1024} KB", 11f, color = p.muted)
                }
                IconBtn(LdIcons.Share, 38.dp, 17.dp) {
                    val uri = FileProvider.getUriForFile(ctx, "com.mandu.liquiddialer.files", f)
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("audio/mp4").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share"))
                }
                IconBtn(LdIcons.Trash, 38.dp, 17.dp) { if (isP) { player[0]?.release(); player[0] = null; playing = null }; f.delete(); files = Repo.recordings(ctx) }
            }
        }
    }
}

@Composable
private fun NotesPage(number: String?, onBack: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    var notes by remember { mutableStateOf(if (number == null) Repo.notes(ctx) else Repo.notesFor(ctx, number)) }
    var text by remember { mutableStateOf("") }
    val df = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    PageFrame(if (number == null) "Call notes" else "Notes · ${Repo.lookupName(ctx, number)?.first ?: Repo.pretty(number)}", onBack) {
        if (number != null) GlassCard(Modifier.padding(bottom = 12.dp)) {
            LdField(text, { text = it }, "Naya note…", minLines = 2)
            PrimaryButton("Save", enabled = text.isNotBlank()) {
                Repo.addNote(ctx, number, Repo.lookupName(ctx, number)?.first ?: "", text); text = ""; notes = Repo.notesFor(ctx, number)
            }
        }
        if (notes.isEmpty()) EmptyState(LdIcons.Edit, "Koi note nahi", "Call ke dauran Notes button dabao")
        notes.forEach { n ->
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).glass(p, RoundedCornerShape(20.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        LdText(n.name.ifBlank { Repo.pretty(n.number) }, 13.5f, FontWeight.Bold)
                        LdText(df.format(Date(n.time)), 11f, color = p.muted)
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).clickable { Repo.deleteNote(ctx, n.id); notes = if (number == null) Repo.notes(ctx) else Repo.notesFor(ctx, number) },
                        contentAlignment = Alignment.Center) { LdIcon(LdIcons.Trash, 16.dp, p.muted) }
                }
                Spacer(Modifier.height(6.dp))
                LdText(n.text, 14f, maxLines = 20)
            }
        }
    }
}

@Composable
private fun StatsPage(st: AppState, onBack: () -> Unit) {
    val p = LocalLd.current
    PageFrame("Call stats", onBack) {
        val now = System.currentTimeMillis()
        listOf("Aaj" to 1, "Pichhle 7 din" to 7, "Pichhle 30 din" to 30).forEach { (label, days) ->
            val list = st.recents.filter { now - it.date <= days * 86400000L }
            val inc = list.filter { it.type == android.provider.CallLog.Calls.INCOMING_TYPE }
            val out = list.filter { it.type == android.provider.CallLog.Calls.OUTGOING_TYPE }
            val missed = list.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE || it.type == android.provider.CallLog.Calls.REJECTED_TYPE }
            SecTitle(label)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Incoming", "${inc.size}", fmtDur(inc.sumOf { it.duration }))
                    Stat("Outgoing", "${out.size}", fmtDur(out.sumOf { it.duration }))
                    Stat("Missed", "${missed.size}", "", p.danger)
                }
                Spacer(Modifier.height(10.dp))
                LdText("Kul baat: ${fmtDur(list.sumOf { it.duration })}", 13f, FontWeight.SemiBold, p.accent)
            }
        }
        SecTitle("Sabse zyada baat (30 din)")
        GlassCard {
            val top = st.recents.filter { now - it.date <= 30 * 86400000L }.groupBy { Repo.key(it.number) }
                .map { (_, v) -> v.first() to v.sumOf { it.duration } }.sortedByDescending { it.second }.take(5)
            if (top.isEmpty()) LdText("Abhi data nahi", 13f, color = p.muted)
            top.forEach { (r, d) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val nm = r.name ?: Repo.pretty(r.number)
                    Avatar(nm, r.photo, 34.dp, 12f); LdText(nm, 13.5f, FontWeight.SemiBold, modifier = Modifier.weight(1f)); LdText(fmtDur(d), 12.5f, FontWeight.Bold, p.accent)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, color: Color = LocalLd.current.text) {
    val p = LocalLd.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LdText(value, 22f, FontWeight.Bold, color); LdText(label, 11.5f, FontWeight.SemiBold, p.muted); if (sub.isNotBlank()) LdText(sub, 11f, color = p.muted)
    }
}

@Composable
private fun BlockedPage(st: AppState, act: MainActivity, onBack: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    var n by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(Repo.blocked(ctx)) }
    PageFrame("Blocked numbers", onBack) {
        if (!Repo.canBlock(ctx)) {
            GlassCard {
                LdText("Block karne ke liye Liquid Dialer ko default phone app banao", 13.5f, FontWeight.SemiBold, maxLines = 3)
                Spacer(Modifier.height(10.dp)); PrimaryButton("Default banao") { act.requestDefault() }
            }
        } else {
            GlassCard {
                LdField(n, { n = it }, "Number jo block karna hai", phone = true)
                PrimaryButton("Block karo", enabled = n.isNotBlank()) { if (Repo.block(ctx, n.trim())) { n = ""; list = Repo.blocked(ctx); act.reload() } }
            }
            Spacer(Modifier.height(14.dp))
            if (list.isEmpty()) EmptyState(LdIcons.Block, "Koi number block nahi", "Yahan ya contact sheet se block karo")
            list.forEach { b ->
                GlassRow(onClick = {}) {
                    Avatar(Repo.lookupName(ctx, b)?.first ?: b, null, seed = b)
                    Column(Modifier.weight(1f)) { LdText(Repo.lookupName(ctx, b)?.first ?: Repo.pretty(b), 14.5f, FontWeight.SemiBold); LdText(Repo.pretty(b), 11.5f, color = p.muted) }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { Repo.unblock(ctx, b); list = Repo.blocked(ctx); act.reload() }.padding(8.dp)) {
                        LdText("Unblock", 12.5f, FontWeight.SemiBold, p.accent)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SpeedDialPage(st: AppState, onBack: () -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    var editing by remember { mutableStateOf<Int?>(null) }
    PageFrame("Speed dial", onBack) {
        LdText("Dialer pe number ko lamba dabao to seedha call. 1 = voicemail.", 12f, color = p.muted, maxLines = 3, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        GlassCard {
            (2..9).forEach { k ->
                val n = s.speedDial[k]
                val nm = n?.let { num -> st.contacts.firstOrNull { Repo.key(it.number) == Repo.key(num) }?.name }
                SRow("$k  ·  " + (nm ?: n?.let { Repo.pretty(it) } ?: "Set karo"), if (nm != null) Repo.pretty(n) else null, last = k == 9,
                    trailing = { if (n != null) Box(Modifier.clip(CircleShape).clickable { LdApp.instance.scope.launch { LdApp.instance.repo.setSpeedDial(s.speedDial - k) } }.padding(6.dp)) { LdIcon(LdIcons.X, 16.dp, p.muted) } else LdIcon(LdIcons.Plus, 16.dp, p.accent) }) { editing = k }
            }
        }
    }
    val e = editing
    if (e != null) Sheet({ editing = null }) {
        var q by remember { mutableStateOf("") }
        SheetTitle("Speed dial $e")
        LdField(q, { q = it }, "Naam search karo ya number likho")
        if (q.any { it.isDigit() } && q.filter { it.isDigit() }.length >= 3) SheetRow(LdIcons.Phone, "Ye number: $q") {
            LdApp.instance.scope.launch { LdApp.instance.repo.setSpeedDial(s.speedDial + (e to Repo.normalize(q))) }; editing = null
        }
        st.contacts.filter { Repo.matches(it, q) }.take(30).forEach { c ->
            SheetRow(LdIcons.User, c.name, Repo.pretty(c.number)) { LdApp.instance.scope.launch { LdApp.instance.repo.setSpeedDial(s.speedDial + (e to c.number)) }; editing = null }
        }
    }
}

@Composable
private fun RepliesPage(onBack: () -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    var newText by remember { mutableStateOf("") }
    PageFrame("Quick replies", onBack) {
        LdText("Incoming call pe 'Message' dabao: call kategi aur ye SMS jayega.", 12f, color = p.muted, maxLines = 3, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        GlassCard {
            s.quickReplies.forEachIndexed { i, r ->
                SRow(r, last = i == s.quickReplies.lastIndex, trailing = {
                    Box(Modifier.clip(CircleShape).clickable { LdApp.instance.scope.launch { LdApp.instance.repo.setReplies(s.quickReplies.filterIndexed { j, _ -> j != i }) } }.padding(6.dp)) { LdIcon(LdIcons.Trash, 16.dp, p.muted) }
                })
            }
        }
        Spacer(Modifier.height(14.dp))
        GlassCard {
            LdField(newText, { newText = it }, "Naya message…")
            PrimaryButton("Add karo", enabled = newText.isNotBlank()) { LdApp.instance.scope.launch { LdApp.instance.repo.setReplies(s.quickReplies + newText.trim()) }; newText = "" }
        }
    }
}

@Composable
private fun PermissionsPage(st: AppState, act: MainActivity, onBack: () -> Unit) {
    val p = LocalLd.current
    val tick = st.permsTick
    PageFrame("Permissions", onBack) {
        GlassCard {
            SRow(if (st.isDefault) "Default phone app ✓" else "Default phone app — nahi", "Incoming call screen ke liye sabse zaroori", LdIcons.Phone,
                trailing = { if (!st.isDefault) LdText("Allow", 13f, FontWeight.Bold, p.accent) else LdIcon(LdIcons.Check, 18.dp, p.ok) }) { act.requestDefault() }
            RUNTIME_PERMS.forEachIndexed { i, (perm, why) ->
                val ok = act.has(perm).also { tick }
                SRow(perm.substringAfterLast('.').replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, why, last = i == RUNTIME_PERMS.lastIndex,
                    trailing = { if (ok) LdIcon(LdIcons.Check, 18.dp, p.ok) else LdText("Allow", 13f, FontWeight.Bold, p.accent) }) { if (!ok) act.askPermission(perm) }
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Sab permission do") { act.askPermissions() }
        Spacer(Modifier.height(8.dp))
        GhostButton("Settings kholo") { act.openAppSettings() }
        LdText("Agar 'Allow' dabane pe kuch na ho, to 'Settings kholo' > Permissions me jaake on karo.", 11.5f, color = p.muted, maxLines = 3, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
    }
}

@Composable
fun Onboarding(st: AppState, act: MainActivity) {
    val p = LocalLd.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(22.dp).fillMaxWidth().liquid(RoundedCornerShape(30.dp), 24.dp).background(p.bg2.copy(alpha = if (p.isLight) .5f else .7f)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .88f), p.accent))), contentAlignment = Alignment.Center) {
                LdIcon(LdIcons.Phone, 32.dp, Color.White)
            }
            Spacer(Modifier.height(14.dp))
            LdText("Liquid Dialer me swagat hai", 20f, FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            listOf("Asli calls, contacts aur call history", "Incoming call poori screen pe", "Block, recording, notes, speed dial").forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LdIcon(LdIcons.Check, 16.dp, p.ok); LdText(it, 13.5f, maxLines = 2)
                }
            }
            Spacer(Modifier.height(8.dp))
            LdText("Agle screen pe 'Liquid Dialer' chuno aur 'Set as default' / Allow dabao. Phir permissions Allow karo.", 12f, color = p.muted, maxLines = 4)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Default phone app banao") { act.finishOnboarding(); act.requestDefault() }
            Spacer(Modifier.height(8.dp))
            GhostButton("Baad me") { act.finishOnboarding(); act.askPermissions() }
        }
    }
}


/** Ek stream ka seedha system volume slider (real setStreamVolume, koi fake control nahi). */
@Composable
private fun StreamVolRow(am: AudioManager, title: String, stream: Int, last: Boolean = false) {
    val p = LocalLd.current
    val max = remember { try { am.getStreamMaxVolume(stream) } catch (_: Throwable) { 0 } }
    var v by remember { mutableIntStateOf(try { am.getStreamVolume(stream) } catch (_: Throwable) { 0 }) }
    Column {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LdText(title, 14f, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                LdText("$v/$max", 12f, color = p.muted)
            }
            Slider(v.toFloat().coerceIn(0f, max.toFloat().coerceAtLeast(1f)), { nv ->
                v = nv.roundToInt().coerceIn(0, max)
                try { am.setStreamVolume(stream, v, AudioManager.FLAG_SHOW_UI) } catch (_: Throwable) {}
            }, valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = p.accent, inactiveTrackColor = p.accent.pct(.2f)))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(if (p.isLight) Color(0x120F172A) else Color.White.copy(alpha = .07f)))
    }
}

/** Volume page: saare stream sliders + media loudness boost (honest scope: media only, SIM call kabhi nahi). */
@Composable
private fun VolumePage(onBack: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val s by LdApp.settings.collectAsState()
    val boostRunning by BoostService.running.collectAsState()
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var showAck by remember { mutableStateOf(false) }
    fun enableBoost() {
        val cur = LdApp.settings.value
        save(K.volBoost, true)
        BoostService.applyNow(ctx, cur.volBoostGain)
    }
    PageFrame("Volume", onBack) {
        GlassCard {
            StreamVolRow(am, "Ringtone volume", AudioManager.STREAM_RING)
            StreamVolRow(am, "Media volume", AudioManager.STREAM_MUSIC)
            StreamVolRow(am, "Call volume (sunne ki)", AudioManager.STREAM_VOICE_CALL)
            StreamVolRow(am, "Notification volume", AudioManager.STREAM_NOTIFICATION)
            StreamVolRow(am, "Alarm volume", AudioManager.STREAM_ALARM, last = true)
        }
        SecTitle("Media boost")
        GlassCard {
            SwitchRow("Media volume boost", "Music, video aur apps ki awaaz system max se bhi upar. SIM call ki awaaz isse NAHI badhti.", s.volBoost) { on ->
                if (on) { if (LdApp.settings.value.boostAck) enableBoost() else showAck = true }
                else { save(K.volBoost, false); BoostService.stop(ctx) }
            }
            if (s.volBoost) {
                var gain by remember(s.volBoostGain) { mutableIntStateOf(s.volBoostGain) }
                LdText("Boost: $gain% · ≈${VolumeBoost.dbLabel(gain)}", 12.5f, FontWeight.SemiBold, p.muted, Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp))
                Slider(gain.toFloat(), { gain = it.roundToInt() }, valueRange = 0f..100f,
                    onValueChangeFinished = { save(K.volBoostGain, gain); BoostService.applyNow(ctx, gain) },
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = p.accent, inactiveTrackColor = p.accent.pct(.2f)),
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("boost_gain"))
                // koi auto-restart nahi: process marne ke baad boost khud wapas nahi chalta
                if (!boostRunning) SRow("Boost dobara chalu karo", "App/process band hone par boost ruk gaya tha; ye khud wapas nahi chalta", LdIcons.Volume) { BoostService.applyNow(ctx, s.volBoostGain) }
            }
            LdText("Dhyan: boost sirf media (music/video/apps) ki awaaz ke liye hai - ye app call ki awaaz nahi badhata - call aate hi boost apne aap ruk jata hai aur call ke baad sirf tum chahu to hi dobara chalu hoga. Zyada boost se awaaz phat sakti hai aur speaker ya kaan ko nuksan ho sakta hai; hamesha kam se shuru karo. Boost chalne par chhota notification dikhega, wahan se bhi band kar sakte ho.", 11.5f,
                color = p.danger, maxLines = 8, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp))
        }
    }
    // pehli baar chalu karne par seedha risk acknowledgement
    if (showAck) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAck = false },
            title = { androidx.compose.material3.Text("Boost chalu karein?") },
            text = { androidx.compose.material3.Text("Ye sirf media (music/video/apps) ki awaaz badhata hai, SIM call ki nahi. Bahut zyada boost se awaaz phat sakti hai aur speaker ya kaan ko nuksan ho sakta hai. Kam se shuru karo; awaaz phate to turant kam ya band karo.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAck = false; save(K.boostAck, true); enableBoost() }) {
                    androidx.compose.material3.Text("Samajh gaya, chalu karo")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAck = false }) {
                    androidx.compose.material3.Text("Rehne do")
                }
            },
        )
    }
}
