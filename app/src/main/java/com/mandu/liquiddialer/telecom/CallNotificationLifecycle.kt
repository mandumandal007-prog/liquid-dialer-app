package com.mandu.liquiddialer.telecom

import android.app.Notification
import android.util.Log

/** Owns one real call notification. No loops, restarts, alarms or dismiss interception. */
internal class CallNotificationLifecycle(
    private val promote: (Notification) -> Unit,
    private val demote: () -> Unit
) {
    private var ownsForeground = false
    private var rejected = false

    /** False leaves the caller free to post the existing ordinary row. */
    fun publish(notification: Notification): Boolean {
        if (rejected) return false
        return try {
            promote(notification)
            ownsForeground = true
            true
        } catch (e: RuntimeException) {
            rejected = true // Do not retry a rejected promotion on every audio/details callback.
            Log.w("CallNotifier", "Call foreground ownership unavailable; keeping ordinary row", e)
            false
        }
    }

    fun end() {
        if (ownsForeground) {
            try { demote() } catch (e: RuntimeException) {
                Log.w("CallNotifier", "Call foreground cleanup rejected", e)
            }
        }
        ownsForeground = false
        rejected = false
    }
}
