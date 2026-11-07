package com.mandu.liquiddialer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Poora app backup - appearance settings, notes, call reminders, call lists aur blocked prefixes
 * ek hi user-picked JSON file me. Backup file UNTRUSTED input hai: parse() poori file pehle
 * validate karta hai; koi bhi section type/range/limit se bahar ho to poori file reject hoti hai
 * (koi partial apply nahi). Restore hamesha merge karta hai - phone ka existing data kabhi delete
 * nahi hota. Recordings/photos/call history is scope me NAHI hain.
 */
object FullBackup {
    const val KIND = "liquid-dialer-full-backup"
    const val VERSION = 1
    const val MAX_BYTES = 5 * 1024 * 1024
    const val MAX_NOTES = 10000
    const val MAX_REMINDERS = 10000
    const val MAX_PREFIXES = 1000

    /** Validated, ready-to-apply backup. parse() ke bina kabhi construct mat karo. */
    data class Plan(
        val settings: Map<String, Any>?,
        val notes: List<Note>,
        val reminders: List<CallReminders.Reminder>,
        val lists: List<CallLists.ListItem>,
        val blockedPrefixes: List<String>,
    )

    data class Summary(
        val notesAdded: Int, val notesKept: Int,
        val remindersSet: Int, val remindersSkipped: Int,
        val listsAdded: Int, val listsKept: Int,
        val prefixesAdded: Int, val settingsApplied: Boolean,
    )

    // ---------------- export ----------------

    @Synchronized fun export(ctx: Context): String {
        val root = JSONObject()
        root.put("kind", KIND); root.put("version", VERSION); root.put("createdAt", System.currentTimeMillis())
        root.put("settings", JSONObject(com.mandu.liquiddialer.ui.SettingsBackup.buildJson(LdApp.settings.value)))
        val notes = JSONArray(); Repo.notesChecked(ctx).forEach {
            notes.put(JSONObject().put("id", it.id).put("number", it.number).put("name", it.name).put("text", it.text).put("time", it.time))
        }
        root.put("notes", notes)
        val rems = JSONArray(); CallReminders.allRaw(ctx).forEach {
            rems.put(JSONObject().put("number", it.number).put("name", it.name).put("at", it.atMs))
        }
        root.put("reminders", rems)
        root.put("callLists", JSONObject(CallLists.run { encode(read(ctx)) }))
        val prefs = JSONArray(); LdApp.settings.value.blockedPrefixes.forEach { prefs.put(it) }
        root.put("blockedPrefixes", prefs)
        return root.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup 5 MB se bada hai; pehle purane notes/recordings hatao" } }
    }

    /** User-picked backup file ko capped aur strict-UTF8 read karo; kharab/bahut badi file pe exception. */
    fun read(ctx: Context, uri: android.net.Uri): String {
        val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(8192)
        (ctx.contentResolver.openInputStream(uri) ?: error("Backup file khul nahi rahi")).use { input ->
            while (true) { val n = input.read(buf); if (n < 0) break; require(out.size() + n <= MAX_BYTES) { "Backup 5 MB se bada hai" }; out.write(buf, 0, n) }
        }
        return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString()
    }

    // ---------------- parse (strict, no mutation) ----------------

    private fun strict(text: String): JSONObject {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup 5 MB se bada hai" }
        // JSONTokener uses raw NUL as its end marker; escaped NUL inside strings is valid data.
        require('\u0000' !in text) { "Backup format sahi nahi hai" }
        val t = JSONTokener(text); val o = t.nextValue() as? JSONObject ?: error("Backup format sahi nahi hai")
        require(t.nextClean() == '\u0000') { "Backup format sahi nahi hai" }
        return o
    }

