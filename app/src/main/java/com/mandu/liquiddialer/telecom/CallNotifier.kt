package com.mandu.liquiddialer.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
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
    const val CH_SUMMARY = "call_summary"
    const val CH_BIRTHDAY = "birthdays"
    const val ID_CALL = 41
    const val ID_MISSED = 42
    const val ID_SUMMARY = 43
    const val ID_BIRTHDAY = 45

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
        nm.createNotificationChannel(NotificationChannel(CH_SUMMARY, "Call summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        })
        nm.createNotificationChannel(NotificationChannel(CH_BIRTHDAY, "Birthdays", NotificationManager.IMPORTANCE_DEFAULT).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        })
    }

    /** Call khatam hone ka chhota summary (opt-in). Lock screen par naam/number nahi dikhta:
     *  visibility PRIVATE + generic public version. */
    fun summary(ctx: Context, name: String, durText: String) {
        val open = PendingIntent.getActivity(ctx, 401, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val b = NotificationCompat.Builder(ctx, CH_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(name)
            .setContentText("Call khatam · $durText")
            .setSubText("Liquid Dialer")
            .setColor(0xFF5B8CFF.toInt())
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(ctx, CH_SUMMARY)
                .setSmallIcon(R.drawable.ic_stat_call).setContentTitle("Call khatam").build())
            .setContentIntent(open)
        try { NotificationManagerCompat.from(ctx).notify(ID_SUMMARY, b.build()) } catch (e: Exception) { }
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
            // Icon bar: standard action buttons render as text chips only, and with
            // DecoratedCustomViewStyle any addAction would ALSO draw as duplicate text below,
            // so the bar is our own RemoteViews row (mute/speaker/red hangup icons) and no
            // addAction calls here. Accessible names via contentDescription; same receiver
            // PendingIntents as before, so taps behave exactly like the old text buttons.
            val muted = CallManager.audio.value?.isMuted == true
            val spk = CallManager.isSpeaker()
            val showTimer = st == Call.STATE_ACTIVE && connect > 0
            fun bar(): RemoteViews {
                val rv = RemoteViews(ctx.packageName, R.layout.notif_call_ongoing)
                rv.setTextViewText(R.id.notif_call_name, name)
                rv.setTextViewText(R.id.notif_call_status, text)
                rv.setViewVisibility(R.id.notif_call_status, if (showTimer) android.view.View.GONE else android.view.View.VISIBLE)
                rv.setViewVisibility(R.id.notif_call_timer, if (showTimer) android.view.View.VISIBLE else android.view.View.GONE)
                if (showTimer) {
                    // Details.connectTimeMillis is wall-clock; Chronometer needs elapsedRealtime.
                    val base = android.os.SystemClock.elapsedRealtime() - (System.currentTimeMillis() - connect)
                    rv.setLong(R.id.notif_call_timer, "setBase", if (base > 0) base else android.os.SystemClock.elapsedRealtime())
                }
                rv.setInt(R.id.notif_btn_mute, "setBackgroundResource", if (muted) R.drawable.notif_btn_circle_active else R.drawable.notif_btn_circle)
                rv.setContentDescription(R.id.notif_btn_mute, if (muted) "Unmute" else "Mute")
                rv.setOnClickPendingIntent(R.id.notif_btn_mute, receiverPi(ctx, CallActionReceiver.MUTE, 4))
                rv.setInt(R.id.notif_btn_speaker, "setBackgroundResource", if (spk) R.drawable.notif_btn_circle_active else R.drawable.notif_btn_circle)
                rv.setContentDescription(R.id.notif_btn_speaker, if (spk) "Speaker off" else "Speaker")
                rv.setOnClickPendingIntent(R.id.notif_btn_speaker, receiverPi(ctx, CallActionReceiver.SPEAKER, 5))
                rv.setContentDescription(R.id.notif_btn_end, "Call kaato")
                rv.setOnClickPendingIntent(R.id.notif_btn_end, receiverPi(ctx, CallActionReceiver.HANGUP, 3))
                return rv
            }
            b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            b.setCustomContentView(bar())
            b.setCustomBigContentView(bar())
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

    /** Manual call reminder due: user ne khud set kiya tha.
     *  Body tap = dialer khulta hai number bhara hua (koi call nahi).
     *  Sirf "Call karo" action seedha call lagata hai. Koi auto-call nahi. */
    fun callReminder(ctx: Context, number: String, name: String) {
        if (number.isBlank()) return
        val display = if (name.isNotBlank()) name else number
        val req = 700 + (com.mandu.liquiddialer.data.Repo.key(number).hashCode() and 0xFF)
        val open = PendingIntent.getActivity(ctx, req,
            Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_DIAL)
                .setData(android.net.Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val callNow = PendingIntent.getActivity(ctx, req + 256,
            Intent(ctx, MainActivity::class.java).setAction("com.mandu.liquiddialer.CALLBACK")
                .putExtra("number", number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val n = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Call reminder: $display")
            .setContentText("Call karna tha. 'Call karo' dabate hi call lag jayega.")
            .setAutoCancel(true)
            .setColor(0xFF3DDC84.toInt())
            .setContentIntent(open)
            .addAction(0, "Call karo", callNow)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(req, n) } catch (e: Exception) { }
    }

    /** Opt-in birthday reminder: aaj jin contacts ka birthday hai unka ek notification.
     *  Body tap = app khulti hai. "Call karo" sirf pehle birthday number par - user ka
     *  tap hi call lagata hai, koi auto-call nahi. */
    fun birthday(ctx: Context, items: List<com.mandu.liquiddialer.data.Repo.Birthday>) {
        if (items.isEmpty()) return
        val first = items.first()
        val names = items.take(3).joinToString(", ") { it.name } + if (items.size > 3) " +${items.size - 3}" else ""
        val open = PendingIntent.getActivity(ctx, 762,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
        val b = NotificationCompat.Builder(ctx, CH_BIRTHDAY)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(if (items.size == 1) "Aaj ${first.name} ka birthday hai" else "Aaj ${items.size} contacts ka birthday hai")
            .setContentText("$names - call karke badhai do")
            .setAutoCancel(true)
            .setColor(0xFF3DDC84.toInt())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(ctx, CH_BIRTHDAY)
                .setSmallIcon(R.drawable.ic_stat_call).setContentTitle("Birthday reminder")
                .setContentText("App kholke dekho").build())
            .setContentIntent(open)
        if (first.number.isNotBlank()) {
            val callNow = PendingIntent.getActivity(ctx, 763,
                Intent(ctx, MainActivity::class.java).setAction("com.mandu.liquiddialer.CALLBACK")
                    .putExtra("number", first.number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags())
            b.addAction(0, "Call karo", callNow)
        }
        try { NotificationManagerCompat.from(ctx).notify(ID_BIRTHDAY, b.build()) } catch (e: Exception) { }
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
