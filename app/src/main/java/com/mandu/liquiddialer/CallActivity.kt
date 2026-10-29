package com.mandu.liquiddialer

import com.mandu.liquiddialer.ui.trNow
import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.telecom.CallRecorder
import com.mandu.liquiddialer.ui.CallScreen
import com.mandu.liquiddialer.ui.LdThemeHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CallActivity : ComponentActivity() {
    private var prox: PowerManager.WakeLock? = null
    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) toggleRecord() else Toast.makeText(this, trNow("Record ke liye Microphone permission chahiye"), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        handleAction(intent)
        setContent {
            LdThemeHost {
                CallScreen(
                    onFinish = { finishAndRemoveTask() },
                    onRecord = { recordClicked() },
                    onAddCall = {
                        startActivity(Intent(this, MainActivity::class.java).putExtra("tab", "dialer").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    onMinimize = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                )
            }
        }
        lifecycleScope.launch {
            combine(CallManager.version, CallManager.audio, LdApp.settings) { _, a, s -> a to s }.collect { (a, s) -> updateProximity(a, s.proximity) }
        }
        lifecycleScope.launch {
            CallManager.calls.collect { list ->
                if (list.isEmpty()) { delay(700); if (CallManager.calls.value.isEmpty()) finishAndRemoveTask() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleAction(intent) }

    private fun handleAction(i: Intent?) {
        if (i?.getStringExtra("action") == "answer") {
            CallManager.answer()
            i.removeExtra("action")
            if (Build.VERSION.SDK_INT >= 26) {
                val km = getSystemService(KeyguardManager::class.java)
                if (km.isKeyguardLocked) km.requestDismissKeyguard(this, null)
            }
        }
    }

    private fun recordClicked() {
        if (CallRecorder.isRecording()) { toggleRecord(); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) micPerm.launch(Manifest.permission.RECORD_AUDIO)
        else toggleRecord()
    }

    private fun toggleRecord() {
        if (CallRecorder.isRecording()) {
            CallRecorder.stop(); Toast.makeText(this, trNow("Recording save ho gayi (Menu > Recordings)"), Toast.LENGTH_SHORT).show(); return
        }
        val c = CallManager.primary() ?: return
        val ok = CallRecorder.start(this, CallManager.number(c), CallManager.nameFor(this, c))
        val src = CallManager.recording.value?.source
        Toast.makeText(this, when {
            !ok -> "Is phone pe recording allow nahi hai"
            src == "VOICE_CALL" -> "Recording shuru (dono awaaz)"
            else -> "Recording shuru — dusre ki awaaz ke liye Speaker on karo"
        }, Toast.LENGTH_LONG).show()
    }

    @Suppress("DEPRECATION")
    private fun updateProximity(a: CallAudioState?, enabled: Boolean) {
        val c = CallManager.primary()
        val st = c?.let { CallManager.state(it) }
        val earpiece = a == null || a.route == CallAudioState.ROUTE_EARPIECE || a.route == CallAudioState.ROUTE_WIRED_HEADSET
        val want = enabled && earpiece && st != null && st != Call.STATE_RINGING && st != Call.STATE_DISCONNECTED && st != Call.STATE_SELECT_PHONE_ACCOUNT
        val pm = getSystemService(PowerManager::class.java)
        if (want) {
            if (prox == null && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                prox = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "liquiddialer:prox")
            }
            prox?.let { if (!it.isHeld) it.acquire(3 * 60 * 60 * 1000L) }
        } else releaseProx()
    }

    private fun releaseProx() { prox?.let { if (it.isHeld) it.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY) } }

    override fun onDestroy() { releaseProx(); super.onDestroy() }
}
