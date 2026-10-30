package com.mandu.liquiddialer

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.telecom.CallNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Call-summary notification: sahi channel, lock-screen privacy (VISIBILITY_PRIVATE + generic public
 *  version), naam+duration content me. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallSummaryNotifTest {

    @Test
    fun summaryNotificationIsPrivateAndWellFormed() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallNotifier.createChannels(ctx)
        CallNotifier.summary(ctx, "Rahul Sharma", "01:23")
        val nm = Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java))
        val list = nm.allNotifications
        assertTrue("summary notification post honi chahiye", list.isNotEmpty())
        val n = list.last()
        assertEquals(CallNotifier.CH_SUMMARY, n.channelId)
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, n.visibility)
        assertTrue(n.extras.getCharSequence("android.title").toString().contains("Rahul Sharma"))
        assertTrue(n.extras.getCharSequence("android.text").toString().contains("01:23"))
        // public version generic: usme naam nahi
        val pub = n.publicVersion
        assertTrue(pub != null && !pub.extras.getCharSequence("android.title").toString().contains("Rahul"))
        // channel bhi private
        val ch = ctx.getSystemService(NotificationManager::class.java).getNotificationChannel(CallNotifier.CH_SUMMARY)
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, ch.lockscreenVisibility)
    }
}
