package com.mandu.liquiddialer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.telecom.Call
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

@Composable
fun MainScreen(st: AppState, act: MainActivity) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val haze = remember { HazeState() }
    var query by remember { mutableStateOf("") }
    var drawer by remember { mutableStateOf(false) }
    val pages = remember { mutableStateListOf<String>() }
    var sheetNumber by remember { mutableStateOf<String?>(null) }
    var sheetContact by remember { mutableStateOf<Contact?>(null) }
    var editContactId by remember { mutableStateOf<Long?>(null) }
    var editPrefill by remember { mutableStateOf("") }
    var menuNumber by remember { mutableStateOf<String?>(null) }
    var lastTab by remember { mutableStateOf("recents") }
    val calls by CallManager.calls.collectAsState()
    val ver by CallManager.version.collectAsState()

    // search-bar mic: same privacy path as dialer voice (on-device first, else explicit consent); fills the search box only, never calls
    val searchVoice = rememberVoiceDialer { said -> query = said.trim().trimEnd('.', '\u0964') }
    // Recents tap with an active search clears it so call history opens; Contacts keeps the search
    fun goTab(k: String) { if (k == "recents" && query.isNotEmpty()) query = ""; st.tab = k }

    fun openPage(x: String) { drawer = false; pages.add(x) }
    val openEditor: (Long?, String) -> Unit = { id, pre -> editContactId = id; editPrefill = pre; sheetNumber = null; sheetContact = null; menuNumber = null; pages.add("editor") }
    val editReq = st.editContactRequest
    LaunchedEffect(editReq) {
        if (editReq != null) { st.editContactRequest = null; openEditor(editReq.first, editReq.second) }
    }

    BackHandler(enabled = drawer || query.isNotEmpty() || st.tab != "recents") {
        when { drawer -> drawer = false; query.isNotEmpty() -> query = ""; st.tab == "dialer" -> st.tab = lastTab; else -> st.tab = "recents" }
    }

    CompositionLocalProvider(LocalHaze provides haze) {
        Box(Modifier.fillMaxSize()) {
            val live = remember(ver, calls) { CallManager.primary() }
            val showBanner = live != null && s.banner && CallManager.state(live!!) != Call.STATE_RINGING
            var bannerHPx by remember { mutableIntStateOf(0) }
            val bannerPad = if (showBanner) with(LocalDensity.current) { bannerHPx.toDp() } + 10.dp else 0.dp
            // ===== haze source: blobs + screens =====
            Box(Modifier.fillMaxSize().haze(haze)) {
                BlobBackground()
                val tabKeys = listOf("recents", "contacts")
                val pager = rememberPagerState(initialPage = tabKeys.indexOf(st.tab).coerceAtLeast(0)) { tabKeys.size }
                LaunchedEffect(pager.settledPage) { val t = tabKeys[pager.settledPage]; lastTab = t; if (st.tab != t && st.tab != "dialer") st.tab = t }
                LaunchedEffect(st.tab) { if (st.tab in tabKeys) { val i = tabKeys.indexOf(st.tab); if (pager.settledPage != i && !pager.isScrollInProgress) pager.animateScrollToPage(i) } }
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().statusBarsPadding(), userScrollEnabled = s.tabSwipe) { page ->
                    when (tabKeys[page]) {
                        "recents" -> RecentsScreen(st, act, query, 86.dp + bannerPad, onOpen = { r -> sheetNumber = r.number; sheetContact = st.contacts.firstOrNull { c -> Repo.key(c.number) == Repo.key(r.number) } },
                            onFav = { c -> sheetContact = c; sheetNumber = c.number }, onLong = { menuNumber = it })
                        else -> ContactsScreen(st, act, query, 86.dp + bannerPad, onOpen = { c -> sheetContact = c; sheetNumber = c.number }, onAdd = { openEditor(null, "") }, onLong = { menuNumber = it })
                    }
                }
            }
            // ===== top bar =====
            AnimatedVisibility(st.tab != "dialer", enter = fadeIn() + slideInVertically { -it / 4 }, exit = fadeOut() + slideOutVertically { -it / 4 }) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(42.dp).liquid(CircleShape, 6.dp).testTag("drawer_open").clickable { drawer = true }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Menu) }
                    Row(Modifier.weight(1f).height(46.dp).liquid(RoundedCornerShape(18.dp), 8.dp).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LdIcon(LdIcons.Search, 18.dp, p.muted)
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) LdText("Search contacts or numbers", 14f, color = p.muted)
                            BasicTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                                textStyle = TextStyle(color = p.text, fontSize = 14.sp), cursorBrush = SolidColor(p.accent))
                        }
                        Box(Modifier.size(30.dp).clip(CircleShape).testTag("search_mic").clickable { searchVoice.trigger() }, contentAlignment = Alignment.Center) {
                            LdIcon(LdIcons.Mic, 19.dp, if (searchVoice.ui.listening) p.accent else p.muted)
                        }
                        if (query.isNotEmpty()) Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)).clickable { query = "" },
                            contentAlignment = Alignment.Center) { LdIcon(LdIcons.X, 13.dp) }
                    }
                    val pn = s.profileName.ifBlank { "You" }
                    Box(Modifier.size(44.dp).shadow(10.dp, CircleShape, ambientColor = p.accent.pct(.4f), spotColor = p.accent.pct(.4f)).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .9f), p.accent))).border(1.dp, p.stroke, CircleShape)
                        .clickable { openPage("profile") }, contentAlignment = Alignment.Center) {
                        if (s.profilePhoto.isNotBlank()) Avatar(pn, s.profilePhoto, 44.dp, 15f, seed = "me")
                        else LdText(initials(pn), 15f, FontWeight.Bold, Color.White)
                    }
                }
            }
            // ===== ongoing call banner =====
            if (showBanner) {
                CallBanner(live!!, if (st.tab == "dialer") 8.dp else 82.dp, onHeight = { bannerHPx = it })
            }
            // ===== bottom nav: rounded liquid frosted pill with sliding bubble =====
            val navTabs = listOf(Triple("recents", "Recents", LdIcons.Clock), Triple("contacts", "Contacts", LdIcons.Users))
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 14.dp).fillMaxWidth().height(66.dp).testTag("bottom_bar")
                .liquid(RoundedCornerShape(999.dp), 22.dp)
                .pointerInput(Unit) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragCancel = { dragX = 0f },
                        onDragEnd = {
                            val cur = if (st.tab == "contacts") 1 else 0
                            if (dragX < -40f && cur == 0) goTab("contacts")
                            else if (dragX > 40f && cur == 1) goTab("recents")
                            dragX = 0f
                        },
                        onHorizontalDrag = { change, amount -> change.consume(); dragX += amount }
                    )
                }) {
                BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
                    val slot = maxWidth / navTabs.size
                    val sel = navTabs.indexOfFirst { it.first == st.tab }.coerceAtLeast(0)
                    val bx by animateDpAsState(slot * sel, tween(280, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)), label = "navslide")
                    Box(Modifier.offset(x = bx).width(slot).fillMaxHeight().clip(RoundedCornerShape(999.dp))
                        .background(Brush.linearGradient(listOf(p.accent.pct(.42f), p.accent.pct(.16f))))
                        .border(1.dp, p.accent.pct(.5f), RoundedCornerShape(999.dp)))
                    Row(Modifier.fillMaxSize()) {
                        navTabs.forEach { (k, label, ic) ->
                            val on = st.tab == k
                            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(999.dp))
                                .clickable(remember { MutableInteractionSource() }, null) { goTab(k) },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                LdIcon(ic, 22.dp, if (on) p.text else p.muted)
                                Spacer(Modifier.height(3.dp))
                                LdText(label, 10.5f, FontWeight.SemiBold, if (on) p.text else p.muted, letter = .2f)
                            }
                        }
                    }
                }
            }
            // ===== round frosted dialer button above the bar (Contacts side) =====
            AnimatedVisibility(visible = st.tab != "dialer" && pages.isEmpty(),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 102.dp),
                enter = fadeIn(tween(220)) + scaleIn(tween(300, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)), initialScale = .7f),
                exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = .7f)) {
                Box(Modifier.size(80.dp).shadow(18.dp, CircleShape, ambientColor = p.accent.pct(.4f), spotColor = p.accent.pct(.4f)).testTag("dialer_fab")
                    .liquid(CircleShape, 22.dp).clickable { st.tab = "dialer" }, contentAlignment = Alignment.Center) {
                    LdIcon(LdIcons.Dialpad, 34.dp, p.accent)
                }
            }
            // ===== half-screen frosted dialer sheet =====
            if (st.tab == "dialer") Sheet(onDismiss = { st.tab = lastTab }) {
                DialerSheet(st, act, onAddContact = { openEditor(null, it) }, onLong = { menuNumber = it })
            }
            // search-bar voice overlays
            searchVoice.ui.notice?.let { n -> VoiceNoticeCard(n, Alignment.TopCenter, 118.dp) { searchVoice.ui.notice = null } }
            if (searchVoice.ui.listening) VoiceListeningCard(Alignment.Center) { searchVoice.stop() }
            if (searchVoice.ui.pendingConsent) VoiceConsentCard(Alignment.Center, onOk = { searchVoice.consentOk() }, onCancel = { searchVoice.consentCancel() })
            // ===== drawer =====
            // confirm-before-call card (setting: confirmCall)
            st.confirmNumber?.let { n ->
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable(remember { MutableInteractionSource() }, null) { st.confirmNumber = null })
                VoiceConfirmCard(VoiceMatch(if (st.contacts.firstOrNull { Repo.key(it.number) == Repo.key(n) } != null) st.contacts.first { Repo.key(it.number) == Repo.key(n) }.name else Repo.pretty(n), n),
                    Alignment.Center,
                    onCall = { st.confirmNumber = null; act.call(n) },
                    onCancel = { st.confirmNumber = null })
            }
            // 3-second undo window before dialing (setting: undoCall)
            st.undoInfo?.let { (n, _) ->
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 170.dp).clip(RoundedCornerShape(18.dp)).background(p.bg2).border(1.dp, p.stroke, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LdText("3 sec me call: ${Repo.pretty(n)}", 13f, FontWeight.SemiBold, maxLines = 1)
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { act.cancelUndo() }.padding(horizontal = 10.dp, vertical = 6.dp)) { LdText("Radd karo", 13f, FontWeight.Bold, p.danger) }
                    }
                }
            }
            if (drawer) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable(remember { MutableInteractionSource() }, null) { drawer = false })
            AnimatedVisibility(drawer, enter = slideInHorizontally(tween(360, easing = CubicBezierEasing(.22f, 1f, .36f, 1f))) { -it },
                exit = slideOutHorizontally(tween(300)) { -it }) {
                Drawer(s, st, onOpen = { openPage(it) }, onClose = { drawer = false }, onVoicemail = { drawer = false; act.voicemail() })
            }
            // ===== pages =====
            pages.forEachIndexed { i, pg ->
                key(pg + i) { CompositionLocalProvider(LocalHaze provides null) {
                    Page(pg, st, act, onBack = { pages.removeLastOrNull() }, onOpen = { pages.add(it) }, editContactId = editContactId, editPrefill = editPrefill,
                        onOpenNumber = { n -> sheetNumber = n; sheetContact = st.contacts.firstOrNull { c -> Repo.key(c.number) == Repo.key(n) } })
                } }
            }
            // ===== contact / number sheet =====
            val sn = sheetNumber
            if (sn != null) Sheet(onDismiss = { sheetNumber = null; sheetContact = null }) {
                NumberSheet(sn, sheetContact, st, act, onClose = { sheetNumber = null; sheetContact = null }, onEdit = { id -> openEditor(id, sn) },
                    onLongNumber = { menuNumber = it },
                    onNotes = { sheetNumber = null; pages.add("notes:" + sn) })
            }
            // ===== long-press number menu =====
            val mn = menuNumber
            if (mn != null) Sheet(onDismiss = { menuNumber = null }) {
                NumberMenu(mn, st, act, onClose = { menuNumber = null }, onEdit = { id -> openEditor(id, mn) })
            }
            // ===== SIM picker for outgoing =====
            val pc = st.pendingCall
            if (pc != null) Sheet(onDismiss = { st.pendingCall = null }) {
                SimPickSheet { h -> st.pendingCall = null; act.call(pc.first, h, video = pc.second) }
            }
            if (st.showOnboarding) Onboarding(st, act)
        }
    }
}

