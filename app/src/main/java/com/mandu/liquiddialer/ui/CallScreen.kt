package com.mandu.liquiddialer.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Build
import android.telecom.Call
import android.view.SurfaceView
import android.telecom.VideoProfile
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
    // wide red pill (OPPO-style hang-up); still one big tap target
    Box(Modifier.graphicsLayer { scaleX = s; scaleY = s }.width(size * 1.5f).height(size * .9f)
        .shadow(10.dp, RoundedCornerShape(999.dp), ambientColor = Color(0x66D93025), spotColor = Color(0x66D93025)).clip(RoundedCornerShape(999.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFFEF4A3D), Color(0xFFD93025))))
        .testTag("end_call")
        .clickable(src, null, onClick = onClick), contentAlignment = Alignment.Center) {
        LdIcon(LdIcons.Phone, size * .42f, Color.White, Modifier.rotate(135f))
    }
}

@Composable
fun androidx.compose.foundation.layout.BoxScope.EndConfirmCard(align: Alignment, onOk: () -> Unit, onCancel: () -> Unit) {
    val p = LocalLd.current
    Column(Modifier.align(align).widthIn(max = 310.dp).clip(RoundedCornerShape(22.dp)).background(p.bg2).border(1.dp, p.stroke, RoundedCornerShape(22.dp)).padding(20.dp).testTag("end_confirm")) {
        LdText("Call kaatein?", 15f, FontWeight.Bold)
        LdText("Call abhi chal rahi hai.", 12.5f, color = p.muted, maxLines = 3, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(p.glass).clickable { onCancel() }.padding(vertical = 12.dp).testTag("end_confirm_cancel"), contentAlignment = Alignment.Center) { LdText("Wapas", 13f, FontWeight.SemiBold) }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(Color(0xFFD93025)).clickable { onOk() }.padding(vertical = 12.dp).testTag("end_confirm_ok"), contentAlignment = Alignment.Center) { LdText("Haan, cut karo", 13f, FontWeight.SemiBold, Color.White) }
        }
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
    var confirmEnd by remember { mutableStateOf(false) }
    // keep the last call's identity so the end screen still shows who it was and how long it ran
    var lastEnded by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    call?.let { c ->
        val dur = c.details?.connectTimeMillis ?: 0L
        lastEnded = Triple(CallManager.nameFor(ctx, c), CallManager.number(c), if (dur > 0) fmtDur((System.currentTimeMillis() - dur) / 1000) else "")
    }

    Box(Modifier.fillMaxSize()) {
        CallBackground(Modifier.haze(haze), s.callBgStyle, call?.let { CallManager.photoFor(it) })
        CompositionLocalProvider(LocalHaze provides haze) {
            if (call == null) {
                CallEndedScreen(lastEnded)
            } else {
                val st = CallManager.state(call)
                if (st == Call.STATE_RINGING) IncomingContent(call, s.swipeAnswer, onMessage = { sheet = "reply" })
                else OngoingContent(call, audio, rec, onRecord, onAddCall, onMinimize, openSheet = { sheet = it },
                    onEndRequest = { if (s.confirmBeforeEnd) confirmEnd = true else CallManager.end(call) })
                if (st == Call.STATE_SELECT_PHONE_ACCOUNT && sheet == null) sheet = "sim"
            }
            val c = call
            val vNotice by CallManager.videoNotice.collectAsState()
            vNotice?.let { n -> VoiceNoticeCard(n, Alignment.TopCenter, 64.dp) { CallManager.videoNotice.value = null } }
            if (confirmEnd && c != null) EndConfirmCard(align = Alignment.BottomCenter, onOk = { confirmEnd = false; CallManager.end(c) }, onCancel = { confirmEnd = false })
            if (sheet != null && c != null) {
                Sheet(onDismiss = { sheet = null }) {
                    when (sheet) {
                        "reply" -> QuickReplySheet(s.quickReplies) { msg -> CallManager.reject(c, msg); sheet = null }
                        "notes" -> NotesSheet(CallManager.number(c), CallManager.nameFor(ctx, c)) { sheet = null }
                        "audio" -> AudioRouteSheet(audio) { CallManager.setRoute(it); sheet = null }
                        "sim" -> SimPickSheet { h -> c.phoneAccountSelected(h, false); sheet = null }
                        "conference" -> ConferenceSheet(c) { sheet = null }
                        "video" -> VideoSheet(c) { sheet = null }
                    }
                }
            }
        }
    }
}

