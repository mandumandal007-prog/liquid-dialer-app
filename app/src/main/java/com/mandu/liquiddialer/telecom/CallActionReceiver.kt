package com.mandu.liquiddialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            DECLINE -> CallManager.reject()
            HANGUP -> CallManager.end()
            MUTE -> CallManager.toggleMute()
            SPEAKER -> CallManager.toggleSpeaker()
            BLOCK_NUM -> {
                intent.getStringExtra("number")?.let { com.mandu.liquiddialer.data.Repo.block(ctx, it) }
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(CallNotifier.ID_MISSED)
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(44)
            }
            MISSED_OPEN -> {
                CallNotifier.cancelReminders(ctx)
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(CallNotifier.ID_MISSED)
                val i = Intent(ctx, com.mandu.liquiddialer.MainActivity::class.java).putExtra("tab", "recents").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(i) } catch (e: Exception) { }
            }
            MISSED_DISMISS -> CallNotifier.cancelReminders(ctx)
            MISSED_REMIND -> {
                val lm = CallNotifier.lastMissed
                if (lm != null) CallNotifier.missed(ctx, lm.first, lm.second, remind = false)
            }
        }
        CallNotifier.update(ctx)
    }
    companion object {
        const val DECLINE = "com.mandu.liquiddialer.DECLINE"
        const val HANGUP = "com.mandu.liquiddialer.HANGUP"
        const val MUTE = "com.mandu.liquiddialer.MUTE"
        const val SPEAKER = "com.mandu.liquiddialer.SPEAKER"
        const val BLOCK_NUM = "com.mandu.liquiddialer.BLOCK_NUM"
        const val MISSED_OPEN = "com.mandu.liquiddialer.MISSED_OPEN"
        const val MISSED_DISMISS = "com.mandu.liquiddialer.MISSED_DISMISS"
        const val MISSED_REMIND = "com.mandu.liquiddialer.MISSED_REMIND"
    }
}
