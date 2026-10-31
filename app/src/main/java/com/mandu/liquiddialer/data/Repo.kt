package com.mandu.liquiddialer.data

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Contact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val altName: String,
    val number: String,
    val label: String,
    val starred: Boolean,
    val photo: String?,
)

data class Recent(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long,
    val accountId: String?,
    val photo: String?,
)

data class SimInfo(val handle: PhoneAccountHandle, val label: String, val slot: Int)

data class Note(val id: Long, val number: String, val name: String, val text: String, val time: Long)

object Repo {

    fun normalize(n: String): String = n.filter { it.isDigit() || it == '+' }

    /** last 10 digits — good enough to match +91 / 0 / plain numbers */
    fun key(n: String): String = n.filter { it.isDigit() }.takeLast(10)

    // ---------------- contacts ----------------
    @SuppressLint("Range")
    fun contacts(ctx: Context, sortByLast: Boolean): List<Contact> {
        val out = LinkedHashMap<Long, Contact>()
        val proj = arrayOf(
            CommonDataKinds.Phone.CONTACT_ID, CommonDataKinds.Phone.LOOKUP_KEY,
            CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, CommonDataKinds.Phone.DISPLAY_NAME_ALTERNATIVE,
            CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.LABEL,
            CommonDataKinds.Phone.STARRED, CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI, CommonDataKinds.Phone.IS_SUPER_PRIMARY,
        )
        val sort = (if (sortByLast) CommonDataKinds.Phone.DISPLAY_NAME_ALTERNATIVE else CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY) + " COLLATE NOCASE ASC"
        try {
            ctx.contentResolver.query(CommonDataKinds.Phone.CONTENT_URI, proj, null, null, sort)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    if (out.containsKey(id) && c.getInt(9) == 0) continue
                    val type = c.getInt(5)
                    val label = CommonDataKinds.Phone.getTypeLabel(ctx.resources, type, c.getString(6) ?: "").toString()
                    out[id] = Contact(id, c.getString(1) ?: "", c.getString(2) ?: (c.getString(4) ?: ""), c.getString(3) ?: (c.getString(2) ?: ""),
                        c.getString(4) ?: "", label, c.getInt(7) == 1, c.getString(8))
                }
            }
        } catch (e: Exception) { }
        return out.values.toList()
    }

    data class ContactDetail(
        val id: Long, val rawId: Long, val lookupKey: String, val name: String,
        val phones: List<Pair<String, Int>>, val emails: List<String>, val starred: Boolean, val photo: String?,
    )

    fun contactDetail(ctx: Context, contactId: Long): ContactDetail? {
        val cr = ctx.contentResolver
        var name = ""; var lookup = ""; var starred = false; var photo: String? = null; var rawId = -1L
        cr.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.STARRED, ContactsContract.Contacts.PHOTO_URI), "${ContactsContract.Contacts._ID}=?", arrayOf("$contactId"), null)?.use {
            if (!it.moveToFirst()) return null
            name = it.getString(0) ?: ""; lookup = it.getString(1) ?: ""; starred = it.getInt(2) == 1; photo = it.getString(3)
        } ?: return null
        cr.query(ContactsContract.RawContacts.CONTENT_URI, arrayOf(ContactsContract.RawContacts._ID), "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0", arrayOf("$contactId"), null)?.use {
            if (it.moveToFirst()) rawId = it.getLong(0)
        }
        val phones = mutableListOf<Pair<String, Int>>()
        cr.query(CommonDataKinds.Phone.CONTENT_URI, arrayOf(CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.TYPE), "${CommonDataKinds.Phone.CONTACT_ID}=?", arrayOf("$contactId"), null)?.use {
            while (it.moveToNext()) { val n = it.getString(0) ?: continue; if (phones.none { p -> key(p.first) == key(n) }) phones += n to it.getInt(1) }
        }
        val emails = mutableListOf<String>()
        cr.query(CommonDataKinds.Email.CONTENT_URI, arrayOf(CommonDataKinds.Email.ADDRESS), "${CommonDataKinds.Email.CONTACT_ID}=?", arrayOf("$contactId"), null)?.use {
            while (it.moveToNext()) it.getString(0)?.let { e -> emails += e }
        }
        return ContactDetail(contactId, rawId, lookup, name, phones, emails, starred, photo)
    }

    /** Insert or update a contact in the real contacts provider (device-local account). */
    fun saveContact(ctx: Context, existing: ContactDetail?, name: String, phones: List<Pair<String, Int>>, email: String, photo: ByteArray?, starred: Boolean): Boolean {
        val ops = ArrayList<ContentProviderOperation>()
        val dataUri = ContactsContract.Data.CONTENT_URI
        if (existing == null || existing.rawId < 0) {
            ops += ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .withValue(ContactsContract.RawContacts.STARRED, if (starred) 1 else 0).build()
            fun ins() = ContentProviderOperation.newInsert(dataUri).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name).build()
            phones.filter { it.first.isNotBlank() }.forEach { (n, t) ->
                ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Phone.NUMBER, n).withValue(CommonDataKinds.Phone.TYPE, t).build()
            }
            if (email.isNotBlank()) ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Email.ADDRESS, email).withValue(CommonDataKinds.Email.TYPE, CommonDataKinds.Email.TYPE_HOME).build()
            if (photo != null) ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Photo.PHOTO, photo).build()
        } else {
            val raw = existing.rawId
            val sel = "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
            fun del(mime: String) { ops += ContentProviderOperation.newDelete(dataUri).withSelection(sel, arrayOf("$raw", mime)).build() }
            fun ins() = ContentProviderOperation.newInsert(dataUri).withValue(ContactsContract.Data.RAW_CONTACT_ID, raw)
            del(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name).build()
            del(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            phones.filter { it.first.isNotBlank() }.forEach { (n, t) ->
                ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Phone.NUMBER, n).withValue(CommonDataKinds.Phone.TYPE, t).build()
            }
            del(CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            if (email.isNotBlank()) ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Email.ADDRESS, email).withValue(CommonDataKinds.Email.TYPE, CommonDataKinds.Email.TYPE_HOME).build()
            if (photo != null) {
                del(CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                ops += ins().withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Photo.PHOTO, photo).build()
            }
            ops += ContentProviderOperation.newUpdate(ContactsContract.Contacts.CONTENT_URI)
                .withSelection("${ContactsContract.Contacts._ID}=?", arrayOf("${existing.id}"))
                .withValue(ContactsContract.Contacts.STARRED, if (starred) 1 else 0).build()
        }
        return try { ctx.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops); true } catch (e: Exception) { false }
    }

    fun deleteContact(ctx: Context, contactId: Long, lookupKey: String): Boolean = try {
        val uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
        ctx.contentResolver.delete(uri, null, null) > 0
    } catch (e: Exception) { false }

    fun setStarred(ctx: Context, contactId: Long, starred: Boolean) {
        try {
            val v = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) }
            ctx.contentResolver.update(ContactsContract.Contacts.CONTENT_URI, v, "${ContactsContract.Contacts._ID}=?", arrayOf("$contactId"))
        } catch (e: Exception) { }
    }

    fun lookupName(ctx: Context, number: String): Pair<String, String?>? {
        if (number.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI), null, null, null)?.use {
                if (it.moveToFirst()) (it.getString(0) ?: "") to it.getString(1) else null
            }
        } catch (e: Exception) { null }
    }

    /** Contact phone type label (Mobile/Work/Home...) for incoming-call display. */
    fun contactLabel(ctx: Context, number: String): String? {
        if (number.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.STARRED), null, null, null)?.use { c ->
                if (!c.moveToFirst()) return null
                val starred = c.getInt(1) == 1
                val cid = contactIdFor(ctx, number) ?: return if (starred) "Favourite" else null
                ctx.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL),
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(cid.toString()), null)?.use { p2 ->
                    if (p2.moveToFirst()) ContactsContract.CommonDataKinds.Phone.getTypeLabel(ctx.resources, p2.getInt(0), p2.getString(1))?.toString() else null
                }?.let { lbl -> if (starred) "$lbl · Favourite" else lbl } ?: if (starred) "Favourite" else null
            }
        } catch (e: Exception) { null }
    }

    /** True jab number kisi starred (favourite) contact ka ho. */
    fun contactStarred(ctx: Context, number: String): Boolean {
        if (number.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.STARRED), null, null, null)?.use { c ->
                c.moveToFirst() && c.getInt(0) == 1
            } ?: false
        } catch (e: Exception) { false }
    }

    /** Latest call-log entry for a number: (dateMillis, type, durationSec). */
    fun lastCallFor(ctx: Context, number: String): Triple<Long, Int, Long>? {
        if (number.isBlank()) return null
        return try {
            ctx.contentResolver.query(CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION),
                CallLog.Calls.NUMBER + "=?", arrayOf(number), CallLog.Calls.DATE + " DESC")?.use {
                if (it.moveToFirst()) Triple(it.getLong(0), it.getInt(1), it.getLong(2)) else null
            }
        } catch (e: Exception) { null }
    }

    fun contactIdFor(ctx: Context, number: String): Long? = try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { if (it.moveToFirst()) it.getLong(0) else null }
    } catch (e: Exception) { null }

    // ---------------- T9 ----------------
    private val t9 = mapOf('a' to '2','b' to '2','c' to '2','d' to '3','e' to '3','f' to '3','g' to '4','h' to '4','i' to '4','j' to '5','k' to '5','l' to '5',
        'm' to '6','n' to '6','o' to '6','p' to '7','q' to '7','r' to '7','s' to '7','t' to '8','u' to '8','v' to '8','w' to '9','x' to '9','y' to '9','z' to '9')
    fun t9Of(s: String) = s.lowercase().map { c -> if (c.isDigit()) c else t9[c] ?: ' ' }.joinToString("")
    fun matches(c: Contact, q: String): Boolean {
        if (q.isBlank()) return true
        val ql = q.trim().lowercase()
        if (c.name.lowercase().contains(ql)) return true
        val digits = ql.filter { it.isDigit() }
        if (digits.isNotEmpty() && digits.length == ql.replace(" ", "").replace("+", "").length) {
            if (c.number.filter { it.isDigit() }.contains(digits)) return true
            // T9: any word start, or whole name
            val words = c.name.split(" ", "-", ".").filter { it.isNotBlank() }
            if (words.any { t9Of(it).startsWith(digits) }) return true
            if (t9Of(c.name.replace(" ", "")).contains(digits)) return true
            // initials
            if (words.map { t9Of(it.take(1)) }.joinToString("").startsWith(digits)) return true
        }
        return false
    }

    // ---------------- call log ----------------
    fun recents(ctx: Context, limit: Int = 300): List<Recent> {
        val out = mutableListOf<Recent>()
        try {
            val proj = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE,
                CallLog.Calls.DURATION, CallLog.Calls.PHONE_ACCOUNT_ID, CallLog.Calls.CACHED_PHOTO_URI)
            ctx.contentResolver.query(CallLog.Calls.CONTENT_URI, proj, null, null, CallLog.Calls.DATE + " DESC")?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    out += Recent(c.getLong(0), c.getString(1) ?: "", c.getString(2)?.takeIf { it.isNotBlank() }, c.getInt(3), c.getLong(4), c.getLong(5), c.getString(6), c.getString(7))
                }
            }
        } catch (e: Exception) { }
        return out
    }

    fun historyFor(ctx: Context, number: String): List<Recent> = recents(ctx, 2000).filter { key(it.number) == key(number) && key(number).isNotEmpty() }.take(50)

    fun deleteRecent(ctx: Context, id: Long) { try { ctx.contentResolver.delete(CallLog.Calls.CONTENT_URI, "${CallLog.Calls._ID}=?", arrayOf("$id")) } catch (e: Exception) { } }
    fun deleteRecentsFor(ctx: Context, number: String) { try { ctx.contentResolver.delete(CallLog.Calls.CONTENT_URI, "${CallLog.Calls.NUMBER}=?", arrayOf(number)) } catch (e: Exception) { } }
    fun clearRecents(ctx: Context) { try { ctx.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null) } catch (e: Exception) { } }
    fun markMissedRead(ctx: Context) {
        try {
            val v = ContentValues().apply { put(CallLog.Calls.NEW, 0); put(CallLog.Calls.IS_READ, 1) }
            ctx.contentResolver.update(CallLog.Calls.CONTENT_URI, v, "${CallLog.Calls.TYPE}=? AND ${CallLog.Calls.NEW}=1", arrayOf("${CallLog.Calls.MISSED_TYPE}"))
        } catch (e: Exception) { }
    }

    // ---------------- blocking ----------------
    fun canBlock(ctx: Context) = try { BlockedNumberContract.canCurrentUserBlockNumbers(ctx) } catch (e: Exception) { false }
    fun isBlocked(ctx: Context, n: String) = try { BlockedNumberContract.isBlocked(ctx, n) } catch (e: Exception) { false }
    fun block(ctx: Context, n: String): Boolean = try {
        val v = ContentValues().apply { put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, n) }
        ctx.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, v) != null
    } catch (e: Exception) { false }
    fun unblock(ctx: Context, n: String): Boolean = try { BlockedNumberContract.unblock(ctx, n) > 0 } catch (e: Exception) { false }
    fun blocked(ctx: Context): List<String> {
        val out = mutableListOf<String>()
        try {
            ctx.contentResolver.query(BlockedNumberContract.BlockedNumbers.CONTENT_URI, arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER), null, null, null)?.use {
                while (it.moveToNext()) it.getString(0)?.let { n -> out += n }
            }
        } catch (e: Exception) { }
        return out
    }

    // ---------------- SIMs ----------------
    @SuppressLint("MissingPermission")
    fun sims(ctx: Context): List<SimInfo> = try {
        val tm = ctx.getSystemService(TelecomManager::class.java)
        tm.callCapablePhoneAccounts.mapIndexed { i, h ->
            val acc = tm.getPhoneAccount(h)
            SimInfo(h, acc?.label?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${i + 1}", i + 1)
        }
    } catch (e: Exception) { emptyList() }

    @SuppressLint("MissingPermission")
    fun voicemailNumber(ctx: Context): String = try {
        ctx.getSystemService(TelephonyManager::class.java).voiceMailNumber ?: ""
    } catch (e: Exception) { "" }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun ownNumbers(ctx: Context): List<String> {
        val out = mutableListOf<String>()
        try {
            val sm = ctx.getSystemService(android.telephony.SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList?.forEach { info ->
                @Suppress("DEPRECATION")
                val n = if (android.os.Build.VERSION.SDK_INT >= 33) sm.getPhoneNumber(info.subscriptionId) else info.number
                if (!n.isNullOrBlank()) out.add("SIM ${info.simSlotIndex + 1} · ${info.displayName ?: ""}: ${pretty(n)}")
            }
        } catch (e: Exception) { }
        if (out.isEmpty()) ownNumber(ctx).takeIf { it.isNotBlank() }?.let { out.add(pretty(it)) }
        return out
    }

    /** Decodes the picked image, centre-crops it to a 512px circle and stores it in app storage. Returns a file:// uri. */
    fun saveProfilePhoto(ctx: Context, uri: android.net.Uri): String? = try {
        val src: android.graphics.Bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
                val m = maxOf(info.size.width, info.size.height)
                if (m > 1600) { val f = 1600f / m; d.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1)) }
                d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val o = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
            ctx.contentResolver.openInputStream(uri)!!.use { android.graphics.BitmapFactory.decodeStream(it, null, o)!! }
        }
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2; val y = (src.height - side) / 2
        val out = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        c.drawCircle(256f, 256f, 256f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        c.drawBitmap(src, android.graphics.Rect(x, y, x + side, y + side), android.graphics.Rect(0, 0, 512, 512), paint)
        val f = File(ctx.filesDir, "profile.png")
        f.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        "file://${f.absolutePath}?v=${System.currentTimeMillis()}"
    } catch (e: Exception) { null }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun ownNumber(ctx: Context): String = try { ctx.getSystemService(TelephonyManager::class.java).line1Number ?: "" } catch (e: Exception) { "" }

    fun pretty(n: String): String = try { PhoneNumberUtils.formatNumber(n, "IN") ?: n } catch (e: Exception) { n }

    // ---------------- notes ----------------
    private fun notesFile(ctx: Context) = File(ctx.filesDir, "notes.json")
    fun notes(ctx: Context): List<Note> = try {
        val a = JSONArray(notesFile(ctx).takeIf { it.exists() }?.readText() ?: "[]")
        (0 until a.length()).map { i -> a.getJSONObject(i).let { Note(it.getLong("id"), it.getString("number"), it.optString("name"), it.getString("text"), it.getLong("time")) } }.sortedByDescending { it.time }
    } catch (e: Exception) { emptyList() }
    private fun saveNotes(ctx: Context, list: List<Note>) {
        val a = JSONArray(); list.forEach { a.put(JSONObject().put("id", it.id).put("number", it.number).put("name", it.name).put("text", it.text).put("time", it.time)) }
        notesFile(ctx).writeText(a.toString())
    }
    @Synchronized fun addNote(ctx: Context, number: String, name: String, text: String) {
        if (text.isBlank()) return
        saveNotes(ctx, notes(ctx) + Note(System.currentTimeMillis(), number, name, text.trim(), System.currentTimeMillis()))
    }
    @Synchronized fun deleteNote(ctx: Context, id: Long) = saveNotes(ctx, notes(ctx).filter { it.id != id })
    fun notesFor(ctx: Context, number: String) = notes(ctx).filter { key(it.number) == key(number) }

    // ---------------- recordings ----------------
    fun recordingsDir(ctx: Context): File = File(ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: ctx.filesDir, "LiquidDialer").apply { mkdirs() }
    fun recordings(ctx: Context): List<File> = recordingsDir(ctx).listFiles()?.filter { it.isFile && it.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList()

// ---- recents insights (naye features): aaj ke stats + unseen missed badge ----
data class DayStats(val calls: Int, val talkSecs: Long, val missed: Int)

fun dayStats(recents: List<Recent>, nowMs: Long): DayStats {
    val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val today = recents.filter { dayFmt.format(Date(it.date)) == dayFmt.format(Date(nowMs)) }
    val missed = today.count { it.type == CallLog.Calls.MISSED_TYPE }
    val talk = today.filter { it.type == CallLog.Calls.INCOMING_TYPE || it.type == CallLog.Calls.OUTGOING_TYPE }.sumOf { it.duration }
    return DayStats(today.size, talk, missed)
}

// ---- insights (sirf LOADED recents ka hisaab - poori call history ka dawa NAHI) ----

data class Insights(
    val calls: Int, val talkSecs: Long, val missed: Int,
    val topName: String?, val topCount: Int,
    val longestName: String?, val longestSecs: Long,
    val busiestLabel: String?, val busiestCount: Int,
)

/** Loaded recents se hisaab. missed = sirf sachmuch MISSED_TYPE (badge semantics ke saath match). */
fun insights(recents: List<Recent>): Insights {
    if (recents.isEmpty()) return Insights(0, 0, 0, null, 0, null, 0, null, 0)
    val answered = recents.filter { it.type == CallLog.Calls.INCOMING_TYPE || it.type == CallLog.Calls.OUTGOING_TYPE }
    val talk = answered.sumOf { it.duration }
    val missed = recents.count { it.type == CallLog.Calls.MISSED_TYPE }
    val topEntry = recents.filter { it.number.isNotBlank() }.groupBy { it.number }.maxByOrNull { it.value.size }
    val topName = topEntry?.let { e -> e.value.firstOrNull { !it.name.isNullOrBlank() }?.name ?: pretty(e.key) }
    val longest = answered.filter { it.duration > 0 }.maxByOrNull { it.duration }
    val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val labelFmt = SimpleDateFormat("d MMM", Locale.getDefault())
    val busiest = recents.groupBy { dayFmt.format(Date(it.date)) }.maxByOrNull { it.value.size }
    return Insights(
        calls = recents.size,
        talkSecs = talk,
        missed = missed,
        topName = topName, topCount = topEntry?.value?.size ?: 0,
        longestName = longest?.let { it.name ?: pretty(it.number) }, longestSecs = longest?.duration ?: 0,
        busiestLabel = busiest?.let { labelFmt.format(Date(it.value.first().date)) }, busiestCount = busiest?.value?.size ?: 0,
    )
}

// ---- duplicate contacts (SIRF detection - merge/delete kuch nahi hota) ----

data class DupGroup(val numberKey: String, val pretty: String, val contacts: List<Contact>, val count: Int)

/** POSSIBLE duplicates: aakhri 10 digits (key) same. Conservative full-number match NAHI -
 *  alag country code + same last-10 bhi ek group me aa sakte hain (isliye UI "possible" bolta hai, pakka nahi).
 *  Id-distinct hona zaroori; 6+ digit keys only (service numbers ignore). Count desc. */
fun duplicateGroups(contacts: List<Contact>): List<DupGroup> =
    contacts.filter { it.number.isNotBlank() }
        .groupBy { key(it.number) }
        .filter { (k, l) -> k.length >= 6 && l.map { it.id }.distinct().size > 1 }
        .map { (k, l) ->
            val d = l.distinctBy { it.id }
            DupGroup(k, pretty(d.first().number), d, d.size)
        }
        .sortedByDescending { it.count }

// ---- birthdays (lazy strip; startup query NAHI - Recents screen khulne ke BAAD load hota hai) ----

data class Birthday(val name: String, val number: String, val daysUntil: Int)

/** Birthday string ("yyyy-MM-dd" ya "--MM-dd") se agle occurrence tak ke din. Parse na ho to null. */
fun daysUntilBirthday(raw: String, today: java.time.LocalDate): Int? {
    val parts = raw.trim().split("-").filter { it.isNotEmpty() }
    if (parts.size < 2) return null
    val (month, day) = try {
        if (parts.size == 2) parts[0].toInt() to parts[1].toInt()          // --MM-dd
        else parts[1].toInt() to parts[2].toInt()                          // yyyy-MM-dd
    } catch (_: Exception) { return null }
    if (month !in 1..12 || day !in 1..31) return null
    var next = try { java.time.LocalDate.of(today.year, month, day) }
        catch (_: Exception) {
            // 29 Feb: non-leap saal me 28 Feb ko manaate hain
            if (month == 2 && day == 29) java.time.LocalDate.of(today.year, 2, 28) else return null
        }
    if (next.isBefore(today)) next = try { java.time.LocalDate.of(today.year + 1, month, day) }
        catch (_: Exception) { java.time.LocalDate.of(today.year + 1, 2, 28) }
    return java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()
}

/** Contacts ke birthdays agle `withinDays` dinon me. LAZY call - startup pe mat chalao. */
fun birthdays(ctx: Context, withinDays: Int = 7): List<Birthday> {
    val today = java.time.LocalDate.now()
    val out = LinkedHashMap<String, Birthday>()
    val proj = arrayOf(
        CommonDataKinds.Event.CONTACT_ID, CommonDataKinds.Event.DISPLAY_NAME,
        CommonDataKinds.Event.START_DATE,
    )
    val sel = "${CommonDataKinds.Event.MIMETYPE}=? AND ${CommonDataKinds.Event.TYPE}=?"
    val args = arrayOf(CommonDataKinds.Event.CONTENT_ITEM_TYPE, "${CommonDataKinds.Event.TYPE_BIRTHDAY}")
    try {
        ctx.contentResolver.query(ContactsContract.Data.CONTENT_URI, proj, sel, args, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                val days = daysUntilBirthday(c.getString(2) ?: continue, today) ?: continue
                if (days > withinDays) continue
                // number: strip tap pe number sheet kholne ke liye pehla phone
                val num = ctx.contentResolver.query(
                    CommonDataKinds.Phone.CONTENT_URI, arrayOf(CommonDataKinds.Phone.NUMBER),
                    "${CommonDataKinds.Phone.CONTACT_ID}=?", arrayOf("${c.getLong(0)}"), null
                )?.use { p -> if (p.moveToFirst()) p.getString(0) ?: "" else "" } ?: ""
                out.getOrPut(name) { Birthday(name, num, days) }
            }
        }
    } catch (_: Exception) { }
    return out.values.sortedBy { it.daysUntil }
}

// sirf sachmuch MISSED (ring hui, user ne nahi uthaya): user-rejected ya app-blocked calls
// "missed" nahi hoti - unka badge dena user ko galat signal deta hai (blocked ka apna section hai)
fun unseenMissed(recents: List<Recent>, seenAt: Long): Int =
    recents.count { it.date > seenAt && it.type == CallLog.Calls.MISSED_TYPE }
}
