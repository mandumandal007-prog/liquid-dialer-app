package com.mandu.liquiddialer.ui

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.telecom.CallManager
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

fun fmtDur(sec: Long): String { val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s) }

@Composable
fun nowTicker(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(500); now = System.currentTimeMillis() } }
    return now
}

/** Call screen background: "blobs" (default), "photo" (caller photo, blurred), "solid" (flat). */
@Composable
fun CallBackground(modifier: Modifier = Modifier, style: String = "blobs", photo: String? = null) {
    val p = LocalLd.current
    Box(modifier.fillMaxSize()) {
        when (style) {
            "solid" -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bg2, p.bg))))
            "photo" -> {
                val bmp = rememberPhoto(photo)
                if (bmp != null) {
                    Image(bmp, null, Modifier.fillMaxSize().then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(70.dp, BlurredEdgeTreatment.Unbounded) else Modifier), contentScale = ContentScale.Crop, alpha = .6f)
                    Box(Modifier.fillMaxSize().background(p.bg.copy(alpha = .62f)))
                } else {
                    BlobBackground(strong = true)
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(0f to p.accent.pct(.28f), .62f to Color.Transparent, center = Offset(540f, 0f), radius = 1400f)))
                }
            }
            else -> {
                BlobBackground(strong = true)
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(0f to p.accent.pct(.28f), .62f to Color.Transparent, center = Offset(540f, 0f), radius = 1400f)))
            }
        }
    }
}

@Composable
fun CallBtn(icon: ImageVector, label: String, on: Boolean = false, size: Dp = 68.dp, enabled: Boolean = true, flat: Boolean = false, onClick: () -> Unit) {
    val p = LocalLd.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .93f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.alpha(if (enabled) 1f else .4f)) {
        val base = Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(size)
        val m = when {
            flat && on -> base.clip(CircleShape).background(p.accent)
            flat -> base.clip(CircleShape).background(p.text.copy(alpha = .10f))
            on -> base.shadow(14.dp, CircleShape, ambientColor = p.accent.pct(.42f), spotColor = p.accent.pct(.42f)).clip(CircleShape)
                .background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .86f), p.accent)))
            else -> base.glass(p, CircleShape, 12.dp, .32f)
        }
        Box(m.clickable(src, null, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            LdIcon(icon, size * .38f, if (on) Color.White else p.text)
        }
        LdText(label, (size.value * .165f).coerceIn(9.5f, 12.5f), FontWeight.SemiBold, p.muted, maxLines = 1)
    }
}

@Composable
fun EndCallBtn(size: Dp = 74.dp, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .94f)
    Box(Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(size)
        .shadow(8.dp, CircleShape, ambientColor = Color(0x59000000), spotColor = Color(0x59000000)).clip(CircleShape)
        .background(Color(0xFFD93025))
        .clickable(src, null, onClick = onClick), contentAlignment = Alignment.Center) {
        LdIcon(LdIcons.Phone, size * .42f, Color.White, Modifier.rotate(135f))
    }
}