/** Video sheet: native ViLTE upgrade (support ho to, camera permission ke saath) + imaandari wajah +
 *  Google Meet ka alag internet raasta (kuch auto nahi - meeting user khud banata hai). */
@Composable
private fun VideoSheet(call: Call, onClose: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val supported = CallManager.supportsVideo(call)
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { CallManager.requestVideo(call); onClose() }
        else CallManager.videoNotice.value = "Camera permission nahi mili - video preview nahi dikhega"
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LdText("Video call", 16f, FontWeight.Bold)
        if (supported) {
            LdText("Ye call SIM video (ViLTE) support bata rahi hai.", 12.5f, color = p.muted, maxLines = 2)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.accent).clickable {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    CallManager.requestVideo(call); onClose()
                } else camLauncher.launch(Manifest.permission.CAMERA)
            }.padding(vertical = 12.dp).testTag("video_upgrade_go"), horizontalArrangement = Arrangement.Center) { LdText("SIM video chalu karo", 13f, FontWeight.Bold, Color.White) }
        } else {
            LdText(CallManager.videoUnsupportedReason(call), 12.5f, color = p.muted, modifier = Modifier.testTag("video_reason"), maxLines = 3)
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).glass(p, RoundedCornerShape(16.dp)).clickable {
            val num = try { call.details?.handle?.schemeSpecificPart } catch (_: Throwable) { null }
            if (num == null || !CallManager.meetCallNumber(ctx, num)) CallManager.openMeet(ctx)
            onClose()
        }.padding(12.dp).testTag("meet_launch"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LdIcon(LdIcons.Video, 18.dp, p.accent)
            Column(Modifier.weight(1f)) {
                LdText("Meet se is number ko call", 13f, FontWeight.SemiBold)
                LdText("Experimental: yeh button isi number ke liye Meet calling request bhejta hai. Meet version ke hisaab se call screen khul sakti hai ya call shuru ho sakti hai. Support na mile to Meet home/Play Store khulega", 11f, color = p.muted, maxLines = 4)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LdText("Band karo", 12.5f, FontWeight.SemiBold, p.muted, Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onClose).padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun TopChips(call: Call, showMin: Boolean, onMinimize: () -> Unit, onVideo: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    val sims = remember { Repo.sims(ctx) }
    val simLabel = sims.firstOrNull { it.handle == call.details?.accountHandle }?.let { if (sims.size > 1) "SIM ${it.slot} · ${it.label}" else it.label } ?: "SIM"
    Box(Modifier.fillMaxWidth()) {
        if (showMin) Box(Modifier.align(Alignment.CenterStart)) { IconBtn(LdIcons.ChevronDown, onClick = onMinimize) }
        Box(Modifier.align(Alignment.Center)) { SimChip(simLabel) }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // video entry audio call par hamesha dikhti hai (gayab nahi): tap par video sheet -
            // support ho to ViLTE upgrade, nahi to imaandari wajah + Meet raasta. chalti video call me chhupti hai.
            if (onVideo != null) {
                val vsChip = try { call.details?.videoState } catch (_: Throwable) { null } ?: VideoProfile.STATE_AUDIO_ONLY
                if (vsChip == VideoProfile.STATE_AUDIO_ONLY) {
                    val sup = CallManager.supportsVideo(call)
                    Box(Modifier.size(34.dp).glass(p, CircleShape, 6.dp).clip(CircleShape).testTag("video_call")
                        .clickable(onClick = onVideo), contentAlignment = Alignment.Center) {
                        LdIcon(LdIcons.Video, 18.dp, if (sup) p.accent else p.muted)
                    }
                }
            }
            SimChip("HD", p.ok, live = true)
        }
    }
}

@Composable
private fun Hero(call: Call, status: String, statusColor: Color, timer: String?, badges: List<Pair<String, Color>>, ringing: Boolean, manage: (() -> Unit)? = null) {
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
        if (ringing && s.showContactType && number.isNotBlank()) {
            remember(number) { Repo.contactLabel(ctx, number) }?.let { LdText(it, 12f, FontWeight.SemiBold, p.accent) }
        }
        if (ringing && s.showLastCall && number.isNotBlank()) {
            remember(number) { Repo.lastCallFor(ctx, number) }?.let { (date, typ, dur) ->
                val when0 = android.text.format.DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS, android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString()
                val kind = when (typ) { android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"; android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"; else -> "Missed" }
                LdText("Pichli call: $kind · $when0", 11.5f, color = p.muted)
            }
        }
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
            // conference manage: kisi ek participant ko cut/private karo (chip sirf tab jab call sach me conference ho)
            if (manage != null) {
                Row(Modifier.clip(RoundedCornerShape(999.dp)).glass(p, RoundedCornerShape(999.dp), 0.dp).clickable(onClick = manage).padding(horizontal = 10.dp, vertical = 5.dp).testTag("manage_conf"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    LdIcon(LdIcons.Users, 11.dp, p.accent)
                    LdText("Manage", 10f, FontWeight.Bold, p.accent, letter = .6f)
                }
            }
        }
    }
}