@Composable
private fun CallBanner(call: Call, bannerTopPad: androidx.compose.ui.unit.Dp, onHeight: (Int) -> Unit = {}) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val now = nowTicker()
    val audio by CallManager.audio.collectAsState()
    val connect = call.details?.connectTimeMillis ?: 0
    val t = if (CallManager.state(call) == Call.STATE_ACTIVE && connect > 0) fmtDur((now - connect) / 1000) else if (CallManager.state(call) == Call.STATE_HOLDING) "On hold" else "Calling…"
    Row(Modifier.statusBarsPadding().padding(start = 10.dp, end = 10.dp, top = bannerTopPad).onSizeChanged { onHeight(it.height) }.fillMaxWidth().liquid(RoundedCornerShape(22.dp), 22.dp)
        .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f).clickable { ctx.startActivity(Intent(ctx, com.mandu.liquiddialer.CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LiveDot()
            Column { LdText(CallManager.nameFor(ctx, call), 13f, FontWeight.Bold); LdText(t, 11f, color = p.muted) }
        }
        IconBtn(LdIcons.Mic, 38.dp, 18.dp, on = audio?.isMuted == true) { CallManager.toggleMute() }
        IconBtn(LdIcons.Volume, 38.dp, 18.dp, on = CallManager.isSpeaker()) { CallManager.toggleSpeaker() }
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFD93025)).clickable { CallManager.end(call) }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Phone, 18.dp, Color.White, Modifier.rotate(135f)) }
    }
}

