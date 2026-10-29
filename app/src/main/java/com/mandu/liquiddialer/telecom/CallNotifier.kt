package com.mandu.liquiddialer.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.app.AlarmManager
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

    /** Round accent avatar with the caller's initial, used as the notification large icon. */
    fun avatarBitmap(name: String, accent: Int): android.graphics.Bitmap {
        val size = 128
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.shader = android.graphics.LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), accent, 0xFF8E6BFF.toInt(), android.graphics.Shader.TileMode.CLAMP)
        c.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.shader = null
        paint.color = android.graphics.Color.WHITE
        paint.textSize = size * 0.46f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.isFakeBoldText = true
        val ch = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"
        c.drawText(ch, size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        return bmp
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
            .setSubText("Liquid Dialer")
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
            b.setLargeIcon(avatarBitmap(name, 0xFF5B8CFF.toInt()))
            if (st == Call.STATE_ACTIVE && connect > 0) b.setWhen(connect).setUsesChronometer(true).setShowWhen(true)
            // CallStyle needs a foreground service or fullScreenIntent on Android 12+, so the ongoing card uses plain actions.
            b.addAction(0, "Call kaato", receiverPi(ctx, CallActionReceiver.HANGUP, 3))
            val muted = CallManager.audio.value?.isMuted == true
            b.addAction(0, if (muted) "Unmute" else "Mute", receiverPi(ctx, CallActionReceiver.MUTE, 4))
            b.addAction(0, if (CallManager.isSpeaker()) "Speaker off" else "Speaker", receiverPi(ctx, CallActionReceiver.SPEAKER, 5))
        }
        try { nm.notify(ID_CALL, b.build()) } catch (e: Exception) { }
    }

    fun missed(ctx: Context, number: String, name: String, remind: Boolean = false) {
        val open = receiverPi(ctx, CallActionReceiver.MISSED_OPEN, 0, number)
        val callBack = PendingIntent.getActivity(ctx, 301, Intent(ctx, MainActivity::class.java).setAction("com.mandu.liquiddialer.CALLBACK")
            .putExtra("number", number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val sms = PendingIntent.getActivity(ctx, 302, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val big = NotificationCompat.BigTextStyle().bigText("$name\n$time" + if (number.isNotBlank()) "\n$number" else "")
        val n = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Missed call · $time")
            .setContentText(name)
            .setSubText("Liquid Dialer")
            .setStyle(big)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setDeleteIntent(receiverPi(ctx, CallActionReceiver.MISSED_DISMISS, 1, number))
            .setColor(0xFFFF5D6C.toInt())
            .apply { if (number.isNotBlank()) { addAction(0, "Call back", callBack); addAction(0, "Message", sms); addAction(0, "Block", receiverPi(ctx, CallActionReceiver.BLOCK_NUM, 2, number)) } }
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_MISSED, n) } catch (e: Exception) { }
        if (remind && number.isNotBlank()) scheduleReminders(ctx, number, name)
    }

    /** Opt-in missed-call reminders: re-notify after 5/10/15 min unless opened/dismissed. */
    fun scheduleReminders(ctx: Context, number: String, name: String) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        lastMissed = number to name
        for (i in 1..3) {
            val pi = receiverPi(ctx, CallActionReceiver.MISSED_REMIND, 10 + i, number)
            try { am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + i * 5 * 60_000L, pi) } catch (e: Exception) { }
        }
    }

    fun cancelReminders(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (i in 1..3) try { am.cancel(receiverPi(ctx, CallActionReceiver.MISSED_REMIND, 10 + i, "")) } catch (e: Exception) { }
        lastMissed = null
    }

    @Volatile var lastMissed: Pair<String, String>? = null

    /** Busy/failed outgoing call: one-tap redial. */
    fun redial(ctx: Context, number: String, name: String) {
        if (number.isBlank()) return
        val callBack = PendingIntent.getActivity(ctx, 311, Intent(ctx, MainActivity::class.java).setAction("com.mandu.liquiddialer.CALLBACK")
            .putExtra("number", number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val n = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Call nahi lag paayi")
            .setContentText(name)
            .setAutoCancel(true)
            .setColor(0xFFFF5D6C.toInt())
            .addAction(0, "Dobara call", callBack)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(43, n) } catch (e: Exception) { }
    }

    /** After a call with an unsaved number: offer save / block. */
    fun savePrompt(ctx: Context, number: String) {
        val save = PendingIntent.getActivity(ctx, 321, Intent(Intent.ACTION_INSERT).setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE)
            .putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val n = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Naya number: $number")
            .setContentText("Contacts me save karein?")
            .setAutoCancel(true)
            .setColor(0xFF5B8CFF.toInt())
            .addAction(0, "Save karo", save)
            .addAction(0, "Block", receiverPi(ctx, CallActionReceiver.BLOCK_NUM, 22, number))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(44, n) } catch (e: Exception) { }
    }

    fun cancelAll(ctx: Context) { NotificationManagerCompat.from(ctx).cancel(ID_CALL) }
}