@Composable
private fun OngoingContent(call: Call, audio: CallAudioState?, rec: CallManager.RecInfo?, onRecord: () -> Unit, onAddCall: () -> Unit,
                           onMinimize: () -> Unit, openSheet: (String) -> Unit, onEndRequest: (Call) -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    val now = nowTicker()
    val st = CallManager.state(call)
    val vs = (try { call.details?.videoState } catch (_: Throwable) { null }) ?: VideoProfile.STATE_AUDIO_ONLY
    // black stage ka matlab "frames aa rahe hain" hota - isliye sirf ACTIVE + saamne se video RX par hi dikhao.
    // dialing/pending par normal audio UI + imaandari status; warna kala screen broken lagti hai.
    val videoRx = vs == VideoProfile.STATE_BIDIRECTIONAL || vs == VideoProfile.STATE_RX_ENABLED
    val videoStage = videoRx && st == Call.STATE_ACTIVE
    val upgradeReq by CallManager.videoUpgradeFrom.collectAsState()
    var keypad by remember { mutableStateOf(false) }
    var dtmf by remember { mutableStateOf("") }
    val connect = call.details?.connectTimeMillis ?: 0L
    // direct video call request ki sachai: connect par bhi audio-only hai to saaf batao (koi chhupa fallback nahi)
    LaunchedEffect(st) {
        if (st == Call.STATE_ACTIVE && CallManager.pendingVideoRequest) {
            CallManager.pendingVideoRequest = false
            if (call.details?.videoState == VideoProfile.STATE_AUDIO_ONLY) {
                CallManager.videoNotice.value = "Seedhi video call is SIM/phone par support nahi hui - voice call chal rahi hai"
            }
        }
    }
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
        if (vs != VideoProfile.STATE_AUDIO_ONLY && !videoStage) add("VIDEO" to p.accent)
        if (rec != null) add("REC ${fmtDur((now - rec.started) / 1000)}" to p.danger)
        if (muted) add("MUTED" to p.muted)
        if (st == Call.STATE_HOLDING) add("ON HOLD" to p.muted)
        if (speaker) add("SPEAKER" to p.muted)
        if (bt) add("BLUETOOTH" to p.muted)
    }
    val others = CallManager.others()
    val s by LdApp.settings.collectAsState()

    val gridL = remember(s.layoutGrid) { parseLayout(s.layoutGrid, GRID_IDS) }
    val bottomL = remember(s.layoutBottom) { parseLayout(s.layoutBottom, BOTTOM_IDS) }
    // audio-route button shows where the sound goes now, so it is not a second Speaker toggle
    val routeIcon = if (bt) LdIcons.Bluetooth else if (speaker) LdIcons.Volume else LdIcons.Phone
    val routeLabel = if (bt) "Bluetooth" else if (speaker) "Speaker" else "Earpiece"
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
                !audioVisible -> BtnSpec(routeIcon, routeLabel, bt) { openSheet("audio") }
                else -> BtnSpec(LdIcons.Merge, "Merge", enabled = false) {}
            }
            "audio" -> BtnSpec(routeIcon, routeLabel, bt) { openSheet("audio") }
            "speaker" -> BtnSpec(LdIcons.SpeakerFill, if (speaker) "Speaker on" else "Speaker", speaker, flat = true) {
                if (CallManager.hasBluetooth() && !speaker) openSheet("audio") else CallManager.toggleSpeaker()
            }
            "notes" -> BtnSpec(LdIcons.Edit, "Notes") { openSheet("notes") }
            else -> null
        }
    }

    // upgrade card 60 sec me apne aap hat jaye: provider request timeout ho to prompt na atke
    LaunchedEffect(upgradeReq) {
        val uc = upgradeReq ?: return@LaunchedEffect
        delay(60_000)
        if (CallManager.videoUpgradeFrom.value === uc) { CallManager.videoUpgradeFrom.value = null; CallManager.bump() }
    }

    if (videoStage && !keypad) {
        // ===== full-screen video calling card: saamne wala video poori screen, saare controls overlay =====
        // video ke upar sab text pada ja sake: is card par palette ke text/muted white
        CompositionLocalProvider(LocalLd provides p.copy(text = Color.White, muted = Color.White.copy(alpha = .72f))) {
        // video controls ~4 sec me chhup jate hain, stage par tap se wapas aate hain.
        // prompt/doosri call par kabhi nahi chhupte (consent/info pehle); end call ek tap door.
        var videoUi by remember { mutableStateOf(true) }
        LaunchedEffect(st, upgradeReq, others.isNotEmpty()) {
            if (st != Call.STATE_ACTIVE || upgradeReq != null || others.isNotEmpty()) videoUi = true
        }
        LaunchedEffect(videoUi, st, upgradeReq, others.isNotEmpty()) {
            if (!videoUi || st != Call.STATE_ACTIVE || upgradeReq != null || others.isNotEmpty()) return@LaunchedEffect
            delay(4000)
            videoUi = false
        }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().testTag("video_tap")
                .clickable(remember { MutableInteractionSource() }, indication = null) { videoUi = !videoUi }) {
                VideoStage(call, fullBleed = true)
            }
            if (videoUi) {
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent)))
                .statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 26.dp)) {
                TopChips(call, true, onMinimize, onVideo = { openSheet("video") })
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveDot(if (st == Call.STATE_ACTIVE) p.ok else p.accent)
                    LdText(CallManager.nameFor(ctx, call), 16f, FontWeight.Bold, Color.White, Modifier.weight(1f), maxLines = 1)
                    LdText(if (timer != null) "$status · $timer" else status, 12.5f, FontWeight.SemiBold, Color.White.copy(alpha = .85f))
                }
                // saamne wale ka video upgrade request: user hi decide kare (kabhi auto-accept nahi)
                if (upgradeReq == call) {
                    Row(Modifier.padding(top = 10.dp).fillMaxWidth().glass(p, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LdText("Video on karna chahta hai", 12.5f, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Row(Modifier.clip(RoundedCornerShape(999.dp)).background(p.ok).clickable { CallManager.respondVideoUpgrade(call, true) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("video_accept")) { LdText("Chalu karo", 11.5f, FontWeight.Bold, Color.White) }
                        Row(Modifier.clip(RoundedCornerShape(999.dp)).glass(p, RoundedCornerShape(999.dp), 0.dp).clickable { CallManager.respondVideoUpgrade(call, false) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("video_decline")) { LdText("Nahi", 11.5f, FontWeight.Bold, p.muted) }
                    }
                }
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
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .62f))))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 10.dp)) {
                CallControlsLayout(gridL, bottomL, s.gridScale, s.bottomScale, spec, onEnd = { onEndRequest(call) })
            }
            }
        }
        }
    } else {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) {
        TopChips(call, true, onMinimize, onVideo = { openSheet("video") })
        // saamne wale ka video upgrade request: user hi decide kare (kabhi auto-accept nahi)
        if (upgradeReq == call) {
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().glass(p, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LdText("Video on karna chahta hai", 12.5f, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(p.ok).clickable { CallManager.respondVideoUpgrade(call, true) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("video_accept")) { LdText("Chalu karo", 11.5f, FontWeight.Bold, Color.White) }
                Row(Modifier.clip(RoundedCornerShape(999.dp)).glass(p, RoundedCornerShape(999.dp), 0.dp).clickable { CallManager.respondVideoUpgrade(call, false) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("video_decline")) { LdText("Nahi", 11.5f, FontWeight.Bold, p.muted) }
            }
        }
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
            if (!keypad) Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Hero(call, status, p.accent, timer, badges, false, manage = if (CallManager.isConference(call)) ({ openSheet("conference") }) else null) }
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LdText(CallManager.nameFor(ctx, call) + (timer?.let { "  ·  $it" } ?: ""), 13f, FontWeight.SemiBold, p.muted)
                LdText(dtmf.ifEmpty { " " }, 30f, FontWeight.SemiBold, letter = 2f)
                Keypad(onKey = { k -> dtmf += k; CallManager.dtmf(k[0]) }, keyDp = (LocalConfiguration.current.screenHeightDp * .095f).coerceIn(58f, 84f).dp, letters = s.keypadLetters)
                Row(Modifier.glass(p, RoundedCornerShape(999.dp), 4.dp).clickable { keypad = false }.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LdIcon(LdIcons.X, 16.dp); LdText("Keypad band karo", 13f, FontWeight.SemiBold)
                }
            }
        }
        if (!keypad) {
            Box(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                CallControlsLayout(gridL, bottomL, s.gridScale, s.bottomScale, spec, onEnd = { onEndRequest(call) })
            }
        } else {
            // keypad open: only the bottom row stays
            CallControlsLayout(emptyList(), bottomL, s.gridScale, s.bottomScale, spec, onEnd = { CallManager.end(call) }, compact = true)
        }
    }
    }
}
@Composable
private fun IncomingContent(call: Call, swipe: Boolean, onMessage: () -> Unit) {
    val p = LocalLd.current
    val incomingVideo = ((try { call.details?.videoState } catch (_: Throwable) { null }) ?: VideoProfile.STATE_AUDIO_ONLY) != VideoProfile.STATE_AUDIO_ONLY
    val ctx = LocalContext.current
    val sims = remember { Repo.sims(ctx) }
    val sim = sims.firstOrNull { it.handle == call.details?.accountHandle }?.let { "SIM ${it.slot}" } ?: "SIM"
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 26.dp)) {
        TopChips(call, false, onMinimize = {})
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Hero(call, (if (incomingVideo) "Incoming video call · " else "Incoming call · ") + sim, p.accent, null, emptyList(), true)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            SwipeCallBtn(danger = true, label = "Cut", swipe = swipe) { CallManager.reject(call) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(52.dp).glass(p, CircleShape, 6.dp).clickable { onMessage() }, contentAlignment = Alignment.Center) { LdIcon(LdIcons.Message, 22.dp) }
                LdText("Message", 11f, FontWeight.SemiBold, p.muted)
                Spacer(Modifier.height(18.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // incoming video answer: camera permission ke bina video answer nahi - deny par sirf audio + imaandari notice
            val camAnswer = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) CallManager.answer(call)
                else { CallManager.answerAudioOnly(call); CallManager.videoNotice.value = "Camera permission nahi mili - sirf audio me liya" }
            }
            SwipeCallBtn(danger = false, label = if (incomingVideo) "Video answer" else "Answer", swipe = swipe) {
                if (incomingVideo && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) camAnswer.launch(Manifest.permission.CAMERA)
                else CallManager.answer(call)
            }
                // camera consent: video call ko sirf audio me lene ka saaf option (camera share nahi hoti)
                if (incomingVideo) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).glass(p, RoundedCornerShape(999.dp), 0.dp).clickable { CallManager.answerAudioOnly(call) }.padding(horizontal = 12.dp, vertical = 6.dp).testTag("answer_audio")) {
                        LdText("Sirf audio me lo", 10.5f, FontWeight.SemiBold, p.muted)
                    }
                }
            }
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

