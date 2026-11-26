package com.mandu.liquiddialer.data

import android.content.Context
import com.mandu.liquiddialer.LdApp

/** App-local private numbers. Hides chosen numbers only inside this Dialer app's own lists.
 * The phone's system Contacts, call log and notifications are not touched. Identity is the full
 * international identity from ContactControls.k. Default OFF through the Settings switch. */
object PrivateNumbers {
    private const val PREF = "private_numbers"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    const val NOTE = "Sirf is Dialer app ki lists me hide hota hai: contacts, favourites, recents aur search. Phone ke system Contacts, call log aur notifications me ye numbers dikhte rahenge, aur call aate ya jaate waqt naam screen par aa sakta hai. Ye system-wide privacy nahi hai."

    fun id(number: String): String = ContactControls.k(number)

    fun isPrivate(ctx: Context, number: String): Boolean {
        val key = id(number)
        return key.isNotEmpty() && prefs(ctx).getBoolean(key, false)
    }

    fun set(ctx: Context, number: String, on: Boolean) {
        val key = id(number); if (key.isEmpty()) return
        prefs(ctx).edit().apply { if (on) putBoolean(key, true) else remove(key) }.apply()
    }

    /** Saved private identities, sorted. Kept even while the switch is OFF. */
    fun list(ctx: Context): List<String> =
        prefs(ctx).all.filter { it.value == true }.keys.sorted()

    fun enabled(): Boolean = runCatching { LdApp.settings.value.privateContacts }.getOrDefault(false)

    /** Identities to hide right now. Empty while the switch is OFF. */
    fun hidden(ctx: Context): Set<String> = if (enabled()) list(ctx).toSet() else emptySet()

    fun hideContacts(hidden: Set<String>, rows: List<Contact>): List<Contact> =
        if (hidden.isEmpty()) rows else rows.filter { id(it.number) !in hidden }

    fun hideRecents(hidden: Set<String>, rows: List<Recent>): List<Recent> =
        if (hidden.isEmpty()) rows else rows.filter { id(it.number) !in hidden }
}
