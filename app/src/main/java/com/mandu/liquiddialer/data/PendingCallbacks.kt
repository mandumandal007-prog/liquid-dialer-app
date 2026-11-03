package com.mandu.liquiddialer.data

import android.provider.CallLog

/** Derived from the loaded window only. Never schedules or places a call. */
object PendingCallbacks {
    data class Item(val latest: Recent, val missed: Int)
    fun from(recents: List<Recent>): List<Item> {
        val closed = mutableSetOf<String>()
        val pending = linkedMapOf<String, Item>()
        for (r in recents.sortedWith(compareByDescending<Recent> { it.date }.thenByDescending { it.id })) {
            val key = ContactControls.k(r.number)
            if (key.isEmpty() || key in closed) continue
            if ((r.type == CallLog.Calls.INCOMING_TYPE || r.type == CallLog.Calls.OUTGOING_TYPE) && r.duration > 0) {
                closed += key
            } else if (r.type == CallLog.Calls.MISSED_TYPE) {
                val old = pending[key]
                pending[key] = if (old == null) Item(r,1) else old.copy(missed=old.missed+1)
            }
        }
        return pending.values.toList()
    }
}