/** Royal / luxury "call ended" finish: gold gradient headline, ornamental rules, soft halo. */
@Composable
private fun CallEndedScreen(last: Triple<String, String, String>?) {
    val p = LocalLd.current
    val gold = Brush.linearGradient(listOf(Color(0xFFF9E7B2), Color(0xFFD4AF37), Color(0xFF8C6D1F)))
    Box(Modifier.fillMaxSize().testTag("call_ended"), contentAlignment = Alignment.Center) {
        // soft golden halo behind the headline
        Box(Modifier.size(300.dp).background(Brush.radialGradient(listOf(Color(0x33D4AF37), Color.Transparent)), CircleShape))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(54.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD4AF37)))))
                Box(Modifier.padding(horizontal = 10.dp).size(7.dp).rotate(45f).background(Color(0xFFD4AF37)))
                Box(Modifier.width(54.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color.Transparent))))
            }
            Text("CALL SAMAPT",
                Modifier.padding(top = 18.dp).testTag("call_ended_title"),
                style = androidx.compose.ui.text.TextStyle(brush = gold),
                fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            LdText("Call ended", 12.5f, color = p.muted, letter = 3f, modifier = Modifier.padding(top = 8.dp))
            if (last != null && last.first.isNotBlank()) {
                LdText(last.first, 16f, FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp), maxLines = 1)
                val sub = listOf(Repo.pretty(last.second), last.third).filter { it.isNotBlank() }.joinToString("  ·  ")
                if (sub.isNotBlank()) LdText(sub, 12.5f, color = p.muted, modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(54.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD4AF37)))))
                Box(Modifier.padding(horizontal = 10.dp).size(7.dp).rotate(45f).background(Color(0xFFD4AF37)))
                Box(Modifier.width(54.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color.Transparent))))
            }
        }
    }
}