@Composable
fun SectionHead(title: String, right: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        LdText(title, 19f, FontWeight.Bold, letter = -.2f); right()
    }
}

@Composable
fun DefaultBanner(act: MainActivity) {
    val p = LocalLd.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).glass(p, RoundedCornerShape(20.dp)).background(p.accent.pct(.12f)).clickable { act.requestDefault() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(p.accent.pct(.2f)), contentAlignment = Alignment.Center) { LdIcon(LdIcons.Phone, 19.dp, p.accent) }
        Column(Modifier.weight(1f)) {
            LdText("Default phone app banao", 14f, FontWeight.Bold)
            LdText("Incoming call, block aur call screen ke liye zaroori", 11.5f, color = p.muted, maxLines = 2)
        }
        LdIcon(LdIcons.Chevron, 16.dp, p.muted)
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, action: String? = null, onAction: () -> Unit = {}) {
    val p = LocalLd.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(p.glass).border(1.dp, p.stroke, CircleShape), contentAlignment = Alignment.Center) { LdIcon(icon, 26.dp, p.muted) }
        Spacer(Modifier.height(14.dp))
        LdText(title, 14.5f, FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text2(sub, 12.5f, p.muted)
        if (action != null) { Spacer(Modifier.height(14.dp)); Box(Modifier.fillMaxWidth().padding(end = 104.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(220.dp)) { PrimaryButton(action, onClick = onAction) } } }
    }
}

