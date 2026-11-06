package com.mandu.liquiddialer.data

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mandu.liquiddialer.FakeCallActivity
import com.mandu.liquiddialer.telecom.CallManager
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One local simulation. No Telecom call, contact lookup, call log, mic or recording. */
object FakeCalls {
    const val CHANNEL = "fake_call_simulation"
    const val NOTICE = 8204
    data class Plan(val token: String, val name: String, val at: Long, val ready: Boolean)
    private fun file(ctx: Context) = AtomicFile(File(ctx.noBackupFilesDir, "fake_call.json"))
    @Synchronized fun current(ctx: Context): Plan? = try {
        val o = JSONObject(file(ctx).openRead().bufferedReader().use { it.readText() })
        Plan(o.getString("token"), o.getString("name"), o.getLong("at"), o.getBoolean("ready"))
    } catch (_: Exception) { null }
    private fun write(ctx: Context, p: Plan) {
        val f = file(ctx); val out = f.startWrite()
        try {
            out.write(JSONObject().put("token",p.token).put("name",p.name).put("at",p.at).put("ready",p.ready).toString().toByteArray())
            f.finishWrite(out)
        } catch (e: Exception) { f.failWrite(out); throw e }
    }
    private fun alarm(ctx: Context, p: Plan): PendingIntent = PendingIntent.getBroadcast(ctx, NOTICE,
        Intent(ctx, FakeCallReceiver::class.java).setAction("fake_call_due")
            .setData(Uri.parse("liquiddialer://fake-call/${p.token}" )).putExtra("token",p.token),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun realCallActive(ctx: Context): Boolean = CallManager.calls.value.isNotEmpty() ||
        runCatching { ctx.getSystemService(android.telecom.TelecomManager::class.java).isInCall }.getOrDefault(false)
    fun notificationsAllowed(ctx: Context): Boolean {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Fake call simulation", NotificationManager.IMPORTANCE_HIGH))
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 || nm.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
    }
    /** Delays are approximate and expire after 5 minutes of lateness. No reboot recovery. */
    @Synchronized fun schedule(ctx: Context, name: String, delayMs: Long, now: Long = System.currentTimeMillis()): Plan {
        require(delayMs in listOf(30_000L,60_000L,300_000L))
        check(notificationsAllowed(ctx)) { "Notifications band hain - pehle Settings mein allow karo" }
        check(!realCallActive(ctx)) { "Asli call chal rahi hai" }
        cancel(ctx)
        val p = Plan(UUID.randomUUID().toString(),name.trim().take(40).ifBlank { "Demo caller" },now+delayMs,false)
        write(ctx,p)
        try { ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,p.at,alarm(ctx,p)) }
        catch (e: Exception) { cancel(ctx); throw e }
        return p
    }
    @Synchronized fun preview(ctx: Context): Plan {
        check(!realCallActive(ctx)) { "Asli call chal rahi hai" }
        // A preview never replaces a scheduled alert.
        check(current(ctx) == null) { "Pehle pending simulation cancel karo" }
        return Plan(UUID.randomUUID().toString(),"Demo caller",System.currentTimeMillis(),true).also { write(ctx,it) }
    }
    @Synchronized fun cancel(ctx: Context) {
        val plan = current(ctx)
        // Optional simulation cleanup must never stop real-call setup. Try durable retirement first,
        // then independently attempt both system cleanup steps.
        cleanupIndependently(
            retire = { file(ctx).delete() },
            cancelAlarm = { plan?.let { ctx.getSystemService(AlarmManager::class.java).cancel(alarm(ctx,it)) } },
            cancelNotification = { ctx.getSystemService(NotificationManager::class.java).cancel(NOTICE) }
        )
    }
    internal fun cleanupIndependently(retire: () -> Unit, cancelAlarm: () -> Unit, cancelNotification: () -> Unit) {
        for (step in listOf(retire, cancelAlarm, cancelNotification)) {
            try { step() } catch (_: Exception) { /* Real calls take precedence; no private data in logs. */ }
        }
    }
    @Synchronized fun usable(ctx: Context, token: String, now: Long = System.currentTimeMillis()): Plan? {
        val p=current(ctx) ?: return null
        if(p.token != token) return null
        if(now < p.at || now-p.at > 300_000L || realCallActive(ctx)) { cancel(ctx); return null }
        return p.takeIf { it.ready }
    }
    @Synchronized fun deliver(ctx: Context, token: String, now: Long = System.currentTimeMillis()): Boolean {
        val p=current(ctx) ?: return false
        if(p.token != token || p.ready || now < p.at) return false
        if(now-p.at > 300_000L || realCallActive(ctx) || !notificationsAllowed(ctx)) { cancel(ctx);return false }
        write(ctx,p.copy(ready=true))
        val open=PendingIntent.getActivity(ctx,NOTICE, Intent(ctx,FakeCallActivity::class.java)
            .setData(Uri.parse("liquiddialer://fake-call/${p.token}" )).putExtra("token",p.token),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getBroadcast(ctx,NOTICE+1,Intent(ctx,FakeCallReceiver::class.java).setAction("fake_call_cancel")
            .setData(Uri.parse("liquiddialer://fake-call/${p.token}" )).putExtra("token",p.token),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(ctx,CHANNEL).setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Fake call: ${p.name}").setContentText("Simulation - tap karke kholo. Asli call nahi.")
            .setContentIntent(open).setDeleteIntent(stop).addAction(0,"Cancel",stop)
            .setAutoCancel(true).setTimeoutAfter(300_000).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        return try { ctx.getSystemService(NotificationManager::class.java).notify(NOTICE,n);true }
        catch (_: SecurityException) { cancel(ctx);false }
    }
    @Synchronized fun cleanExpired(ctx: Context, now: Long = System.currentTimeMillis()) {
        current(ctx)?.let { if(now-it.at>300_000L)cancel(ctx) }
    }
}

class FakeCallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val token=intent.getStringExtra("token") ?: return
        if(intent.action=="fake_call_cancel") {
            if(FakeCalls.current(ctx)?.token==token)FakeCalls.cancel(ctx)
        } else if(intent.action=="fake_call_due") FakeCalls.deliver(ctx,token)
    }
}
