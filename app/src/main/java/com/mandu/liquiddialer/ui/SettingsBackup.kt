package com.mandu.liquiddialer.ui

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.LdSettings
import com.mandu.liquiddialer.data.ldStore
import org.json.JSONObject

/**
 * Settings backup/restore - SIRF appearance/display settings ka chhota whitelist.
 * Backup file UNTRUSTED input hai: pura file pehle validate hota hai, koi bhi value
 * type/range/enum se bahar ho to KUCH BHI apply nahi hota (koi partial mutation nahi).
 * Is scope me call behavior, blocking/screening, recording, announce, SIM, voicemail,
 * quick replies, speed dial, quiet hours, app lock, personal data (naam/number/photo)
 * aur blocked numbers NAHI aate - na export, na restore.
 */
object SettingsBackup {
    /** Validated restore candidates awaiting the user's explicit confirm (null = koi preview nahi). */
    val pendingRestore = kotlinx.coroutines.flow.MutableStateFlow<Map<String, Any>?>(null)

    const val KIND = "appearance-settings"
    const val VERSION = 1
    const val MAX_BYTES = 64 * 1024

    private val BOOL_KEYS = listOf(
        "motion", "sortByLast", "lastNameFirst", "tabSwipe", "boldText",
        "showFavourites", "showLastCall", "showContactType", "favStarBadge",
        "keypadLetters", "callShowNumber", "formatNumber",
    )
    private val STR_ENUMS = mapOf(
        "lang" to LANGS.map { it.first }.toSet(),
        "theme" to (THEME_KEYS + "auto").toSet(),
        "callAvatarShape" to setOf("circle", "squircle"),
        "callBgStyle" to setOf("blobs", "photo", "solid"),
        "defaultTab" to setOf("recents", "dialer"),
    )
    private val INT_RANGES = mapOf("blur" to (0..40))
    private val FLOAT_RANGES = mapOf(
        "fontScale" to (0.8f..1.5f),
        "gridScale" to (0.8f..1.2f),
        "bottomScale" to (0.8f..1.2f),
        "callCardScale" to (0.8f..1.3f),
        "dialerScale" to (0.85f..1.25f),
    )
    private val LAYOUT_KEYS = mapOf("layoutGrid" to GRID_IDS, "layoutBottom" to BOTTOM_IDS)
    private const val ACCENT = "accent" // Long, ARGB color 0..0xFFFFFFFF

    val WHITELIST: Set<String> = (BOOL_KEYS + STR_ENUMS.keys + INT_RANGES.keys + FLOAT_RANGES.keys + LAYOUT_KEYS.keys + ACCENT).toSet()

    fun buildJson(s: LdSettings): String {
        val v = JSONObject()
        v.put("lang", s.lang); v.put("theme", s.theme); v.put(ACCENT, s.accent)
        v.put("blur", s.blur); v.put("motion", s.motion); v.put("fontScale", s.fontScale.toDouble())
        v.put("sortByLast", s.sortByLast); v.put("lastNameFirst", s.lastNameFirst)
        v.put("tabSwipe", s.tabSwipe); v.put("boldText", s.boldText)
        v.put("showFavourites", s.showFavourites); v.put("showLastCall", s.showLastCall)
        v.put("showContactType", s.showContactType); v.put("favStarBadge", s.favStarBadge)
        v.put("keypadLetters", s.keypadLetters); v.put("callShowNumber", s.callShowNumber)
        v.put("formatNumber", s.formatNumber); v.put("defaultTab", s.defaultTab)
        v.put("gridScale", s.gridScale.toDouble()); v.put("bottomScale", s.bottomScale.toDouble())
        v.put("callCardScale", s.callCardScale.toDouble()); v.put("dialerScale", s.dialerScale.toDouble())
        v.put("callAvatarShape", s.callAvatarShape); v.put("callBgStyle", s.callBgStyle)
        v.put("layoutGrid", s.layoutGrid); v.put("layoutBottom", s.layoutBottom)
        return JSONObject()
            .put("app", "liquid-dialer")
            .put("kind", KIND)
            .put("version", VERSION)
            .put("values", v)
            .toString(2)
    }

    sealed class ParseResult {
        data class Ok(val values: Map<String, Any>, val ignoredKeys: Int) : ParseResult()
        data class Err(val why: String) : ParseResult()
    }

    private fun validLayout(v: String, ids: List<String>): Boolean {
        val toks = v.split(",")
        if (toks.size != ids.size) return false
        val seen = mutableSetOf<String>()
        toks.forEach { t ->
            val id = t.removePrefix("!")
            if (id !in ids || !seen.add(id)) return false
        }
        return true
    }

