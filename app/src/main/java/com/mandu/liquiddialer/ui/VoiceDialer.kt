package com.mandu.liquiddialer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.Repo

/** Result of matching a spoken command to a contact or raw number. */
data class VoiceMatch(val name: String, val number: String)

/** Pure matcher: spoken text -> contact/number. Unit-tested. */
fun matchVoiceCommand(raw: String, contacts: List<Contact>): VoiceMatch? {
    val text = raw.trim().lowercase()
    if (text.isEmpty()) return null
    // digits route: "call 98765 43210" or "98765 43210 ko call karo"
    val digits = text.filter { it.isDigit() }
    if (digits.length >= 4) {
        val key = digits.takeLast(10)
        val byNum = contacts.firstOrNull { val k = Repo.key(it.number); k.isNotEmpty() && (k == key.takeLast(k.length) || key.endsWith(k) || k.endsWith(key)) }
        return if (byNum != null) VoiceMatch(byNum.name, byNum.number) else VoiceMatch(digits, digits)
    }
    // strip command words (Hinglish + Hindi), then match remaining against contact names
    val q = text
        .replace(Regex("\\b(please|plz|call|dial|phone|ring|karo|karna|kar|do|lagao|laga|milao|mila|mujhe|ko|ki|se|bolo|करो|कॉल|मिलाओ|लगाओ|को)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim()
    if (q.length < 2) return null
    val words = q.split(' ').filter { it.length >= 3 }
    val matches = contacts.filter { c ->
        val n = c.name.lowercase().trim()
        n.isNotBlank() && (n.contains(q) || q.contains(n) || n.split(' ').any { it == q } ||
            (words.isNotEmpty() && words.all { w -> n.contains(w) }))
    }
    // ambiguous speech must never auto-pick a candidate
    if (matches.size != 1) return null
    return VoiceMatch(matches[0].name, matches[0].number)
}

const val VOICE_NOTICE_UNAVAILABLE = "Is phone pe voice service uplabdh nahi hai"
const val VOICE_NOTICE_MIC_DENIED = "Mic permission ke bina voice dialing nahi chalegi"
const val VOICE_NOTICE_BROKEN = "Voice service abhi kaam nahi kar rahi"

/** Maps a SpeechRecognizer error code to a user-facing Hinglish notice. Unit-tested. */
fun voiceErrorNotice(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kuch sunai nahi diya, dobara bolein"
    SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_CLIENT -> "Mic mein dikkat aayi, dobara koshish karein"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VOICE_NOTICE_MIC_DENIED
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network nahi hai, offline pack check karo"
    else -> VOICE_NOTICE_BROKEN
}

/** Observable voice UI state. */
class VoiceUiState {
    var listening by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)     // inline message (errors, no-match)
    var confirm by mutableStateOf<VoiceMatch?>(null) // pending confirmation
}

class VoiceDialer internal constructor(val ui: VoiceUiState, val trigger: () -> Unit, val stop: () -> Unit)

/** Owns SpeechRecognizer lifecycle; trigger() starts (or cancels while listening). */
@Composable
fun rememberVoiceDialer(onHeard: (String) -> Unit): VoiceDialer {
    val ctx = LocalContext.current
    val ui = remember { VoiceUiState() }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun destroy() { try { recognizer?.destroy() } catch (_: Exception) { }; recognizer = null }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            ui.notice = VOICE_NOTICE_UNAVAILABLE
            return
        }
        destroy()
        // Privacy: prefer the guaranteed on-device recognizer (Android 12+/API 31 with on-device pack).
        // On older devices the system's default speech service is used; depending on that service
        // (usually Google's) audio may leave the phone when no offline pack is present.
        val r = try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx))
                SpeechRecognizer.createOnDeviceSpeechRecognizer(ctx)
            else SpeechRecognizer.createSpeechRecognizer(ctx)
        } catch (e: Exception) {
            ui.notice = VOICE_NOTICE_BROKEN; return
        }
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                ui.listening = false
                val said = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (said.isBlank()) ui.notice = "Kuch sunai nahi diya, dobara bolein" else onHeard(said)
                destroy()
            }
            override fun onError(error: Int) {
                ui.listening = false
                ui.notice = voiceErrorNotice(error)
                destroy()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        ui.listening = true
        ui.notice = null
        try { r.startListening(intent) } catch (e: Exception) { ui.listening = false; ui.notice = VOICE_NOTICE_BROKEN; destroy() }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else ui.notice = VOICE_NOTICE_MIC_DENIED
    }

    DisposableEffect(Unit) { onDispose { destroy() } }

    return VoiceDialer(ui,
        trigger = {
            when {
                ui.listening -> { try { recognizer?.stopListening() } catch (_: Exception) { } }
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> startListening()
                else -> permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        stop = { try { recognizer?.cancel() } catch (_: Exception) { }; ui.listening = false; destroy() })
}

/** Look up contact type label (Mobile/Work/Home...) for a number. */
fun lookupContactLabel(ctx: Context, number: String): String? = Repo.contactLabel(ctx, number)

// ---------------- voice UI cards ----------------

@Composable
private fun voiceCardMod(align: Alignment, topPad: androidx.compose.ui.unit.Dp?): Modifier {
    var m: Modifier = Modifier
    if (align != Alignment.Center) m = m.padding(top = topPad ?: 0.dp, start = 24.dp, end = 24.dp)
    return m
}

/** Small inline message with a single dismiss action. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.VoiceNoticeCard(text: String, align: Alignment, topPad: androidx.compose.ui.unit.Dp, onDismiss: () -> Unit) {
    val p = LocalLd.current
    Column(voiceCardMod(align, topPad).align(align).widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp)).background(p.bg2).border(1.dp, p.stroke, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp).testTag("voice_notice"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        LdText(text, 13f, FontWeight.SemiBold, maxLines = 3)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onDismiss() }.padding(horizontal = 12.dp, vertical = 6.dp)) { LdText("Theek hai", 13f, FontWeight.Bold, p.accent) }
    }
}

/** Shown while SpeechRecognizer is listening. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.VoiceListeningCard(align: Alignment, onCancel: () -> Unit) {
    val p = LocalLd.current
    Column(Modifier.align(align).widthIn(max = 300.dp).clip(RoundedCornerShape(22.dp)).background(p.bg2).border(1.dp, p.stroke, RoundedCornerShape(22.dp)).padding(20.dp).testTag("voice_listening"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(p.accent), contentAlignment = Alignment.Center) { LdIcon(LdIcons.Mic, 26.dp, Color.White) }
        Spacer(Modifier.height(12.dp))
        LdText("Sun raha hoon...", 16f, FontWeight.Bold)
        LdText("Naam ya number bolein", 12.5f, color = p.muted, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onCancel() }.padding(horizontal = 12.dp, vertical = 6.dp)) { LdText("Radd karo", 13f, FontWeight.Bold, p.danger) }
    }
}

/** Confirmation before any voice/matched call fires - call only on explicit tap. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.VoiceConfirmCard(m: VoiceMatch, align: Alignment, onCall: () -> Unit, onCancel: () -> Unit) {
    val p = LocalLd.current
    Column(Modifier.align(align).widthIn(max = 310.dp).clip(RoundedCornerShape(22.dp)).background(p.bg2).border(1.dp, p.stroke, RoundedCornerShape(22.dp)).padding(20.dp).testTag("voice_confirm"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        LdText("Call karein?", 13f, FontWeight.SemiBold, p.muted)
        LdText(m.name, 19f, FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        if (m.name != m.number) LdText(Repo.pretty(m.number), 13.5f, color = p.muted, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFD93025)).clickable { onCancel() }.testTag("voice_cancel"), contentAlignment = Alignment.Center) {
                LdIcon(LdIcons.X, 24.dp, Color.White)
            }
            Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF1EA446)).clickable { onCall() }.testTag("voice_call_now"), contentAlignment = Alignment.Center) {
                LdIcon(LdIcons.Phone, 26.dp, Color.White)
            }
        }
    }
}