@Composable
fun Text2(t: String, size: Float, color: Color) = androidx.compose.material3.Text(T(t), color = color, fontSize = size.sp, textAlign = TextAlign.Center)

fun typeIcon(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> LdIcons.PhoneMissed
    CallLog.Calls.OUTGOING_TYPE -> LdIcons.PhoneOut
    else -> LdIcons.PhoneIn
}
fun typeLabel(type: Int) = when (type) {
    CallLog.Calls.MISSED_TYPE -> "Missed"; CallLog.Calls.REJECTED_TYPE -> "Declined"; CallLog.Calls.BLOCKED_TYPE -> "Blocked"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"; CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"; else -> "Incoming"
}

private sealed interface RItem
private data class RHead(val label: String) : RItem
private data class RRow(val r: Recent, val count: Int) : RItem

private fun dayLabel(ts: Long): String {
    val then = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val today = java.util.Calendar.getInstance()
    val yest = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    fun sameDay(a: java.util.Calendar, b: java.util.Calendar) =
        a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
    return when {
        sameDay(then, today) -> "Aaj"
        sameDay(then, yest) -> "Kal"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
    }
}

@Composable
private fun RecentsScreen(st: AppState, act: MainActivity, query: String, topPad: androidx.compose.ui.unit.Dp, onOpen: (Recent) -> Unit, onFav: (Contact) -> Unit, onLong: (String) -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val s by LdApp.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var filter by remember { mutableIntStateOf(0) }
    val favs = st.contacts.filter { it.starred }
    val rows = remember(st.recents, query, filter, s.groupByDay, s.recentsHideBlocked) {
        val out = mutableListOf<RItem>()
        var lastKey = ""; var lastDay = ""
        st.recents.filter { r ->
            val qok = query.isBlank() || (r.name ?: "").contains(query, true) || r.number.filter { it.isDigit() }.contains(query.filter { it.isDigit() }.ifEmpty { "\u0000" })
            val fok = when (filter) {
                1 -> r.type == CallLog.Calls.MISSED_TYPE || r.type == CallLog.Calls.REJECTED_TYPE || r.type == CallLog.Calls.BLOCKED_TYPE
                2 -> r.type == CallLog.Calls.OUTGOING_TYPE
                3 -> r.type == CallLog.Calls.INCOMING_TYPE || r.type == CallLog.Calls.VOICEMAIL_TYPE
                else -> true
            }
            qok && fok && (!s.recentsHideBlocked || r.type != CallLog.Calls.BLOCKED_TYPE)
        }.forEach { r ->
            val day = dayLabel(r.date)
            if (s.groupByDay && day != lastDay) { out += RHead(day); lastDay = day; lastKey = "" }
            val k = Repo.key(r.number) + ":" + r.type
            val prev = out.lastOrNull()
            if (k == lastKey && prev is RRow) out[out.size - 1] = RRow(prev.r, prev.count + 1)
            else out += RRow(r, 1)
            lastKey = k
        }
        out
    }
    val contactHits = remember(st.contacts, query) { if (query.isBlank()) emptyList() else st.contacts.filter { Repo.matches(it, query) }.take(8) }
    val hasLog = act.has(android.Manifest.permission.READ_CALL_LOG)
    val chipLabels = listOf("Sab", "Missed", "Outgoing", "Incoming")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPad, bottom = 190.dp + androidx.compose.foundation.layout.WindowInsets.Companion.navigationBars.asPaddingValues().calculateBottomPadding()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!st.isDefault) item { DefaultBanner(act) }
        if (s.showFavourites && favs.isNotEmpty() && query.isBlank() && filter == 0) item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LdText("FAVOURITES", 12f, FontWeight.Bold, p.muted, letter = 1.2f)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(30.dp).clip(CircleShape).testTag("fav_toggle").clickable { scope.launch { LdApp.instance.repo.set(K.showFavourites, !s.showFavourites) } }, contentAlignment = Alignment.Center) {
                        LdIcon(LdIcons.EyeOff, 17.dp, p.muted)
                    }
                    Spacer(Modifier.width(2.dp))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    items(favs, key = { it.id }) { c ->
                        Column(Modifier.width(62.dp).clickable(remember { MutableInteractionSource() }, null) { act.callUi(c.number) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.clickable { act.callUi(c.number) }) { Avatar(c.name, c.photo, 54.dp, 17f) }
                            LdText(c.name.split(" ").first(), 11f, color = p.muted)
                        }
                    }
                }
            }
        }
        if (query.isNotBlank() && contactHits.isNotEmpty()) {
            item(key = "contacts-head") { LdText("CONTACTS", 12f, FontWeight.Bold, p.muted, Modifier.padding(start = 4.dp, top = 4.dp), letter = 1.2f) }
            items(contactHits, key = { "c" + it.id }) { c ->
                GlassRow(onClick = { onFav(c) }, onLongClick = { onLong(c.number) }) {
                    Avatar(c.name, c.photo, seed = c.name)
                    Column(Modifier.weight(1f)) {
                        LdText(c.name, 14.5f, FontWeight.SemiBold)
                        LdText(c.label + "  ·  " + Repo.pretty(c.number), 11.5f, color = p.muted, modifier = Modifier.padding(top = 3.dp))
                    }
                    VideoMini { act.videoCall(c.number) }
                    CallMini { act.callUi(c.number) }
                }
            }
        }
        item {
            SectionHead("Recent calls") {
                if (st.recents.isNotEmpty()) Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { confirmClear = true }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    LdText("Clear", 12.5f, FontWeight.SemiBold, p.accent)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 2.dp)) {
                chipLabels.forEachIndexed { i, label ->
                    val on = filter == i
                    Box(Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (on) p.accent.pct(.30f) else p.glass)
                        .border(1.dp, if (on) p.accent else p.stroke, RoundedCornerShape(999.dp))
                        .clickable { filter = i }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                        LdText(label, 12f, FontWeight.SemiBold, if (on) p.text else p.muted)
                    }
                }
            }
        }
        if (!hasLog) item { EmptyState(LdIcons.Clock, "Call history ki permission chahiye", "Recents dikhane ke liye Call log permission do", "Permission do") { act.askPermissions() } }
        else if (rows.isEmpty() && contactHits.isEmpty()) item { EmptyState(LdIcons.Clock, if (query.isBlank()) "Abhi koi call nahi" else "Kuch nahi mila", if (query.isBlank()) "Aapki calls yahan dikhengi" else "Doosra naam ya number try karo") }
        if (hasLog) items(rows, key = { when (it) { is RHead -> "h-" + it.label; is RRow -> "r-" + it.r.id } }) { it2 ->
            when (it2) {
                is RHead -> LdText(it2.label, 12f, FontWeight.Bold, p.muted, Modifier.padding(start = 4.dp, top = 8.dp), letter = 1f)
                is RRow -> {
                    val r = it2.r; val count = it2.count
                    val name = r.name ?: if (r.number.isBlank()) "Private number" else Repo.pretty(r.number)
                    val missed = r.type == CallLog.Calls.MISSED_TYPE || r.type == CallLog.Calls.REJECTED_TYPE || r.type == CallLog.Calls.BLOCKED_TYPE
                    val simLabel = if (st.sims.size > 1 && s.showSimInRecents) st.sims.firstOrNull { it.handle.id == r.accountId }?.let { "SIM ${it.slot}" } else null
                    GlassRow(onClick = { onOpen(r) }, onLongClick = { if (r.number.isNotBlank()) onLong(r.number) }) {
                        Avatar(name, r.photo, size = if (s.recentsCompact) 34.dp else 46.dp, fontSize = if (s.recentsCompact) 12f else 15f, seed = name)
                        Column(Modifier.weight(1f)) {
                            LdText(name + if (count > 1) " ($count)" else "", 14.5f, FontWeight.SemiBold, if (missed) p.danger else p.text)
                            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                LdIcon(typeIcon(r.type), 13.dp, if (missed) p.danger else p.muted)
                                val timeTxt = if (s.exactTime) SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(r.date))
                                    else DateUtils.getRelativeTimeSpanString(r.date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
                                LdText("${typeLabel(r.type)}  ·  $timeTxt", 11.5f, color = if (missed) p.danger else p.muted)
                            }
                            // duration sirf connect hui calls ki (missed/declined ki duration 0/fake nahi hoti)
                            val durTxt = if (s.recentsShowDuration && r.duration > 0 &&
                                (r.type == CallLog.Calls.INCOMING_TYPE || r.type == CallLog.Calls.OUTGOING_TYPE)) fmtDur(r.duration) else null
                            val meta2 = listOfNotNull(durTxt, simLabel).joinToString("  ·  ")
                            if (meta2.isNotEmpty()) LdText(meta2, 11f, color = p.muted, modifier = Modifier.padding(top = 1.dp))
                        }
                        VideoMini { act.videoCall(r.number) }
                        CallMini { act.callUi(r.number) }
                    }
                }
            }
        }
    }
    if (confirmClear) Sheet(onDismiss = { confirmClear = false }) {
        SheetTitle("Saari call history delete karein?")
        LdText("Ye phone ki call log se hamesha ke liye hat jayegi.", 13f, color = p.muted, maxLines = 3)
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Haan, clear karo", danger = true) { Repo.clearRecents(ctx); confirmClear = false; act.reload() }
        Spacer(Modifier.height(8.dp))
        GhostButton("Rehne do") { confirmClear = false }
    }
}