    fun parse(text: String): ParseResult {
        if (text.length > MAX_BYTES) return ParseResult.Err("file bahut badi hai")
        val obj = try { JSONObject(text) } catch (e: Exception) { return ParseResult.Err("JSON file nahi hai") }
        if (obj.optString("app") != "liquid-dialer" || obj.optString("kind") != KIND)
            return ParseResult.Err("ye Liquid Dialer ka appearance settings backup nahi hai")
        val ver = obj.opt("version")
        if (ver !is Int || ver < 1) return ParseResult.Err("backup version samajh nahi aaya")
        if (ver > VERSION) return ParseResult.Err("ye backup naye app version ka hai - pehle app update karo")
        val values = obj.optJSONObject("values") ?: return ParseResult.Err("backup me settings values nahi hain")
        val out = mutableMapOf<String, Any>()
        var ignored = 0
        val it = values.keys()
        while (it.hasNext()) {
            val k = it.next()
            if (k !in WHITELIST) { ignored++; continue }
            val raw = values.get(k)
            when {
                k in BOOL_KEYS -> if (raw is Boolean) out[k] = raw else return ParseResult.Err("'$k' ka type galat hai")
                k in STR_ENUMS -> {
                    if (raw !is String || raw !in STR_ENUMS.getValue(k)) return ParseResult.Err("'$k' ki value manzoor nahi")
                    out[k] = raw
                }
                k in INT_RANGES -> {
                    if (raw !is Number || raw.toLong() != raw.toDouble().toLong()) return ParseResult.Err("'$k' ka type galat hai")
                    val n = raw.toInt()
                    if (n !in INT_RANGES.getValue(k)) return ParseResult.Err("'$k' range se bahar hai")
                    out[k] = n
                }
                k in FLOAT_RANGES -> {
                    if (raw !is Number) return ParseResult.Err("'$k' ka type galat hai")
                    val f = raw.toFloat()
                    if (f.isNaN() || f !in FLOAT_RANGES.getValue(k)) return ParseResult.Err("'$k' range se bahar hai")
                    out[k] = f
                }
                k in LAYOUT_KEYS -> {
                    if (raw !is String || !validLayout(raw, LAYOUT_KEYS.getValue(k))) return ParseResult.Err("'$k' ka layout sahi nahi hai")
                    out[k] = raw
                }
                k == ACCENT -> {
                    if (raw !is Number) return ParseResult.Err("'accent' ka type galat hai")
                    val l = raw.toLong()
                    if (l < 0 || l > 0xFFFFFFFFL) return ParseResult.Err("'accent' range se bahar hai")
                    out[k] = l
                }
            }
        }
        if (out.isEmpty()) return ParseResult.Err("backup me koi pehchani gayi setting nahi mili")
        return ParseResult.Ok(out, ignored)
    }

    /** Content:// ya file:// URI se capped read; bahut badi/unreadable file pe null (kuch apply nahi hota). */
    fun readCapped(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            val buf = ByteArray(MAX_BYTES + 1)
            var off = 0
            while (off < buf.size) {
                val r = ins.read(buf, off, buf.size - off)
                if (r < 0) break
                off += r
            }
            if (off > MAX_BYTES) null else String(buf, 0, off, Charsets.UTF_8)
        }
    } catch (e: Exception) { null }

    /** Ek hi edit transaction me apply - ya to sab ya kuch nahi. Sirf whitelist keys chhuata hai. */
    suspend fun applyRestored(ctx: Context, values: Map<String, Any>) {
        ctx.ldStore.edit { p ->
            values.forEach { (k, v) ->
                when (k) {
                    "lang" -> p[K.lang] = v as String
                    "theme" -> p[K.theme] = v as String
                    "accent" -> p[K.accent] = v as Long
                    "blur" -> p[K.blur] = v as Int
                    "motion" -> p[K.motion] = v as Boolean
                    "fontScale" -> p[K.fontScale] = v as Float
                    "sortByLast" -> p[K.sortByLast] = v as Boolean
                    "lastNameFirst" -> p[K.lastNameFirst] = v as Boolean
                    "tabSwipe" -> p[K.tabSwipe] = v as Boolean
                    "boldText" -> p[K.boldText] = v as Boolean
                    "showFavourites" -> p[K.showFavourites] = v as Boolean
                    "showLastCall" -> p[K.showLastCall] = v as Boolean
                    "showContactType" -> p[K.showContactType] = v as Boolean
                    "favStarBadge" -> p[K.favStarBadge] = v as Boolean
                    "keypadLetters" -> p[K.keypadLetters] = v as Boolean
                    "callShowNumber" -> p[K.callShowNumber] = v as Boolean
                    "formatNumber" -> p[K.formatNumber] = v as Boolean
                    "defaultTab" -> p[K.defaultTab] = v as String
                    "gridScale" -> p[K.gridScale] = v as Float
                    "bottomScale" -> p[K.bottomScale] = v as Float
                    "callCardScale" -> p[K.callCardScale] = v as Float
                    "dialerScale" -> p[K.dialerScale] = v as Float
                    "callAvatarShape" -> p[K.callAvatarShape] = v as String
                    "callBgStyle" -> p[K.callBgStyle] = v as String
                    "layoutGrid" -> p[K.layoutGrid] = v as String
                    "layoutBottom" -> p[K.layoutBottom] = v as String
                }
            }
        }
    }
}
