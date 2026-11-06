package com.mandu.liquiddialer

import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.mandu.liquiddialer.data.FakeCalls
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Opened deliberately from preview or notification tap. Never a real Telecom call. */
class FakeCallActivity : ComponentActivity() {
    private var ring: Ringtone?=null
    private var token=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token=intent.getStringExtra("token") ?: ""
        val p=FakeCalls.usable(this,token) ?: run { finish();return }
        // Consume once on open so an old notification cannot replay after a process restart.
        FakeCalls.cancel(this)
        enableEdgeToEdge()
        // Use the normal activity window. No lock-screen bypass or full-screen notification intent.
        getSystemService(android.app.NotificationManager::class.java).cancel(FakeCalls.NOTICE)
        setContent { LdThemeHost { FakeCallView(p.name,onAnswer={stopRing()},onEnd={closeSimulation()}) } }
        val audio=getSystemService(AudioManager::class.java)
        if(audio.ringerMode==AudioManager.RINGER_MODE_NORMAL && savedInstanceState==null) {
            ring=runCatching { RingtoneManager.getRingtone(this,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) }.getOrNull()
            runCatching { ring?.play() }
        }
        lifecycleScope.launch { delay(45_000);closeSimulation() }
        lifecycleScope.launch { CallManager.calls.collect { if(it.isNotEmpty())closeSimulation() } }
    }
    private fun stopRing(){runCatching {ring?.stop()};ring=null}
    private fun closeSimulation(){stopRing();if(FakeCalls.current(this)?.token==token)FakeCalls.cancel(this);finish()}
    override fun onStop(){super.onStop();closeSimulation()}
    override fun onDestroy(){stopRing();super.onDestroy()}
}
