package com.mandu.liquiddialer.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.R
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.launch

/**
 * Media loudness boost: a LoudnessEnhancer attached to the global output mix (audio session 0)
 * with only the normal MODIFY_AUDIO_SETTINGS permission. Note: session-0 global effects are a
 * deprecated framework path; support varies by device/HAL and is unverified until run on the
 * user's phone - any refusal is reported honestly and the setting is switched off.
 *
 * Honest scope: this boosts MEDIA playback (music, videos, other apps' sound) only. This app
 * never routes it at SIM/voice-call audio: whenever a call starts (incoming or outgoing),
 * LdInCallService calls pauseForCall and the effect is released on the spot. It then stays
 * OFF until the user explicitly resumes it (Volume page "Boost dobara chalu karo"); there is
 * no auto-resume after the call, and applyNow refuses to start while any call is live.
 * The effect dies with the process, so this small foreground service holds it while the user
 * keeps the boost on. It never starts or re-enables itself: only an explicit user
 * toggle/slider/"dobara chalu karo" starts it.
 */
class BoostService : Service() {

    private var enhancer: LoudnessEnhancer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            try { LdApp.instance.scope.launch { LdApp.instance.repo.set(K.volBoost, false) } } catch (_: Throwable) {}
            stopSelf(); return START_NOT_STICKY
        }
        val pct = intent?.getIntExtra(EXTRA_PCT, -1)?.takeIf { it >= 0 } ?: LdApp.settings.value.volBoostGain
        applyBoost(pct)
        return START_STICKY
    }

    private fun applyBoost(pct: Int) {
        try {
            running.value = false
            val e = enhancer ?: LoudnessEnhancer(0).also { enhancer = it }
            e.setTargetGain(VolumeBoost.gainToMb(pct))
            e.enabled = true
            running.value = true
            foreground(pct)
        } catch (t: Throwable) {
            // device/HAL refused the global effect: say so, switch the setting off, stop
            try { enhancer?.release() } catch (_: Throwable) {}
            enhancer = null
            try { LdApp.instance.scope.launch { LdApp.instance.repo.set(K.volBoost, false) } } catch (_: Throwable) {}
            notifyFailed()
            stopSelf()
        }
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Media boost", NotificationManager.IMPORTANCE_MIN))
        }
    }

    private fun foreground(pct: Int) {
        ensureChannel()
        val open = PendingIntent.getActivity(this, 1, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this, 2, Intent(this, BoostService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Media volume boost chal raha hai")
            .setContentText("$pct% · ≈${VolumeBoost.dbLabel(pct)} · sirf media ke liye")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(0, "Boost band karo", stopPi)
            .build()
        ServiceCompat.startForeground(this, NOTIF_ID, n,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
    }

    private fun notifyFailed() {
        ensureChannel()
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Media boost is phone par support nahi hua")
            .setContentText("Phone ne boost mana kar diya; setting band kar di gayi hai.")
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID + 1, n)
    }

    override fun onDestroy() {
        running.value = false
        try { enhancer?.release() } catch (_: Throwable) {}
        enhancer = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_APPLY = "com.mandu.liquiddialer.boost.APPLY"
        const val ACTION_STOP = "com.mandu.liquiddialer.boost.STOP"
        const val EXTRA_PCT = "pct"
        private const val CHANNEL = "media_boost"
        private const val NOTIF_ID = 77

        /** True sirf jab effect live attached ho (process death ke baad false - koi auto-restart nahi). */
        val running = kotlinx.coroutines.flow.MutableStateFlow(false)

        fun applyNow(ctx: Context, pct: Int) {
            // call chal rahi ho to boost shuru hi mat karo (session-0 effect call audio tak
            // kuch devices par pahunch sakta hai - risk lene ke bajaye saaf mana)
            if (com.mandu.liquiddialer.telecom.CallManager.calls.value.isNotEmpty()) return
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, BoostService::class.java)
                    .setAction(ACTION_APPLY).putExtra(EXTRA_PCT, pct))
            } catch (_: Throwable) {}
        }

        /**
         * SIM call start hote hi boost rok do. Setting (volBoost) jaan-boojh kar ON rehti hai
         * taaki Volume page par "Boost dobara chalu karo" dikhe aur user khud hi wapas chalu
         * kare - call ke baad kabhi auto-resume NAHI hota.
         */
        fun pauseForCall(ctx: Context) {
            if (!running.value) return
            running.value = false
            stop(ctx)
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, BoostService::class.java)) } catch (_: Throwable) {}
        }
    }
}
