package com.mandu.liquiddialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mandu.liquiddialer.data.CallReminders

/** Phone restart ke baad AlarmManager ke saare alarms ud jaate hain.
 *  Future reminders dobara schedule karta hai. Jo reminder phone band hone ke
 *  dauraan nikal gaya (24h ke andar), use boot par turant notification de kar
 *  batata hai - warna user ko pata hi nahi chalta. Bahut purana (24h+) saaf. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
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
