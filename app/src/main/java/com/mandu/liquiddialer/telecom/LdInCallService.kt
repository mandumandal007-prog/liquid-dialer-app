package com.mandu.liquiddialer.telecom

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import com.mandu.liquiddialer.CallActivity
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.Repo
import java.util.Locale

@Suppress("DEPRECATION")
class LdInCallService : InCallService() {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var sensorOn = false
    private val wasOutgoing = HashSet<Call>()
    private val wasRinging = HashSet<Call>()
    private val answered = HashSet<Call>()

    private val cb = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) { onCallChanged(call, state) }
        override fun onDetailsChanged(call: Call, details: Call.Details) { watchVideo(call); CallManager.bump(); CallNotifier.update(this@LdInCallService) }
        override fun onConferenceableCallsChanged(call: Call, c: MutableList<Call>) { CallManager.bump() }
        override fun onChildrenChanged(call: Call, children: MutableList<Call>) { CallManager.bump() }
        override fun onParentChanged(call: Call, parent: Call?) { CallManager.bump() }
    }

    override fun onCreate() { super.onCreate(); CallManager.service = this }
    override fun onDestroy() {
        stopFlip(); tts?.shutdown(); tts = null
        if (CallManager.service === this) CallManager.service = null
        super.onDestroy()
    }

    private val videoWatched = HashSet<Call>()
    private fun watchVideo(call: Call) {
        if (!videoWatched.add(call)) return
        try {
            call.videoCall?.registerCallback(object : InCallService.VideoCall.Callback() {
                override fun onSessionModifyRequestReceived(profile: VideoProfile?) { }
                override fun onSessionModifyResponseReceived(status: Int, requested: VideoProfile?, response: VideoProfile?) {
                    // SessionModifyResponse.STATUS_SUCCESS == 1 (the constants class is not public SDK)
                    if (status != 1) {
                        CallManager.videoNotice.value = "Video call is SIM/phone par uplabdh nahi"
                        CallManager.bump()
                    }
                }
                override fun onCallSessionEvent(event: Int) { }
                override fun onPeerDimensionsChanged(width: Int, height: Int) { }
                override fun onVideoQualityChanged(quality: Int) { }
                override fun onCallDataUsageChanged(dataUsage: Long) { }
                override fun onCameraCapabilitiesChanged(caps: VideoProfile.CameraCapabilities?) { }
            })
        } catch (_: Throwable) { }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.service = this
        com.mandu.liquiddialer.media.BoostService.pauseForCall(this)
        call.registerCallback(cb)
        watchVideo(call)
        CallManager.calls.value = CallManager.calls.value + call
        val s = LdApp.settings.value
        val st = CallManager.state(call)
        if (st == Call.STATE_RINGING) {
            wasRinging += call
            val number = CallManager.number(call)
            val canReadContacts = checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val known = !canReadContacts || CallManager.isKnown(this, call)
            if ((s.blockPrivate && number.isBlank()) || (s.blockUnknown && number.isNotBlank() && !known)) {
                call.reject(false, null); return
            }
            CallNotifier.update(this)
            if (s.incomingFull) launchUi()
            if (s.flipToMute) startFlip()
            if (s.announceCaller && (!s.announceOnlyFav || Repo.contactStarred(this, number))) announce(CallManager.nameFor(this, call), s.announceRepeat.coerceIn(1, 3))
            if (s.flashOnRing && (!s.flashOnlyFav || Repo.contactStarred(this, number))) startFlash()
            if (s.ringVolumeMax) maxStream(android.media.AudioManager.STREAM_RING)
        } else {
            if (st == Call.STATE_DIALING || st == Call.STATE_CONNECTING || st == Call.STATE_SELECT_PHONE_ACCOUNT) wasOutgoing += call
            CallNotifier.update(this)
            launchUi()
        }
        CallManager.bump()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(cb)
        CallManager.calls.value = CallManager.calls.value - call
        val s = LdApp.settings.value
        val cause = call.details?.disconnectCause?.code
        if (wasRinging.contains(call) && !answered.contains(call) && cause == DisconnectCause.MISSED && s.missedAlerts) {
            CallNotifier.missed(this, CallManager.number(call), CallManager.nameFor(this, call), s.missedRemind)
        }
        // outgoing call that never connected (busy/error): suggest redial
        if (wasOutgoing.contains(call) && !answered.contains(call) && s.redialSuggest &&
            (cause == DisconnectCause.BUSY || cause == DisconnectCause.ERROR)) {
            CallNotifier.redial(this, CallManager.number(call), CallManager.nameFor(this, call))
        }
        // finished call with an unsaved number: offer save/block
        if (answered.contains(call) && s.postCallSave) {
            val n = CallManager.number(call)
            val canRead = checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (n.isNotBlank() && canRead && !CallManager.isKnown(this, call)) CallNotifier.savePrompt(this, n)
        }
        if (answered.contains(call) && s.vibrateEnd) vibrate(60)
        wasRinging -= call; wasOutgoing -= call; answered -= call; videoWatched -= call
        if (CallManager.calls.value.isEmpty()) {
            CallRecorder.stop()
            CallNotifier.cancelAll(this)
            stopFlip(); stopFlash(); stopMinuteBeep()
        } else CallNotifier.update(this)
        if (CallManager.ringing() == null) { stopFlip(); stopFlash() }
        if (CallManager.calls.value.none { CallManager.state(it) == Call.STATE_ACTIVE }) stopMinuteBeep()
        CallManager.bump()
    }

    private fun onCallChanged(call: Call, state: Int) {
        val s = LdApp.settings.value
        if (state == Call.STATE_ACTIVE && !answered.contains(call)) {
            answered += call
            if (wasOutgoing.contains(call) && s.vibrateAnswer) vibrate(80)
            if (wasOutgoing.contains(call) && s.autoSpeaker && !CallManager.hasBluetooth()) CallManager.setRoute(CallAudioState.ROUTE_SPEAKER)
            val unknownOk = !s.recordUnknownOnly || !CallManager.isKnown(this, call)
            if (s.autoRecord && unknownOk && !CallRecorder.isRecording()) {
                CallRecorder.start(this, CallManager.number(call), CallManager.nameFor(this, call))
            }
            if (s.callVolumeMax) maxStream(android.media.AudioManager.STREAM_VOICE_CALL)
            startMinuteBeep(s.minuteBeep)
        }
        if (state != Call.STATE_RINGING) { stopFlip(); stopFlash(); tts?.stop() }
        CallNotifier.update(this)
        CallManager.bump()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallManager.audio.value = audioState
        CallNotifier.update(this)
        CallManager.bump()
    }

    private fun launchUi() {
        try {
            startActivity(Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        } catch (_: Exception) { }
    }

    private fun vibrate(ms: Long) {
        try {
            val v = getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(ms)
        } catch (_: Exception) { }
    }

    private fun announce(name: String, repeat: Int = 1) {
        tts?.shutdown()
        tts = TextToSpeech(this) { st ->
            if (st == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                for (i in 0 until repeat) {
                    main.postDelayed({ if (CallManager.ringing() != null) tts?.speak("$name calling", TextToSpeech.QUEUE_FLUSH, null, "caller$i") }, 1200L + i * 5000L)
                }
            }
        }
    }

    // ---- flash on ring (opt-in) ----
    private var flashOn = false
    private val flashTick = object : Runnable {
        override fun run() {
            if (!flashOn) return
            torch(toggle = true)
            main.postDelayed(this, 350)
        }
    }
    private var torchLit = false
    private fun torch(toggle: Boolean) {
        try {
            val cm = getSystemService(android.hardware.camera2.CameraManager::class.java)
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return
            torchLit = !torchLit
            cm.setTorchMode(id, torchLit)
        } catch (_: Exception) { }
    }
    private fun startFlash() { if (flashOn) return; flashOn = true; torchLit = false; main.post(flashTick) }
    private fun stopFlash() {
        if (!flashOn) return
        flashOn = false; main.removeCallbacks(flashTick)
        if (torchLit) { torch(toggle = true); torchLit = false }
    }

    // ---- minute beep (opt-in) ----
    private var beepMin = 0
    private var toneGen: android.media.ToneGenerator? = null
    private val beepTick = object : Runnable {
        override fun run() {
            if (beepMin <= 0) return
            try { toneGen?.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 180) } catch (_: Exception) { }
            main.postDelayed(this, beepMin * 60_000L)
        }
    }
    private fun startMinuteBeep(min: Int) {
        if (min <= 0 || beepMin == min) return
        stopMinuteBeep()
        beepMin = min
        try { toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, 70) } catch (_: Exception) { }
        main.postDelayed(beepTick, beepMin * 60_000L)
    }
    private fun stopMinuteBeep() {
        beepMin = 0; main.removeCallbacks(beepTick)
        try { toneGen?.release() } catch (_: Exception) { }; toneGen = null
    }

    private fun maxStream(stream: Int) {
        try {
            val am = getSystemService(android.media.AudioManager::class.java)
            am.setStreamVolume(stream, am.getStreamMaxVolume(stream), 0)
        } catch (_: Exception) { }
    }

    // ---- flip to mute ----
    private var faceDownCount = 0
    private val flip = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (e.values[2] < -8.5f) faceDownCount++ else faceDownCount = 0
            if (faceDownCount >= 4) {
                try { getSystemService(TelecomManager::class.java).silenceRinger() } catch (_: Exception) { }
                tts?.stop(); stopFlip()
            }
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    }
    private fun startFlip() {
        if (sensorOn) return
        val sm = getSystemService(SensorManager::class.java) ?: return
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        faceDownCount = 0
        sensorOn = sm.registerListener(flip, acc, SensorManager.SENSOR_DELAY_NORMAL)
    }
    private fun stopFlip() {
        if (!sensorOn) return
        getSystemService(SensorManager::class.java)?.unregisterListener(flip); sensorOn = false
    }
}