@Composable
fun CallScreen(onFinish: () -> Unit, onRecord: () -> Unit, onAddCall: () -> Unit, onMinimize: () -> Unit) {
    val ver by CallManager.version.collectAsState()
    val calls by CallManager.calls.collectAsState()
    val audio by CallManager.audio.collectAsState()
    val rec by CallManager.recording.collectAsState()
    val s by LdApp.settings.collectAsState()
    val ctx = LocalContext.current
    val haze = remember { HazeState() }
    val call = remember(ver, calls) { CallManager.primary() }
    var sheet by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        CallBackground(Modifier.haze(haze), s.callBgStyle, call?.let { CallManager.photoFor(it) })
        CompositionLocalProvider(LocalHaze provides haze) {
            if (call == null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    LdText("Call ended", 20f, FontWeight.SemiBold)
                }
            } else {
                val st = CallManager.state(call)
                if (st == Call.STATE_RINGING) IncomingContent(call, s.swipeAnswer, onMessage = { sheet = "reply" })
                else OngoingContent(call, audio, rec, onRecord, onAddCall, onMinimize, openSheet = { sheet = it })
                if (st == Call.STATE_SELECT_PHONE_ACCOUNT && sheet == null) sheet = "sim"
            }
            val c = call
            if (sheet != null && c != null) {
                Sheet(onDismiss = { sheet = null }) {
                    when (sheet) {
                        "reply" -> QuickReplySheet(s.quickReplies) { msg -> CallManager.reject(c, msg); sheet = null }
                        "notes" -> NotesSheet(CallManager.number(c), CallManager.nameFor(ctx, c)) { sheet = null }
                        "audio" -> AudioRouteSheet(audio) { CallManager.setRoute(it); sheet = null }
                        "sim" -> SimPickSheet { h -> c.phoneAccountSelected(h, false); sheet = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopChips(call: Call, showMin: Boolean, onMinimize: () -> Unit) {
    val ctx = LocalContext.current
    val sims = remember { Repo.sims(ctx) }
    val simLabel = sims.firstOrNull { it.handle == call.details?.accountHandle }?.let { if (sims.size > 1) "SIM ${it.slot} · ${it.label}" else it.label } ?: "SIM"
    Box(Modifier.fillMaxWidth()) {
        if (showMin) Box(Modifier.align(Alignment.CenterStart)) { IconBtn(LdIcons.ChevronDown, onClick = onMinimize) }
        Box(Modifier.align(Alignment.Center)) { SimChip(simLabel) }
        Box(Modifier.align(Alignment.CenterEnd)) { SimChip("HD", LocalLd.current.ok, live = true) }
    }
}

@Composable
private fun Hero(call: Call, status: String, statusColor: Color, timer: String?, badges: List<Pair<String, Color>>, ringing: Boolean) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    val s by LdApp.settings.collectAsState()
    val heroScale = s.callCardScale
    val aShape = if (s.callAvatarShape == "squircle") RoundedCornerShape(28) else CircleShape
    val name = CallManager.nameFor(ctx, call)
    val number = CallManager.number(call)
    val inf = rememberInfiniteTransition(label = "floaty")
    val fy by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(if (ringing) 2000 else 2750, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fy")
    val pr by inf.animateFloat(1f, 1.035f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pr")
    val sh = LocalConfiguration.current.screenHeightDp
    val av = (when { sh < 640 -> 76.dp; sh < 740 -> 88.dp; else -> 100.dp }) * heroScale
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.graphicsLayer {
            translationY = if (p.motion) -7.dp.toPx() * fy else 0f
            if (ringing && p.motion) { scaleX = pr; scaleY = pr }
        }) { Avatar(name, CallManager.photoFor(call), av, av.value * .29f, glow = true, seed = name, shape = aShape) }
        Spacer(Modifier.height(4.dp))
        LdText(name, (if (sh < 700) 21f else 23f) * heroScale, FontWeight.Bold, letter = -.3f, maxLines = 1)
        if (s.callShowNumber && number.isNotBlank() && name != Repo.pretty(number)) LdText(Repo.pretty(number), 13f, color = p.muted, letter = .3f)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 6.dp)) {
            if (ringing) LiveDot()
            LdText(status, 12.5f, FontWeight.SemiBold, statusColor)
        }
        if (timer != null) LdText(timer, 20f, FontWeight.SemiBold, letter = 2f)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.heightIn(min = 22.dp).padding(top = 8.dp)) {
            badges.forEach { (t, c) ->
                Row(Modifier.glass(p, RoundedCornerShape(999.dp), 0.dp).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (t.startsWith("REC")) {
                        val inf2 = rememberInfiniteTransition(label = "blink")
                        val a by inf2.animateFloat(1f, .25f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a")
                        Box(Modifier.size(6.dp).alpha(a).background(p.danger, CircleShape))
                    }
                    LdText(t, 10f, FontWeight.Bold, c, letter = .6f)
                }
            }
        }
    }
}

@Composable
private fun OngoingContent(call: Call, audio: CallAudioState?, rec: CallManager.RecInfo?, onRecord: () -> Unit, onAddCall: () -> Unit,
                           onMinimize: () -> Unit, openSheet: (String) -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val now = nowTicker()
    val st = CallManager.state(call)
    var keypad by remember { mutableStateOf(false) }
    var dtmf by remember { mutableStateOf("") }
    val connect = call.details?.connectTimeMillis ?: 0L
    val status = when (st) {
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SIM chuno"
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_ACTIVE -> if (call.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true) "Conference" else "Connected"
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Call ended"
        else -> "…"
    }
    val timer = if (st == Call.STATE_ACTIVE && connect > 0) fmtDur((now - connect) / 1000) else if (st == Call.STATE_DIALING) "00:00" else null
    val muted = audio?.isMuted == true
    val speaker = audio?.route == CallAudioState.ROUTE_SPEAKER
    val bt = audio?.route == CallAudioState.ROUTE_BLUETOOTH
    val badges = buildList {
        add("HD VOICE" to p.ok)
        if (rec != null) add("REC ${fmtDur((now - rec.started) / 1000)}" to p.danger)
        if (muted) add("MUTED" to p.muted)
        if (st == Call.STATE_HOLDING) add("ON HOLD" to p.muted)
        if (speaker) add("SPEAKER" to p.muted)
        if (bt) add("BLUETOOTH" to p.muted)
    }
    val others = CallManager.others()

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) {
        TopChips(call, true, onMinimize)
        if (others.isNotEmpty()) {
            val o = others.first()
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().glass(p, RoundedCornerShape(20.dp)).clickable { CallManager.swap() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(CallManager.nameFor(ctx, o), CallManager.photoFor(o), 36.dp, 13f)
                Column(Modifier.weight(1f)) {
                    LdText(CallManager.nameFor(ctx, o), 13.5f, FontWeight.SemiBold)
                    LdText(if (CallManager.state(o) == Call.STATE_HOLDING) "On hold · tap to swap" else "Doosri call", 11.5f, color = p.muted)
                }
                LdIcon(LdIcons.Swap, 20.dp, p.accent)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (!keypad) Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Hero(call, status, p.accent, timer, badges, false) }
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LdText(CallManager.nameFor(ctx, call) + (timer?.let { "  ·  $it" } ?: ""), 13f, FontWeight.SemiBold, p.muted)
                LdText(dtmf.ifEmpty { " " }, 30f, FontWeight.SemiBold, letter = 2f)
                Keypad(onKey = { k -> dtmf += k; CallManager.dtmf(k[0]) }, keyDp = (LocalConfiguration.current.screenHeightDp * .095f).coerceIn(58f, 84f).dp)
                Row(Modifier.glass(p, RoundedCornerShape(999.dp), 4.dp).clickable { keypad = false }.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LdIcon(LdIcons.X, 16.dp); LdText("Keypad band karo", 13f, FontWeight.SemiBold)
                }
            }
        }
        val s by LdApp.settings.collectAsState()
        val gridL = remember(s.layoutGrid) { parseLayout(s.layoutGrid, GRID_IDS) }
        val bottomL = remember(s.layoutBottom) { parseLayout(s.layoutBottom, BOTTOM_IDS) }
        val audioVisible = gridL.any { it.first == "audio" && it.second }
        val spec: (String) -> BtnSpec? = { id ->
            when (id) {
                "mute" -> BtnSpec(LdIcons.Mic, if (muted) "Unmute" else "Mute", muted) { CallManager.toggleMute() }
                "keypad" -> BtnSpec(LdIcons.Dialpad, "Keypad") { keypad = true }
                "record" -> BtnSpec(LdIcons.Record, if (rec != null) "Stop rec" else "Record", rec != null, onClick = onRecord)
                "hold" -> BtnSpec(LdIcons.Pause, if (st == Call.STATE_HOLDING) "Resume" else "Hold", st == Call.STATE_HOLDING,
                    call.details?.can(Call.Details.CAPABILITY_HOLD) == true || st == Call.STATE_HOLDING) { CallManager.toggleHold(call) }
                "add" -> BtnSpec(LdIcons.Plus, "Add call", onClick = onAddCall)
                "merge" -> when {
                    others.isNotEmpty() && CallManager.canMerge() -> BtnSpec(LdIcons.Merge, "Merge") { CallManager.merge() }
                    others.isNotEmpty() -> BtnSpec(LdIcons.Swap, "Swap") { CallManager.swap() }
                    !audioVisible -> BtnSpec(if (bt) LdIcons.Bluetooth else LdIcons.Volume, "Audio", bt) { openSheet("audio") }
                    else -> BtnSpec(LdIcons.Merge, "Merge", enabled = false) {}
                }
                "audio" -> BtnSpec(if (bt) LdIcons.Bluetooth else LdIcons.Volume, "Audio", bt) { openSheet("audio") }
                "speaker" -> BtnSpec(LdIcons.SpeakerFill, if (speaker) "Speaker on" else "Speaker", speaker, flat = true) {
                    if (CallManager.hasBluetooth() && !speaker) openSheet("audio") else CallManager.toggleSpeaker()
                }
                "notes" -> BtnSpec(LdIcons.Edit, "Notes") { openSheet("notes") }
                else -> null
            }
        }
        if (!keypad) {
            Box(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                CallControlsLayout(gridL, bottomL, s.gridScale, s.bottomScale, spec, onEnd = { CallManager.end(call) })
            }
        } else {
            // keypad open: only the bottom row stays
            CallControlsLayout(emptyList(), bottomL, s.gridScale, s.bottomScale, spec, onEnd = { CallManager.end(call) }, compact = true)
        }
    }
}