    fun parse(text: String): Plan {
        val root = strict(text)
        require(root.get("kind") == KIND && root.get("version") is Int && root.getInt("version") == VERSION) { "Ye Liquid Dialer ka backup nahi hai" }

        val settings: Map<String, Any>? = if (root.has("settings")) {
            require(root.get("settings") is JSONObject) { "Settings section sahi nahi hai" }
            when (val r = com.mandu.liquiddialer.ui.SettingsBackup.parse(root.getJSONObject("settings").toString())) {
                is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Ok -> r.values
                is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err -> error("Settings section sahi nahi hai: " + r.why)
            }
        } else null

        val notes = mutableListOf<Note>()
        if (root.has("notes")) {
            val a = root.get("notes") as? JSONArray ?: error("Notes section sahi nahi hai")
            require(a.length() <= MAX_NOTES) { "Notes section bahut bada hai" }
            val ids = HashSet<Long>()
            for (i in 0 until a.length()) {
                val o = a.get(i) as? JSONObject ?: error("Notes section sahi nahi hai")
                require((o.get("id") is Int || o.get("id") is Long) && (o.get("time") is Int || o.get("time") is Long) && o.get("number") is String && o.get("text") is String) { "Notes section sahi nahi hai" }
                val id = o.getLong("id"); require(id > 0 && ids.add(id)) { "Notes section sahi nahi hai" }
                val name = if (o.has("name")) { require(o.get("name") is String) { "Notes section sahi nahi hai" }; o.getString("name") } else ""
                require(o.getString("text").length <= 50000) { "Notes section sahi nahi hai" }
                notes += Note(id, o.getString("number"), name, o.getString("text"), o.getLong("time"))
            }
        }

        val rems = mutableListOf<CallReminders.Reminder>()
        if (root.has("reminders")) {
            val a = root.get("reminders") as? JSONArray ?: error("Reminders section sahi nahi hai")
            require(a.length() <= MAX_REMINDERS) { "Reminders section bahut bada hai" }
            for (i in 0 until a.length()) {
                val o = a.get(i) as? JSONObject ?: error("Reminders section sahi nahi hai")
                require(o.get("number") is String && o.get("name") is String && (o.get("at") is Int || o.get("at") is Long)) { "Reminders section sahi nahi hai" }
                require(CallReminders.validNumber(o.getString("number"))) { "Reminders section sahi nahi hai" }
                require(o.getString("name").length <= 200) { "Reminders section sahi nahi hai" }
                val at = o.getLong("at"); require(at > 0) { "Reminders section sahi nahi hai" }
                rems += CallReminders.Reminder(o.getString("number"), o.getString("name"), at)
            }
        }

        val lists: List<CallLists.ListItem> = if (root.has("callLists")) {
            require(root.get("callLists") is JSONObject) { "Lists section sahi nahi hai" }
            try { CallLists.run { decode(root.getJSONObject("callLists").toString()) } }
            catch (e: Exception) { error("Lists section sahi nahi hai: " + (e.message ?: "")) }
        } else emptyList()

        val prefixes = mutableListOf<String>()
        if (root.has("blockedPrefixes")) {
            val a = root.get("blockedPrefixes") as? JSONArray ?: error("Blocked prefixes section sahi nahi hai")
            require(a.length() <= MAX_PREFIXES) { "Blocked prefixes section bahut bada hai" }
            val seen = HashSet<String>()
            for (i in 0 until a.length()) {
                val p = a.get(i) as? String ?: error("Blocked prefixes section sahi nahi hai")
                require(p.length in 3..16 && p.all { it.isDigit() || it == '+' } && p.lastIndexOf('+') <= 0 && seen.add(p)) { "Blocked prefixes section sahi nahi hai" }
                prefixes += p
            }
        }
        return Plan(settings, notes, rems, lists, prefixes)
    }

    // ---------------- apply (user-confirmed merge; kabhi delete nahi) ----------------

    @Synchronized fun apply(ctx: Context, plan: Plan, nowMs: Long = System.currentTimeMillis()): Summary {
        var notesAdded = 0
        if (plan.notes.isNotEmpty()) {
            val current = Repo.notesChecked(ctx); val have = current.mapTo(HashSet()) { it.id }
            val fresh = plan.notes.filter { it.id !in have }
            require(current.size + fresh.size <= MAX_NOTES) { "Notes ki limit aa gayi; pehle kuch purane notes hatao" }
            if (fresh.isNotEmpty()) Repo.run { importMerged(ctx, fresh) }
            notesAdded = fresh.size
        }
        var remsSet = 0; var remsSkipped = 0
        plan.reminders.forEach {
            if (it.atMs > nowMs) { CallReminders.set(ctx, it.number, it.name, it.atMs); remsSet++ } else remsSkipped++
        }
        var listsAdded = 0; var listsKept = 0
        if (plan.lists.isNotEmpty()) {
            val existing = CallLists.read(ctx).map { it.title.trim().lowercase() }.toHashSet()
            plan.lists.forEach { l ->
                if (l.title.trim().lowercase() in existing) { listsKept++; return@forEach }
                val after = CallLists.create(ctx, l.title)
                val newId = after.last().id
                l.entries.forEach { e ->
                    CallLists.add(ctx, newId, e.number, e.name)
                    if (e.outcome!="pending" || e.note.isNotEmpty() || e.updatedAt>0) {
                        val cur = CallLists.read(ctx).first { it.id == newId }
                        cur.entries.lastOrNull()?.let { CallLists.followUp(ctx, newId, it.id, e.outcome, e.note, e.updatedAt) }
                    }
                }
                existing += l.title.trim().lowercase(); listsAdded++
            }
        }
        var prefixesAdded = 0
        if (plan.blockedPrefixes.isNotEmpty()) {
            val cur = LdApp.settings.value.blockedPrefixes
            val merged = (cur + plan.blockedPrefixes).distinct()
            prefixesAdded = merged.size - cur.size
            if (prefixesAdded > 0) {
                kotlinx.coroutines.runBlocking {
                    ctx.ldStore.edit { it[K.blockedPrefixes] = merged.joinToString(";") }
                }
            }
        }
        var settingsApplied = false
        plan.settings?.let {
            kotlinx.coroutines.runBlocking { com.mandu.liquiddialer.ui.SettingsBackup.applyRestored(ctx, it) }
            settingsApplied = true
        }
        return Summary(notesAdded, plan.notes.size - notesAdded, remsSet, remsSkipped, listsAdded, listsKept, prefixesAdded, settingsApplied)
    }
}