/** .row glass list item */
@Composable
fun GlassRow(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val p = LocalLd.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .985f)
    Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s }.glass(p, RoundedCornerShape(20.dp), 8.dp, .20f)
        .combinedClickable(src, null, onLongClick = onLongClick, onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** .call-mini */
@Composable
fun CallMini(onClick: () -> Unit) {
    val p = LocalLd.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .9f)
    Box(Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(44.dp).clip(CircleShape)
        .background(Brush.linearGradient(listOf(p.accent.pct(.30f), p.accent.pct(.12f)))).border(1.dp, p.accent.pct(.45f), CircleShape)
        .clickable(src, null, onClick = onClick), contentAlignment = Alignment.Center) { LdIcon(LdIcons.Phone, 20.dp, p.accent) }
}

/** .video-mini: seedhi native video call (ViLTE); support na mile to call screen saaf batata hai */
@Composable
fun VideoMini(onClick: () -> Unit) {
    val p = LocalLd.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .9f)
    Box(Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(44.dp).clip(CircleShape)
        .background(Brush.linearGradient(listOf(p.accent.pct(.14f), p.accent.pct(.06f)))).border(1.dp, p.accent.pct(.25f), CircleShape)
        .clickable(src, null, onClick = onClick).testTag("video_mini"), contentAlignment = Alignment.Center) { LdIcon(LdIcons.Video, 20.dp, p.accent) }
}