/** Conference participants: kisi EK ko cut ya private karo - poori conference yahan se kabhi nahi kat-ti. */
@Composable
private fun ConferenceSheet(call: Call, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalLd.current
    val ver by CallManager.version.collectAsState()
    val children = remember(ver) { CallManager.conferenceChildren(call) }
    val anyUnsupported = remember(ver) { children.any { !CallManager.canDisconnectChild(it) && !CallManager.canSeparateChild(it) } }
    SheetTitle("Conference manage karo")
    if (children.isEmpty()) {
        LdText("Is call me abhi koi alag participant nahi mila. Ye tabhi dikhta hai jab phone/SIM conference support kare.", 12.5f, color = p.muted, maxLines = 4)
        return
    }
    LdText("Kisi ek ko cut ya private karo - baaki conference chalti rahegi.", 12f, color = p.muted, maxLines = 3, modifier = Modifier.padding(bottom = 10.dp))
    children.forEachIndexed { i, child ->
        val canCut = remember(ver) { CallManager.canDisconnectChild(child) }
        val canSplit = remember(ver) { CallManager.canSeparateChild(child) }
        val cname = CallManager.nameFor(ctx, child)
        val cnum = CallManager.number(child)
        val cstate = when (CallManager.state(child)) {
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_ACTIVE -> "Connected"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
            else -> ""
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(cname, CallManager.photoFor(child), 38.dp, 14f, seed = cname)
                Column(Modifier.weight(1f)) {
                    LdText(cname, 14f, FontWeight.SemiBold, maxLines = 1)
                    val sub = (if (cnum.isNotBlank() && cname != Repo.pretty(cnum)) Repo.pretty(cnum) + if (cstate.isNotBlank()) " · $cstate" else "" else cstate)
                    if (sub.isNotBlank()) LdText(sub, 11.5f, color = p.muted, maxLines = 1)
                }
            }
            Row(Modifier.padding(top = 8.dp, start = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(if (canCut) Color(0xFFD93025) else p.glass).alpha(if (canCut) 1f else .45f)
                    .clickable(enabled = canCut) { CallManager.disconnectChild(child) }
                    .padding(horizontal = 14.dp, vertical = 8.dp).testTag("conf_cut_$i"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LdIcon(LdIcons.PhoneOff, 13.dp, if (canCut) Color.White else p.muted)
                    LdText("Cut karo", 12f, FontWeight.SemiBold, if (canCut) Color.White else p.muted)
                }
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(p.glass).alpha(if (canSplit) 1f else .45f)
                    .clickable(enabled = canSplit) { CallManager.separateChild(child); onClose() }
                    .padding(horizontal = 14.dp, vertical = 8.dp).testTag("conf_priv_$i"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LdIcon(LdIcons.PhoneOut, 13.dp, if (canSplit) p.accent else p.muted)
                    LdText("Private", 12f, FontWeight.SemiBold, if (canSplit) p.accent else p.muted)
                }
            }
        }
    }
    if (anyUnsupported) LdText("Gray option is call par SIM/phone support nahi kar raha.", 11.5f, color = p.muted, maxLines = 3, modifier = Modifier.padding(top = 10.dp))
}

