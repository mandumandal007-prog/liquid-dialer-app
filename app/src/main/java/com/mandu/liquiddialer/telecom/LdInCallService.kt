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
        override fun onDetailsChanged(call: Call, details: Call.Details) { CallManager.bump(); CallNotifier.update(this@LdInCallService) }
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

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.service = this
        call.registerCallback(cb)
        CallManager.calls.value = CallManager.calls.value + call
        val s = LdApp.settings.value
        val st = CallManager.state(call)
        if (st == Call.STATE_RINGING) {
            wasRinging += call
            val number = CallManager.number(call)
            val known = CallManager.isKnown(this, call)
            if ((s.blockPrivate && number.isBlank()) || (s.blockUnknown && number.isNotBlank() && !known)) {
                call.reject(false, null); return
            }
            CallNotifier.update(this)
            if (s.incomingFull) launchUi()
            if (s.flipToMute) startFlip()
            if (s.announceCaller) announce(CallManager.nameFor(this, call))
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
            CallNotifier.missed(this, CallManager.number(call), CallManager.nameFor(this, call))
        }
        if (answered.contains(call) && s.vibrateEnd) vibrate(60)
        wasRinging -= call; wasOutgoing -= call; answered -= call
        if (CallManager.calls.value.isEmpty()) {
            CallRecorder.stop()
            CallNotifier.cancelAll(this)
            stopFlip()
        } else CallNotifier.update(this)
        if (CallManager.ringing() == null) stopFlip()
        CallManager.bump()
    }

    private fun onCallChanged(call: Call, state: Int) {
        val s = LdApp.settings.value
        if (state == Call.STATE_ACTIVE && !answered.contains(call)) {
            answered += call
            if (wasOutgoing.contains(call) && s.vibrateAnswer) vibrate(80)
            if (s.autoRecord && !CallRecorder.isRecording()) {
                CallRecorder.start(this, CallManager.number(call), CallManager.nameFor(this, call))
            }
        }
        if (state != Call.STATE_RINGING) { stopFlip(); tts?.stop() }
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

    private fun announce(name: String) {
        tts?.shutdown()
        tts = TextToSpeech(this) { st ->
            if (st == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                main.postDelayed({ if (CallManager.ringing() != null) tts?.speak("$name calling", TextToSpeech.QUEUE_FLUSH, null, "caller") }, 1200)
            }
        }
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