@Composable
private fun ContactsScreen(st: AppState, act: MainActivity, query: String, topPad: androidx.compose.ui.unit.Dp, onOpen: (Contact) -> Unit, onAdd: () -> Unit, onLong: (String) -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val list = remember(st.contacts, query) { st.contacts.filter { Repo.matches(it, query) } }
    val hasPerm = act.has(android.Manifest.permission.READ_CONTACTS)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPad, bottom = 150.dp + androidx.compose.foundation.layout.WindowInsets.Companion.navigationBars.asPaddingValues().calculateBottomPadding()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionHead("Contacts") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LdText("${list.size}", 11.5f, FontWeight.Bold, p.muted, Modifier.clip(RoundedCornerShape(999.dp)).background(p.glass).border(1.dp, p.stroke, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                    Box(Modifier.size(32.dp).glass(p, CircleShape, 2.dp).clickable { onAdd() }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Plus, 16.dp, p.accent) }
                }
            }
        }
        if (!hasPerm) item { EmptyState(LdIcons.Users, "Contacts ki permission chahiye", "Contacts dikhane ke liye permission do", "Permission do") { act.askPermissions() } }
        else if (list.isEmpty()) item { EmptyState(LdIcons.Users, if (query.isBlank()) "Koi contact nahi" else "Kuch nahi mila", if (query.isBlank()) "Naya contact add karo" else "Naam, number ya T9 se search karo", if (query.isBlank()) "Contact add karo" else null, onAdd) }
        items(list, key = { it.id }) { c ->
            val display = if (s.lastNameFirst) c.altName else c.name
            GlassRow(onClick = { onOpen(c) }, onLongClick = { onLong(c.number) }) {
                Avatar(c.name, c.photo, seed = c.name)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        LdText(display, 14.5f, FontWeight.SemiBold, modifier = Modifier.weight(1f, false))
                        if (c.starred) LdIcon(LdIcons.StarFill, 13.dp, Color(0xFFF5C542))
                    }
                    Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        LdText(c.label, 11.5f, color = p.muted); LdText("·", 11.5f, color = p.muted.copy(alpha = .5f)); LdText(Repo.pretty(c.number), 11.5f, color = p.muted)
                    }
                }
                CallMini { act.call(c.number) }
            }
        }
    }
}

fun formatDial(v: String): String {
    if (v.isEmpty() || v == "+") return v
    return if (v.startsWith("+")) v.take(3) + " " + v.drop(3).chunked(5).joinToString(" ") else v.chunked(5).joinToString(" ")
}

