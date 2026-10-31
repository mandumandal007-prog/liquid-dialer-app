package com.mandu.liquiddialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mandu.liquiddialer.data.CallReminders
import kotlinx.coroutines.flow.first

/** Phone restart YA app update ke baad AlarmManager ke saare alarms ud jaate hain
 *  (MY_PACKAGE_REPLACED bhi cover kiya - yahan updates aate rehte hain).
 *  Future reminders dobara schedule karta hai. Jo reminder phone band hone ke
 *  dauraan nikal gaya (24h ke andar), use boot par turant notification de kar
 *  batata hai - warna user ko pata hi nahi chalta. Bahut purana (24h+) saaf.
 *  Birthday reminder on ho to uska roz ka alarm bhi wapas arm hota hai. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // Birthday alarm re-arm (cold process - DataStore se sync read, StateFlow default pe nahi)
        val bdayOn = kotlinx.coroutines.runBlocking {
            try { com.mandu.liquiddialer.data.SettingsRepo(ctx).flow.first().birthdayRemind } catch (e: Exception) { false }
        }
        if (bdayOn) com.mandu.liquiddialer.data.BirthdayReminders.schedule(ctx)
        val now = System.currentTimeMillis()
        for (r in CallReminders.allRaw(ctx)) {
            when {
                r.atMs > now -> CallReminders.set(ctx, r.number, r.name, r.atMs)
                r.atMs > now - 24 * 60 * 60_000L -> {
                    CallReminders.remove(ctx, r.number)
                    CallNotifier.callReminder(ctx, r.number, r.name)
                }
                else -> CallReminders.remove(ctx, r.number)
            }
        }
    }
}
