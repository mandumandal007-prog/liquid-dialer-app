package com.mandu.liquiddialer.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import com.mandu.liquiddialer.data.Repo
import kotlinx.coroutines.flow.MutableStateFlow

/** Single source of truth for live calls, fed by [LdInCallService]. */
@Suppress("DEPRECATION")
object CallManager {
    var service: LdInCallService? = null
    val calls = MutableStateFlow<List<Call>>(emptyList())
    val audio = MutableStateFlow<CallAudioState?>(null)
    val version = MutableStateFlow(0)
    val recording = MutableStateFlow<RecInfo?>(null)
    private val names = HashMap<String, Pair<String, String?>>()

    data class RecInfo(val file: java.io.File, val started: Long, val source: String)

    fun bump() { version.value = version.value + 1 }

    fun number(call: Call): String = call.details?.handle?.schemeSpecificPart ?: ""

    fun nameFor(ctx: Context, call: Call): String {
        val n = number(call)
        val cached = names[n]
        if (cached != null) return cached.first.ifBlank { n }
        val looked = Repo.lookupName(ctx, n)
        val disp = looked?.first ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: ""
        names[n] = disp to looked?.second
        return disp.ifBlank { if (n.isBlank()) "Private number" else Repo.pretty(n) }
    }
    fun photoFor(call: Call): String? = names[number(call)]?.second
    fun isKnown(ctx: Context, call: Call) = Repo.lookupName(ctx, number(call)) != null

    fun state(call: Call): Int = if (Build.VERSION.SDK_INT >= 31) call.details.state else call.state

    fun primary(): Call? {
        val list = calls.value
        return list.firstOrNull { state(it) == Call.STATE_RINGING }
            ?: list.firstOrNull { state(it) == Call.STATE_ACTIVE && it.parent == null }
            ?: list.firstOrNull { state(it) in listOf(Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_SELECT_PHONE_ACCOUNT, Call.STATE_PULLING_CALL) }
            ?: list.firstOrNull { state(it) == Call.STATE_HOLDING && it.parent == null }
            ?: list.firstOrNull { it.parent == null }
    }
    fun ringing(): Call? = calls.value.firstOrNull { state(it) == Call.STATE_RINGING }
    fun others(): List<Call> {
        val p = primary()
        val children = calls.value.flatMap { conferenceChildren(it) }.toSet()
        return calls.value.filter { it != p && it !in children && it.parent == null && state(it) != Call.STATE_DISCONNECTED }
    }

    fun answer(call: Call? = ringing()) { call?.answer(answerVideoState(call)) }

    /** Camera consent: incoming video call ko sirf audio me lo (user ka saaf choice). */
    fun answerAudioOnly(call: Call? = ringing()) { call?.answer(VideoProfile.STATE_AUDIO_ONLY) }

    /** Incoming video call ko video ke saath hi answer karo (warna ViLTE kho jati hai). */
    fun answerVideoState(call: Call?): Int =
        try { call?.details?.videoState ?: VideoProfile.STATE_AUDIO_ONLY } catch (_: Throwable) { VideoProfile.STATE_AUDIO_ONLY }
    fun reject(call: Call? = ringing(), msg: String? = null) {
        if (call == null) return
        if (msg != null) call.reject(true, msg) else call.reject(false, null)
    }
    fun end(call: Call? = primary()) {
        if (call == null) return
        if (state(call) == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
    }
    fun endAll() { calls.value.forEach { if (state(it) == Call.STATE_RINGING) it.reject(false, null) else it.disconnect() } }

    fun toggleMute() { val s = service ?: return; s.setMuted(!(audio.value?.isMuted ?: false)) }
    fun isSpeaker() = audio.value?.route == CallAudioState.ROUTE_SPEAKER
    fun toggleSpeaker() {
        val s = service ?: return
        val a = audio.value
        if (a?.route == CallAudioState.ROUTE_SPEAKER) {
            val mask = a.supportedRouteMask
            s.setAudioRoute(when {
                mask and CallAudioState.ROUTE_BLUETOOTH != 0 -> CallAudioState.ROUTE_BLUETOOTH
                mask and CallAudioState.ROUTE_WIRED_HEADSET != 0 -> CallAudioState.ROUTE_WIRED_HEADSET
                else -> CallAudioState.ROUTE_EARPIECE
            })
        } else s.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
    }
    fun setRoute(route: Int) { service?.setAudioRoute(route) }
    fun hasBluetooth() = (audio.value?.supportedRouteMask ?: 0) and CallAudioState.ROUTE_BLUETOOTH != 0

    fun toggleHold(call: Call? = primary()) {
        call ?: return
        if (state(call) == Call.STATE_HOLDING) call.unhold() else call.hold()
    }
    fun dtmf(c: Char) { primary()?.let { it.playDtmfTone(c); it.stopDtmfTone() } }

    fun canMerge(): Boolean {
        val p = primary() ?: return false
        return p.conferenceableCalls.isNotEmpty() || (p.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE))
    }
    fun merge() {
        val p = primary() ?: return
        if (p.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) { p.mergeConference(); return }
        p.conferenceableCalls.firstOrNull()?.let { p.conference(it) }
            ?: others().firstOrNull()?.let { p.conference(it) }
    }
    fun swap() {
        val p = primary() ?: return
        if (p.details.can(Call.Details.CAPABILITY_SWAP_CONFERENCE)) { p.swapConference(); return }
        val held = others().firstOrNull { state(it) == Call.STATE_HOLDING } ?: return
        held.unhold()
    }