@Composable
private fun DialerSheet(st: AppState, act: MainActivity, onAddContact: (String) -> Unit, onLong: (String) -> Unit) {
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val ctx = LocalContext.current
    val dial = st.dial
    val sims = st.sims
    val simIdx = sims.indexOfFirst { it.handle.id == s.defaultSim }
    val simText = if (simIdx >= 0) "SIM ${simIdx + 1}" else if (sims.size > 1) "SIM: Ask" else "SIM 1"
    val scope = rememberCoroutineScope()
    fun cycleSim() {
        if (sims.size < 2) return
        val next = if (simIdx + 1 >= sims.size) "ask" else sims[simIdx + 1].handle.id
        scope.launch { LdApp.instance.repo.set(K.defaultSim, next) }
    }
    val matches = remember(dial, st.contacts) { if (dial.length < 2) emptyList() else st.contacts.filter { Repo.matches(it, dial) }.take(3) }
    // live suggestions while typing: saved contacts first, then numbers from call history (local data only; tapping fills the number, never calls)
    val suggest = remember(dial, st.contacts, st.recents) {
        if (dial.length < 2) emptyList() else {
            val dd = dial.filter { it.isDigit() }
            val out = LinkedHashMap<String, Pair<String, String>>()
            st.contacts.filter { Repo.matches(it, dial) }.take(6).forEach { out.putIfAbsent(Repo.key(it.number), it.name to it.number) }
            if (dd.length >= 2) st.recents.filter { r -> r.number.filter { it.isDigit() }.contains(dd) }.take(40).forEach { r ->
                out.putIfAbsent(Repo.key(r.number), (r.name?.takeIf { it.isNotBlank() } ?: Repo.pretty(r.number)) to r.number)
            }
            out.values.filter { Repo.key(it.second) != Repo.key(dial) }.take(4).toList()
        }
    }
    var voiceConfirmHolder by remember { mutableStateOf<VoiceMatch?>(null) }
    var voiceNoticeHolder by remember { mutableStateOf<String?>(null) }
    val voice = rememberVoiceDialer { said ->
        val m = matchVoiceCommand(said, st.contacts)
        if (m != null) voiceConfirmHolder = m else voiceNoticeHolder = "Contact nahi mila: $said"
    }
    // digit keys keep their width-based size; dialerScale only shrinks the dialer, it never stretches the sheet into dead gaps
    val scrW = LocalConfiguration.current.screenWidthDp
    val natKey = minOf((scrW * .19f).coerceIn(54f, 92f), (scrW - 52f) / 3.55f) * s.dialerScale.coerceIn(0.8f, 1f)
    // sheet wraps its content: small fixed gaps only, so no empty space above or below the keypad
    BoxWithConstraints(Modifier.fillMaxWidth().wrapContentHeight()) {
        val keyByW = (maxWidth.value - 16f) / 3.55f
        var dialMenu by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SimChip(if (sims.size > 1 && simIdx >= 0) "$simText · ${sims[simIdx].label}" else simText, onClick = { cycleSim() })
                Row(Modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).testTag("voice_mic").clickable { voice.trigger() }, contentAlignment = Alignment.Center) {
                    LdIcon(LdIcons.Mic, 21.dp, if (voice.ui.listening) p.accent else p.muted)
                }
                Box(Modifier.size(32.dp).clip(CircleShape).testTag("dial_menu").clickable { dialMenu = !dialMenu }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Dots, 21.dp, p.muted) }
            }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val nameMatch = matches.firstOrNull { Repo.key(it.number) == Repo.key(dial) && dial.length >= 6 }
                LdText(if (s.formatNumber) formatDial(dial) else dial, (if (dial.length > 14) 24f else 30f), FontWeight.SemiBold, letter = 2f, maxLines = 1,
                    modifier = Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = {}, onLongClick = { if (dial.isNotEmpty()) onLong(dial) }))
                Spacer(Modifier.height(3.dp))
                if (suggest.isNotEmpty() && nameMatch == null) {
                    LazyRow(Modifier.fillMaxWidth().padding(top = 4.dp).testTag("dial_suggest"), horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        items(suggest, key = { it.second }) { (nm, num) ->
                            Box(Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(999.dp)).background(p.accent.pct(.14f)).clickable { st.dial = num.filter { c -> c.isDigit() || c in "+*#" } }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                LdText(if (nm == Repo.pretty(num)) nm else nm.split(" ").first() + " · " + Repo.pretty(num), 11.5f, FontWeight.SemiBold, p.accent, maxLines = 1)
                            }
                        }
                    }
                } else LdText(when { dial.isEmpty() -> "Number likho"; nameMatch != null -> nameMatch.name; else -> "" }, 12.5f, color = p.muted, letter = .4f)
                if (dial.isNotEmpty() && nameMatch == null && dial.length >= 3) {
                    Box(Modifier.padding(top = 3.dp).clip(RoundedCornerShape(12.dp)).clickable { onAddContact(dial) }.padding(horizontal = 10.dp, vertical = 2.dp)) {
                        LdText("+ Contact me add karo", 12f, FontWeight.SemiBold, p.accent)
                    }
                }
            }
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Keypad(onKey = { k -> st.dial = st.dial + k }, onLong = { k ->
                    when (k) {
                        "0" -> st.dial = st.dial + "+"
                        "1" -> if (st.dial.isEmpty()) act.voicemail() else st.dial += "1"
                        else -> {
                            val n = k.toIntOrNull()?.let { s.speedDial[it] }
                            if (st.dial.isEmpty() && n != null) act.call(n)
                            else if (st.dial.isEmpty() && k.toIntOrNull() != null) Toast.makeText(ctx, "Speed dial $k set nahi hai (Settings > Speed dial)", Toast.LENGTH_SHORT).show()
                            else st.dial += k
                        }
                    }
                }, maxKey = minOf(natKey, keyByW).dp, letters = s.keypadLetters, rowGap = 5.dp, tag = "dial_keypad")
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).glass(p, CircleShape, 6.dp).combinedClickable(onLongClick = { st.dial = "" }) { st.dial = st.dial.dropLast(1) },
                    contentAlignment = Alignment.Center) { LdIcon(LdIcons.Backspace, 27.dp) }
                val ready = dial.isNotEmpty()
                val src = remember { MutableInteractionSource() }
                val sc = pressScale(src, .94f)
                Box(Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.size(64.dp).testTag("call_btn").alpha(if (ready) 1f else .55f)
                    .shadow(16.dp, CircleShape, ambientColor = p.accent.pct(.42f), spotColor = p.accent.pct(.42f)).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .88f), p.accent)))
                    .border(1.dp, p.accent.pct(.55f), CircleShape)
                    .clickable(src, null) {
                        if (dial.isNotEmpty()) { act.call(dial); st.dial = "" }
                        else st.recents.firstOrNull { it.type == CallLog.Calls.OUTGOING_TYPE }?.let { st.dial = it.number }
                    }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Phone, 30.dp, Color.White) }
                Box(Modifier.size(60.dp).glass(p, CircleShape, 6.dp).clickable { cycleSim() }, contentAlignment = Alignment.Center) {
                    LdText(if (simIdx >= 0) "SIM${simIdx + 1}" else if (sims.size > 1) "ASK" else "SIM1", 11f, FontWeight.ExtraBold, letter = .4f)
                }
            }
        }
            // voice dialing overlays
        voice.ui.notice?.let { n -> VoiceNoticeCard(n, Alignment.TopCenter, 118.dp) { voice.ui.notice = null } }
        voiceNoticeHolder?.let { n -> VoiceNoticeCard(n, Alignment.TopCenter, 118.dp) { voiceNoticeHolder = null } }
        if (voice.ui.listening) VoiceListeningCard(Alignment.Center) { voice.stop() }
        if (voice.ui.pendingConsent) VoiceConsentCard(Alignment.Center, onOk = { voice.consentOk() }, onCancel = { voice.consentCancel() })
        voiceConfirmHolder?.let { m ->
            VoiceConfirmCard(m, Alignment.Center,
                onCall = { voiceConfirmHolder = null; act.call(m.number) },
                onCancel = { voiceConfirmHolder = null })
        }
        if (dialMenu) {
                Column(Modifier.align(Alignment.TopEnd).padding(top = 96.dp, end = 8.dp).width(200.dp).clip(RoundedCornerShape(16.dp)).background(p.bg2).border(1.dp, p.stroke, RoundedCornerShape(16.dp)).padding(vertical = 6.dp).testTag("dial_menu_popup")) {
                    Row(Modifier.fillMaxWidth().clickable {
                        dialMenu = false
                        val clip = ctx.getSystemService(ClipboardManager::class.java)?.primaryClip
                        val t = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                        val clean = t.filter { it.isDigit() || it in "+*#,;" }
                        if (clean.isNotEmpty()) st.dial += clean
                    }.padding(horizontal = 14.dp, vertical = 11.dp)) { LdText("Number paste karo", 14f, FontWeight.SemiBold) }
                    Row(Modifier.fillMaxWidth().clickable { dialMenu = false; if (st.dial.isNotEmpty()) st.dial += "," }.padding(horizontal = 14.dp, vertical = 11.dp)) { LdText("Pause add karo (,)", 14f, FontWeight.SemiBold) }
                    Row(Modifier.fillMaxWidth().clickable { dialMenu = false; if (st.dial.isNotEmpty()) st.dial += ";" }.padding(horizontal = 14.dp, vertical = 11.dp)) { LdText("Wait add karo (;)", 14f, FontWeight.SemiBold) }
                }
            }
    }
}

