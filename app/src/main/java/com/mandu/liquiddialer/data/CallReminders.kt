package com.mandu.liquiddialer.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mandu.liquiddialer.telecom.CallActionReceiver
import java.util.Calendar

/** Manual call reminders: user khud chunta hai kis number par kab call karna hai.
 *  Koi auto-call nahi - sirf notification reminder, call user hi karta hai.
 *  Ek number par ek active reminder; naya set karne par purana replace. */
object CallReminders {

    data class Reminder(val number: String, val name: String, val atMs: Long)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("call_reminders", Context.MODE_PRIVATE)

    fun all(ctx: Context): List<Reminder> {
        val now = System.currentTimeMillis()
        val raw = prefs(ctx).all.mapNotNull { (k, v) ->
            val parts = (v as? String)?.split("|", limit = 3) ?: return@mapNotNull null
            val at = parts.getOrNull(2)?.toLongOrNull() ?: return@mapNotNull null
            if (at <= now) { prefs(ctx).edit().remove(k).apply(); return@mapNotNull null } // purane saaf
            Reminder(parts[0], parts[1], at)
        }
        return raw.sortedBy { it.atMs }
    }

    fun forNumber(ctx: Context, number: String): Reminder? =
        all(ctx).firstOrNull { Repo.key(it.number) == Repo.key(number) }

    /** preset -> trigger time. evening: aaj 18:00, nikal gayi to kal 18:00. morning: kal 09:00. */
    fun presetAt(preset: String, nowMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        return when (preset) {
            "30min" -> nowMs + 30 * 60_000L
            "1h" -> nowMs + 60 * 60_000L
            "evening" -> {
                cal.apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                if (cal.timeInMillis <= nowMs) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            else -> { // "morning" = kal subah 9 baje
                cal.apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                cal.timeInMillis
            }
        }
    }

    fun set(ctx: Context, number: String, name: String, atMs: Long) {
        prefs(ctx).edit().putString(Repo.key(number), "$number|$name|$atMs").apply()
        val am = ctx.getSystemService(AlarmManager::class.java)
        try { am.set(AlarmManager.RTC_WAKEUP, atMs, pi(ctx, number, name)) } catch (e: Exception) { }
    }

    fun cancel(ctx: Context, number: String) {
        prefs(ctx).edit().remove(Repo.key(number)).apply()
        val am = ctx.getSystemService(AlarmManager::class.java)
        try { am.cancel(pi(ctx, number, "")) } catch (e: Exception) { }
    }

    fun remove(ctx: Context, number: String) { prefs(ctx).edit().remove(Repo.key(number)).apply() }

    private fun pi(ctx: Context, number: String, name: String): PendingIntent {
        val i = Intent(ctx, CallActionReceiver::class.java).setAction(CallActionReceiver.CALL_REMIND)
            .putExtra("number", number).putExtra("name", name)
        return PendingIntent.getBroadcast(ctx, 700 + (Repo.key(number).hashCode() and 0xFF), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
