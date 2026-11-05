package com.mandu.liquiddialer.data

import android.content.Context

/** Per-number controls. Defaults OFF, no migration. Keys preserve full international numbers. */
object ContactControls {
    private const val PREF = "contact_controls"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Explicit +/00 international forms share an identity. Only unambiguous Indian mobile
     * formats are mapped to +91. Other local numbers keep their full digits and local namespace.
     * Never infer a foreign country or discard a significant national leading zero. */
    internal fun k(number: String): String {
        val text = number.trim()
        if (text.any { !(it.isDigit() || it in "+-(). ") }) return ""
        val raw = text.filter { it.isDigit() }
        if (raw.isEmpty()) return ""
        if (text.startsWith("+")) return "+$raw"
        if (raw.startsWith("00")) return raw.drop(2).takeIf { it.isNotEmpty() }?.let { "+$it" } ?: ""
        val mobile = when {
            raw.length == 10 && raw[0] in '6'..'9' -> raw
            raw.length == 11 && raw[0] == '0' && raw[1] in '6'..'9' -> raw.drop(1)
            raw.length == 12 && raw.startsWith("91") && raw[2] in '6'..'9' -> raw.drop(2)
            else -> null
        }
        return if (mobile != null) "+91$mobile" else "local:$raw"
    }

    /** SIM pin: is number ke liye user ne jo SIM khud chuni (PhoneAccountHandle id). null = auto. */
    fun simPin(ctx: Context, number: String): String? =
        if (k(number).isEmpty()) null else prefs(ctx).getString(k(number) + ":sim", null)

    fun setSimPin(ctx: Context, number: String, handleId: String?) {
        val key = k(number); if (key.isEmpty()) return
        prefs(ctx).edit().apply { if (handleId == null) remove("$key:sim") else putString("$key:sim", handleId) }.apply()
    }

    /** Is number ko kabhi block mat karo: unknown aur spam-series blocking se upar.
     * Private/hidden pe lagoo nahi (number hota hi nahi). System block list pe effect nahi. */
    fun blockNever(ctx: Context, number: String): Boolean =
        k(number).isNotEmpty() && prefs(ctx).getBoolean(k(number) + ":neverblock", false)

    fun setBlockNever(ctx: Context, number: String, on: Boolean) {
        val key = k(number); if (key.isEmpty()) return
        prefs(ctx).edit().putBoolean(key + ":neverblock", on).apply()
    }

    /** Never-block list, keyed identities (local: prefix ke saath), sorted. */
    fun blockNeverList(ctx: Context): List<String> =
        prefs(ctx).all.filter { it.key.endsWith(":neverblock") && it.value == true }
            .keys.map { it.removeSuffix(":neverblock") }.sorted()

    /** Quiet hours me bhi ring karne do (per-number bypass). */
    fun quietAllow(ctx: Context, number: String): Boolean =
        k(number).isNotEmpty() && prefs(ctx).getBoolean(k(number) + ":quiet", false)

    fun setQuietAllow(ctx: Context, number: String, on: Boolean) {
        val key = k(number); if (key.isEmpty()) return
        prefs(ctx).edit().putBoolean(key + ":quiet", on).apply()
    }

    /** Is number ki call hamesha record (global auto-record off ho tab bhi). */
    fun recordAlways(ctx: Context, number: String): Boolean =
        k(number).isNotEmpty() && prefs(ctx).getBoolean(k(number) + ":record", false)

    fun setRecordAlways(ctx: Context, number: String, on: Boolean) {
        val key = k(number); if (key.isEmpty()) return
        prefs(ctx).edit().putBoolean(key + ":record", on).apply()
    }

    /** Is number ka naam ring pe bolo (global announce off ho tab bhi). */
    fun announceAlways(ctx: Context, number: String): Boolean =
        k(number).isNotEmpty() && prefs(ctx).getBoolean(k(number) + ":announce", false)

    fun setAnnounceAlways(ctx: Context, number: String, on: Boolean) {
        val key = k(number); if (key.isEmpty()) return
        prefs(ctx).edit().putBoolean(key + ":announce", on).apply()
    }

    // ---- Pure decisions (unit-testable) ----

    /** Per-number record: global rule YA explicit per-number pin. */
    fun shouldAutoRecord(globalAuto: Boolean, unknownOnly: Boolean, isKnown: Boolean, contactRecord: Boolean): Boolean =
        (globalAuto && (!unknownOnly || !isKnown)) || contactRecord

    /** Per-number announce: global rule YA explicit per-number pin. */
    fun shouldAnnounce(globalAnnounce: Boolean, onlyFav: Boolean, isFav: Boolean, contactAnnounce: Boolean): Boolean =
        (globalAnnounce && (!onlyFav || isFav)) || contactAnnounce
}
