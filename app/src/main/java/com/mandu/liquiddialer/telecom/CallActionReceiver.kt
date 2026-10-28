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
        }
        CallNotifier.update(ctx)
    }
    companion object {
        const val DECLINE = "com.mandu.liquiddialer.DECLINE"
        const val HANGUP = "com.mandu.liquiddialer.HANGUP"
        const val MUTE = "com.mandu.liquiddialer.MUTE"
        const val SPEAKER = "com.mandu.liquiddialer.SPEAKER"
    }
}
