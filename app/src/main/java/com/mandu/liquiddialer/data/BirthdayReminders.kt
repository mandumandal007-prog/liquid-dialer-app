package com.mandu.liquiddialer.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mandu.liquiddialer.telecom.CallActionReceiver
import com.mandu.liquiddialer.telecom.CallNotifier
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Birthday reminder (opt-in, default OFF): jis din kisi contact ka birthday ho,
 *  subah 9 baje ek notification. Roz ka ek inexact alarm (exact-alarm permission
 *  nahi maangte); fire ke baad agle din ka alarm wahin dobara set hota hai.
 *  Koi auto-call ya auto-message nahi - notification ka "Call karo" action user
 *  ke tap par hi call lagata hai. */
object BirthdayReminders {

    const val HOUR = 9

    /** Agla fire time: aaj 09:00 agar wo abhi aane wala hai, warna kal 09:00.
     *  DST-safe: kal ki date local calendar se, fixed +24h math se nahi. */
    fun nextFireMs(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val today9 = date.atTime(LocalTime.of(HOUR, 0)).atZone(zone).toInstant().toEpochMilli()
        if (today9 > nowMs) return today9
        return date.plusDays(1).atTime(LocalTime.of(HOUR, 0)).atZone(zone).toInstant().toEpochMilli()
    }

    private fun pi(ctx: Context): PendingIntent {
        val i = Intent(ctx, CallActionReceiver::class.java).setAction(CallActionReceiver.BIRTHDAY_CHECK)
        return PendingIntent.getBroadcast(ctx, 760, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, nowMs: Long = System.currentTimeMillis()) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        try { am.set(AlarmManager.RTC_WAKEUP, nextFireMs(nowMs), pi(ctx)) } catch (e: Exception) { }
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        try { am.cancel(pi(ctx)) } catch (e: Exception) { }
    }

    /** Alarm fire ya boot/update ke baad re-arm. enabled = settings ka live value
     *  (caller synchronous read karke deta hai - receiver cold process me StateFlow
     *  default pe bharosa nahi kar sakta). off = sirf cancel, kuch schedule nahi.
     *  on + aaj birthday = notification; dono haalat me agla din ka alarm set. */
    fun onAlarm(
        ctx: Context,
        enabled: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        load: (Context) -> List<Repo.Birthday> = { Repo.birthdays(it, 0) },
        notify: (Context, List<Repo.Birthday>) -> Unit = { c, l -> CallNotifier.birthday(c, l) },
    ) {
        if (!enabled) { cancel(ctx); return }
        val todays = try { load(ctx) } catch (e: Exception) { emptyList() }
        if (todays.isNotEmpty()) try { notify(ctx, todays) } catch (e: Exception) { }
        schedule(ctx, nowMs)
    }
}