@Composable
private fun IncomingContent(call: Call, swipe: Boolean, onMessage: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val sims = remember { Repo.sims(ctx) }
    val sim = sims.firstOrNull { it.handle == call.details?.accountHandle }?.let { "SIM ${it.slot}" } ?: "SIM"
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 26.dp)) {
        TopChips(call, false) {}
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Hero(call, "Incoming call · $sim", p.accent, null, emptyList(), true)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            SwipeCallBtn(danger = true, label = "Cut", swipe = swipe) { CallManager.reject(call) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(52.dp).glass(p, CircleShape, 6.dp).clickable { onMessage() }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Message, 22.dp) }
                LdText("Message", 11f, FontWeight.SemiBold, p.muted)
                Spacer(Modifier.height(18.dp))
            }
            SwipeCallBtn(danger = false, label = "Answer", swipe = swipe) { CallManager.answer(call) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SwipeCallBtn(danger: Boolean, label: String, swipe: Boolean, onTrigger: () -> Unit) {
    val p = LocalLd.current
    val scope = rememberCoroutineScope()
    val off = remember { Animatable(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 110.dp.toPx() }
    var fired by remember { mutableStateOf(false) }
    val sw = LocalConfiguration.current.screenWidthDp
    val btn = (sw * .19f).coerceIn(62f, 76f).dp
    val inf = rememberInfiniteTransition(label = "glow")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    val hint by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "hint")
    val base = if (danger) Color(0xFFD93025) else Color(0xFF1EA446)
    val pull = (-off.value / threshold).coerceIn(0f, 1.2f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (swipe) {
            // three stacked chevrons that light up bottom -> top: "swipe up"
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(1f - pull.coerceIn(0f, 1f)).padding(bottom = 2.dp)) {
                for (i in 2 downTo 0) {
                    val ph = ((hint * 3f) - i).let { if (it < 0f) it + 3f else it }
                    val a = (1f - kotlin.math.abs(ph - .5f) * 1.6f).coerceIn(.18f, 1f)
                    LdIcon(LdIcons.ChevronDown, 18.dp, base.copy(alpha = a), Modifier.rotate(180f).offset(y = (i * -4).dp))
                }
                LdText("Upar khiskao", 10.5f, FontWeight.SemiBold, p.muted)
            }
        }
        Box(Modifier
            .offset { IntOffset(0, off.value.roundToInt()) }
            .size(btn * 1.6f)
            .then(
                if (swipe) Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (-off.value >= threshold && !fired) { fired = true; onTrigger() }
                                else off.animateTo(0f, spring(dampingRatio = .45f, stiffness = 380f))
                            }
                        },
                        onDragCancel = { scope.launch { off.animateTo(0f, spring(.45f, 380f)) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { off.snapTo((off.value + drag).coerceIn(-threshold * 1.4f, 0f)) }
                    }
                } else Modifier.clickable(remember { MutableInteractionSource() }, null) { if (!fired) { fired = true; onTrigger() } }
            ), contentAlignment = Alignment.Center) {
            // glow + expanding pulse rings
            Canvas(Modifier.matchParentSize()) {
                val r0 = size.minDimension / 1.6f / 2f
                drawCircle(Brush.radialGradient(0f to base.copy(alpha = .38f + .25f * pull), 1f to Color.Transparent, center = center, radius = r0 * 1.6f), r0 * 1.6f)
                if (p.motion) for (k in 0..1) {
                    val t = (pulse + k * .5f) % 1f
                    drawCircle(base.copy(alpha = .55f * (1f - t)), r0 + r0 * .55f * t, style = Stroke((3.dp.toPx() * (1f - t)) + .6f))
                }
            }
            // flat Google-Dialer style circle
            Box(Modifier
                .graphicsLayer { scaleX = 1f + .08f * pull; scaleY = 1f + .08f * pull }
                .size(btn)
                .shadow(8.dp, CircleShape, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
                .clip(CircleShape)
                .background(base), contentAlignment = Alignment.Center) {
                // handset (red = handset down)
                val ic = btn * .42f
                val rot = if (danger) 135f else 0f
                LdIcon(LdIcons.Phone, ic, Color.White, Modifier.rotate(rot))
            }
        }
        LdText(label, 12.5f, FontWeight.SemiBold, p.text.copy(alpha = .85f))
    }
}