    // ---- conference participants: per-child controls, only when the call itself reports support ----
    fun conferenceChildren(call: Call?): List<Call> = try { call?.children ?: emptyList() } catch (_: Throwable) { emptyList() }
    fun isConference(call: Call?): Boolean {
        call ?: return false
        return try { call.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true || conferenceChildren(call).isNotEmpty() } catch (_: Throwable) { false }
    }
    fun canDisconnectChild(c: Call): Boolean = try { c.details?.can(Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE) == true } catch (_: Throwable) { false }
    fun canSeparateChild(c: Call): Boolean = try { c.details?.can(Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE) == true } catch (_: Throwable) { false }
    fun disconnectChild(c: Call) { try { c.disconnect() } catch (_: Throwable) {} }
    fun separateChild(c: Call) { try { c.splitFromConference() } catch (_: Throwable) {} }

    // ---- native video calling (ViLTE): only when the live call itself reports support ----
    val videoNotice = MutableStateFlow<String?>(null)

    /** Set when WE placed a direct video call; consumed when the call connects so the screen can
     *  honestly say if the network fell back to voice (videoState stayed AUDIO_ONLY). */
    var pendingVideoRequest = false

    const val VIDEO_UNSUPPORTED = "Ye SIM/phone video call support nahi kar raha"

    /** True only while the call is audio-only AND both sides report bidirectional video (ViLTE) support. */
    fun supportsVideo(call: Call?): Boolean {
        val d = call?.details ?: return false
        if (d.videoState != VideoProfile.STATE_AUDIO_ONLY) return false
        return d.can(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL) &&
            d.can(Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL)
    }

    /** Saamne wale ne video upgrade manga hai (accept/decline user karega - kabhi auto nahi). */
    val videoUpgradeFrom = MutableStateFlow<Call?>(null)

    fun respondVideoUpgrade(call: Call, accept: Boolean) {
        try {
            call.videoCall?.sendSessionModifyResponse(
                VideoProfile(if (accept) VideoProfile.STATE_BIDIRECTIONAL else VideoProfile.STATE_AUDIO_ONLY))
        } catch (_: Throwable) { }
        if (videoUpgradeFrom.value == call) videoUpgradeFrom.value = null
        bump()
    }

    /** Mid-call video upgrade (audio -> ViLTE): asli session-modify request bhejo. Provider na ho
     *  to saaf wajah batao, kuch mat karo. */
    fun requestVideo(call: Call? = primary()): Boolean {
        call ?: return false
        val vc = try { call.videoCall } catch (_: Throwable) { null }
        if (vc == null) { videoNotice.value = "Video is call par is phone/SIM se uplabdh nahi"; return false }
        return try {
            vc.sendSessionModifyRequest(VideoProfile(VideoProfile.STATE_BIDIRECTIONAL)); true
        } catch (_: Throwable) { videoNotice.value = "Video request nahi bhej paayi"; false }
    }

    /** Direct video call ke liye start-with-video-state extra; voice call par kuch nahi. */
    fun buildPlaceExtras(account: PhoneAccountHandle?, video: Boolean): Bundle {
        val extras = Bundle()
        if (account != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
        if (video) extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
        return extras
    }

    @SuppressLint("MissingPermission")
    fun place(ctx: Context, number: String, account: PhoneAccountHandle?, video: Boolean = false): Boolean {
        val uri = Uri.fromParts("tel", number, null)
        val extras = buildPlaceExtras(account, video)
        if (video) pendingVideoRequest = true
        return try {
            ctx.getSystemService(TelecomManager::class.java).placeCall(uri, extras); true
        } catch (e: Exception) { if (video) pendingVideoRequest = false; false }
    }
}
