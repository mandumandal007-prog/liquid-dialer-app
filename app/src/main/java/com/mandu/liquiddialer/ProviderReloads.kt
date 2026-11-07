package com.mandu.liquiddialer

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog
import android.provider.ContactsContract

/** Main-thread owned. Merge bursts, but preserve which provider actually changed.
 * Call-log writes never require rescanning contacts, SIMs or the blocked-number provider.
 * Contact edits also refresh Recents because their displayed names can change. */
internal class ProviderReloads(
    private val handler: Handler,
    private val reload: (contactsChanged: Boolean) -> Unit,
) {
    private var contactsPending = false
    private var closed = false
    private val dispatch = Runnable {
        val contacts = contactsPending
        contactsPending = false
        if (!closed) reload(contacts)
    }
    private fun changed(contacts: Boolean) {
        if (closed) return
        contactsPending = contactsPending || contacts
        handler.removeCallbacks(dispatch)
        handler.postDelayed(dispatch, 180)
    }
    private val calls = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = changed(false)
    }
    private val contacts = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = changed(true)
    }
    fun register(resolver: ContentResolver) {
        try { resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, calls) } catch (_: Exception) { }
        try { resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contacts) } catch (_: Exception) { }
    }
    fun close(resolver: ContentResolver) {
        closed = true
        contactsPending = false
        handler.removeCallbacks(dispatch)
        try { resolver.unregisterContentObserver(calls) } catch (_: Exception) { }
        try { resolver.unregisterContentObserver(contacts) } catch (_: Exception) { }
    }
}
