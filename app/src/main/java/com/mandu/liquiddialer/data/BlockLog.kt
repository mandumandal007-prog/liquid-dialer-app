package com.mandu.liquiddialer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** App ki screening se roki gayi calls ka bounded local log (private / unknown / series).
 * System BlockedNumberContract se hui manual blocks ka event app ko kabhi nahi milta,
 * isliye wo is list me nahi aati (wo Recents me dikhti hain). Sirf is phone pe rehta hai:
 * kisi sync, backup ya server me nahi jata. Purani entries khud hat ti hain (MAX ke upar). */
object BlockLog {
    private const val PREF = "block_log"
    private const val KEY = "events"
    const val MAX = 200
    const val REASON_PRIVATE = "private"
    const val REASON_UNKNOWN = "unknown"
    const val REASON_SERIES = "series"

    data class Entry(val display: String, val idn: String, val reason: String, val ts: Long)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun validReason(r: String) = r == REASON_PRIVATE || r == REASON_UNKNOWN || r == REASON_SERIES

    /** Screening se block hone pe call karo. number blank ho sakta hai (private/hidden). */
    @Synchronized fun append(ctx: Context, number: String, reason: String, ts: Long = System.currentTimeMillis()) {
        if (!validReason(reason)) return
        val display = number.trim()
        val idn = ContactControls.k(display)
        // Ajeeb characters wala number kabhi store mat karo; private (blank) allowed hai
        if (display.isNotEmpty() && idn.isEmpty()) return
        val arr = read(ctx)
        arr.add(0, Entry(display, idn, reason, ts))
        val out = JSONArray()
        arr.take(MAX).forEach { e ->
            out.put(JSONObject().put("n", e.display).put("i", e.idn).put("r", e.reason).put("t", e.ts))
        }
        prefs(ctx).edit().putString(KEY, out.toString()).apply()
    }

    /** Newest pehle. Corrupt/adhuri entries chupchaap skip hoti hain. */
    fun list(ctx: Context): List<Entry> = read(ctx)

    @Synchronized fun clear(ctx: Context) { prefs(ctx).edit().remove(KEY).apply() }

    private fun read(ctx: Context): MutableList<Entry> {
        val raw = prefs(ctx).getString(KEY, null) ?: return mutableListOf()
        val arr = try { JSONArray(raw) } catch (e: Exception) { return mutableListOf() }
        val out = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val r = o.optString("r")
            if (!validReason(r)) continue
            val ts = o.optLong("t", 0L)
            if (ts <= 0L) continue
            out.add(Entry(o.optString("n"), o.optString("i"), r, ts))
        }
        return out
    }
}
