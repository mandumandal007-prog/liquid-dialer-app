package com.mandu.liquiddialer.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.mandu.liquiddialer.CallActivity
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.R

object CallNotifier {
    const val CH_INCOMING = "incoming_calls"
    const val CH_ONGOING = "ongoing_calls"
    const val CH_MISSED = "missed_calls"
    const val ID_CALL = 41
    const val ID_MISSED = 42

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            description = "Full-screen incoming call"
        })
        nm.createNotificationChannel(NotificationChannel(CH_ONGOING, "Ongoing call", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null); lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
        nm.createNotificationChannel(NotificationChannel(CH_MISSED, "Missed calls", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun callActivityPi(ctx: Context, action: String? = null, req: Int = 0): PendingIntent {
        val i = Intent(ctx, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (action != null) putExtra("action", action)
        }
        return PendingIntent.getActivity(ctx, 100 + req, i, flags())
    }

    fun receiverPi(ctx: Context, action: String, req: Int, extra: String? = null): PendingIntent {
        val i = Intent(ctx, CallActionReceiver::class.java).setAction(action)
        if (extra != null) i.putExtra("number", extra)
        return PendingIntent.getBroadcast(ctx, 200 + req, i, flags())
    }

    fun update(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        val call = CallManager.primary()
        if (call == null) { nm.cancel(ID_CALL); return }
        val name = CallManager.nameFor(ctx, call)
        val person = Person.Builder().setName(name).setImportant(true).build()
        val st = CallManager.state(call)
        val b = NotificationCompat.Builder(ctx, if (st == Call.STATE_RINGING) CH_INCOMING else CH_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(name)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(st != Call.STATE_RINGING)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(callActivityPi(ctx))
            .setColor(0xFF5B8CFF.toInt())
        if (st == Call.STATE_RINGING) {
            b.setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentText("Incoming call")
                .setFullScreenIntent(callActivityPi(ctx, req = 1), true)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person,
                    receiverPi(ctx, CallActionReceiver.DECLINE, 1),
                    callActivityPi(ctx, "answer", 2)))
        } else {
            val connect = call.details?.connectTimeMillis ?: 0L
            val text = when (st) {
                Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
                Call.STATE_HOLDING -> "On hold"
                Call.STATE_ACTIVE -> "Ongoing call"
                else -> "Call"
            }
            b.setContentText(text)
            if (st == Call.STATE_ACTIVE && connect > 0) b.setWhen(connect).setUsesChronometer(true).setShowWhen(true)
            // CallStyle needs a foreground service or fullScreenIntent on Android 12+, so the ongoing card uses plain actions.
            b.addAction(0, "Call kaato", receiverPi(ctx, CallActionReceiver.HANGUP, 3))
            val muted = CallManager.audio.value?.isMuted == true
            b.addAction(0, if (muted) "Unmute" else "Mute", receiverPi(ctx, CallActionReceiver.MUTE, 4))
            b.addAction(0, if (CallManager.isSpeaker()) "Speaker off" else "Speaker", receiverPi(ctx, CallActionReceiver.SPEAKER, 5))
        }
        try { nm.notify(ID_CALL, b.build()) } catch (e: Exception) { }
    }

    fun missed(ctx: Context, number: String, name: String) {
        val open = PendingIntent.getActivity(ctx, 300, Intent(ctx, MainActivity::class.java).putExtra("tab", "recents")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val callBack = PendingIntent.getActivity(ctx, 301, Intent(ctx, MainActivity::class.java).setAction("com.mandu.liquiddialer.CALLBACK")
            .putExtra("number", number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val sms = PendingIntent.getActivity(ctx, 302, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val n = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Missed call")
            .setContentText(name)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setColor(0xFFFF5D6C.toInt())
            .apply { if (number.isNotBlank()) { addAction(0, "Call back", callBack); addAction(0, "Message", sms) } }
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_MISSED, n) } catch (e: Exception) { }
    }

    fun cancelAll(ctx: Context) { NotificationManagerCompat.from(ctx).cancel(ID_CALL) }
}