@Composable
fun QuickReplySheet(replies: List<String>, onSend: (String) -> Unit) {
    val p = LocalLd.current
    var custom by remember { mutableStateOf("") }
    SheetTitle("Message bhejo aur call kaato")
    replies.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(p, RoundedCornerShape(16.dp), 2.dp).clickable { onSend(r) }.padding(14.dp)) {
            LdText(r, 14f, maxLines = 3)
        }
    }
    Spacer(Modifier.height(8.dp))
    LdField(custom, { custom = it }, "Apna message likho…")
    PrimaryButton("Bhejo", enabled = custom.isNotBlank()) { onSend(custom) }
}

@Composable
fun NotesSheet(number: String, name: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    var text by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(Repo.notesFor(ctx, number)) }
    val df = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    SheetTitle("Notes · $name")
    LdField(text, { text = it }, "Call ke dauran note likho…", minLines = 3)
    PrimaryButton("Save note", enabled = text.isNotBlank()) {
        Repo.addNote(ctx, number, name, text); text = ""; list = Repo.notesFor(ctx, number)
    }
    if (list.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        LdText("PURANE NOTES", 12f, FontWeight.Bold, p.muted, letter = 1.2f)
        LazyColumn(Modifier.heightIn(max = 260.dp)) {
            items(list, key = { it.id }) { n ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(p, RoundedCornerShape(16.dp), 2.dp).padding(12.dp)) {
                    LdText(df.format(Date(n.time)), 11f, FontWeight.SemiBold, p.muted)
                    LdText(n.text, 14f, maxLines = 10)
                }
            }
        }
    }
}