@Composable
private fun Drawer(s: LdSettings, st: AppState, onOpen: (String) -> Unit, onClose: () -> Unit, onVoicemail: () -> Unit) {
    val p = LocalLd.current
    val w = LocalConfiguration.current.screenWidthDp
    Column(Modifier.fillMaxHeight().width(minOf(w * .86f, 340f).dp).liquid(RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp), 24.dp)
        .background(p.bg2.copy(alpha = if (p.isLight) .4f else .55f))
        .clickable(remember { MutableInteractionSource() }, null) {}
        .systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val name = s.profileName.ifBlank { "You" }
            val ctx = LocalContext.current
            val own = remember { Repo.ownNumbers(ctx) }
            Box(Modifier.clip(CircleShape).clickable { onOpen("profile") }) { Avatar(name, s.profilePhoto.ifBlank { null }, 62.dp, 20f, glow = true, seed = "me") }
            Column(Modifier.weight(1f)) {
                LdText(name, 16f, FontWeight.Bold)
                LdText(s.profileNumber.ifBlank { own.firstOrNull()?.substringAfter(": ") ?: "My profile set karo" }, 12f, color = p.muted)
            }
            IconBtn(LdIcons.X, onClick = onClose)
        }
        @Composable
        fun item(ic: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LdIcon(ic, 20.dp, p.muted); LdText(label, 14f, FontWeight.Medium, modifier = Modifier.weight(1f)); LdIcon(LdIcons.Chevron, 16.dp, p.muted.copy(alpha = .45f))
            }
        }
        item(LdIcons.Settings, "Settings") { onOpen("settings") }
        item(LdIcons.Palette, "Theme & appearance") { onOpen("appearance") }
        item(LdIcons.User, "My profile (naam + photo)") { onOpen("profile") }
        item(LdIcons.Dialpad, "Calling card layout") { onOpen("calllayout") }
        item(LdIcons.Record, "Recordings") { onOpen("recordings") }
        item(LdIcons.Edit, "Call notes") { onOpen("notes") }
        item(LdIcons.Clock, "Call stats") { onOpen("stats") }
        item(LdIcons.Block, "Blocked numbers") { onOpen("blocked") }
        item(LdIcons.Voicemail, "Voicemail") { onVoicemail() }
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 6.dp)) {
            LdText("Liquid Dialer", 12.5f, FontWeight.Bold)
            LdText("v${com.mandu.liquiddialer.BuildConfigHelper.version} · Frosted glass calling", 11.5f, color = p.muted)
            LdText(if (st.isDefault) "Default phone app ✓" else "Abhi default phone app nahi hai", 11.5f, color = if (st.isDefault) p.ok else p.danger)
        }
    }
}
