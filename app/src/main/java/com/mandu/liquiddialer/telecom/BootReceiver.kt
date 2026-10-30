package com.mandu.liquiddialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mandu.liquiddialer.data.CallReminders

/** Phone restart ke baad AlarmManager ke saare alarms ud jaate hain.
 *  Stored future call reminders ko dobara schedule karta hai. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        for (r in CallReminders.all(ctx)) CallReminders.set(ctx, r.number, r.name, r.atMs)
    }
}