/** Asli ViLTE video: saamne wale ka video poora area (display surface) + apna chhota preview.
 *  Surfaces VideoLink ke through provider ko diye jate hain; capture/modem ka kaam IMS stack karta
 *  hai. Surface destroy hote hi provider ko null push - koi leaked/stale surface nahi. SurfaceView
 *  ka surface system-owned hai, isliye release nahi karte. */
@Composable
private fun VideoStage(call: Call, fullBleed: Boolean = false) {
    Box(if (fullBleed) Modifier.fillMaxSize().background(Color.Black)
        else Modifier.fillMaxSize().padding(top = 14.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
        AndroidView(factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : android.view.SurfaceHolder.Callback {
                    override fun surfaceCreated(h: android.view.SurfaceHolder) { com.mandu.liquiddialer.telecom.VideoLink.setDisplay(h.surface) }
                    override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, ht: Int) { }
                    override fun surfaceDestroyed(h: android.view.SurfaceHolder) { com.mandu.liquiddialer.telecom.VideoLink.setDisplay(null) }
                })
            }
        }, modifier = Modifier.fillMaxSize().testTag("video_display"))
        AndroidView(factory = { ctx ->
            SurfaceView(ctx).apply {
                // media overlay: preview doosre (display) SurfaceView ke UPAR dikhe, peeche na chhupe
                setZOrderMediaOverlay(true)
                holder.addCallback(object : android.view.SurfaceHolder.Callback {
                    override fun surfaceCreated(h: android.view.SurfaceHolder) { com.mandu.liquiddialer.telecom.VideoLink.setPreview(h.surface) }
                    override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, ht: Int) { }
                    override fun surfaceDestroyed(h: android.view.SurfaceHolder) { com.mandu.liquiddialer.telecom.VideoLink.setPreview(null) }
                })
            }
        }, modifier = Modifier.align(Alignment.TopEnd)
            .padding(top = if (fullBleed) 120.dp else 10.dp, end = if (fullBleed) 14.dp else 10.dp)
            .size(if (fullBleed) 96.dp else 88.dp, if (fullBleed) 132.dp else 120.dp)
            .clip(RoundedCornerShape(12.dp)).background(Color.DarkGray).testTag("video_preview"))
    }
}