@Composable
fun AudioRouteSheet(audio: CallAudioState?, onPick: (Int) -> Unit) {
    val p = LocalLd.current
    val mask = audio?.supportedRouteMask ?: (CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
    SheetTitle("Audio kahan sune")
    listOf(
        Triple(CallAudioState.ROUTE_EARPIECE, "Phone (earpiece)", LdIcons.Phone),
        Triple(CallAudioState.ROUTE_SPEAKER, "Speaker", LdIcons.Volume),
        Triple(CallAudioState.ROUTE_BLUETOOTH, "Bluetooth", LdIcons.Bluetooth),
        Triple(CallAudioState.ROUTE_WIRED_HEADSET, "Wired headset", LdIcons.Mic),
    ).filter { mask and it.first != 0 }.forEach { (r, label, ic) ->
        val on = audio?.route == r
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(p, RoundedCornerShape(16.dp), 2.dp).clickable { onPick(r) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LdIcon(ic, 20.dp, if (on) p.accent else p.text)
            LdText(label, 14f, FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (on) LdIcon(LdIcons.Check, 18.dp, p.accent)
        }
    }
}

@Composable
fun SimPickSheet(onPick: (android.telecom.PhoneAccountHandle) -> Unit) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    val sims = remember { Repo.sims(ctx) }
    SheetTitle("Kis SIM se call karein?")
    sims.forEach { s ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(p, RoundedCornerShape(16.dp), 2.dp).clickable { onPick(s.handle) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(p.accent.pct(.16f)), contentAlignment = Alignment.Center) { LdIcon(LdIcons.Sim, 19.dp, p.accent) }
            Column(Modifier.weight(1f)) { LdText("SIM ${s.slot}", 14f, FontWeight.SemiBold); LdText(s.label, 11.5f, color = p.muted) }
        }
    }
    if (sims.isEmpty()) LdText("Koi SIM nahi mili (Phone permission check karo)", 13f, color = p.muted)
}
