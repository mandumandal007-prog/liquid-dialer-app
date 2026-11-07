package com.mandu.liquiddialer

import android.content.ContentResolver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ProviderReloadsTest {
    private lateinit var resolver: ContentResolver
    private lateinit var reloads: ProviderReloads
    private val batches = mutableListOf<Boolean>()
    @Before fun setup() {
        resolver = ApplicationProvider.getApplicationContext<LdApp>().contentResolver
        reloads = ProviderReloads(Handler(Looper.getMainLooper())) { batches += it }
        reloads.register(resolver)
    }
    @After fun cleanup() { reloads.close(resolver) }
    private fun callChange() = resolver.notifyChange(CallLog.Calls.CONTENT_URI, null)
    private fun contactChange() = resolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test fun oneHundredCallLogNotificationsRequestOneRecentBatchAndNoContacts() {
        repeat(100) { callChange() }
        advance(179); assertTrue(batches.isEmpty())
        advance(1); assertEquals(listOf(false), batches)
    }
    @Test fun contactEditRefreshesContactsAndRecentNamesTogether() {
        contactChange(); advance(180)
        assertEquals(listOf(true), batches)
    }
    @Test fun mixedBurstDoesNotLoseContactChangeWhenCallLogArrivesLast() {
        contactChange(); advance(100); repeat(10) { callChange() }
        advance(179); assertTrue(batches.isEmpty())
        advance(1); assertEquals(listOf(true), batches)
    }
    @Test fun nextCallOnlyBurstDoesNotRetainOldContactFlag() {
        contactChange(); advance(180)
        callChange(); advance(180)
        assertEquals(listOf(true, false), batches)
    }
    @Test fun childUrisAreObservedAndCallBurstCanBeUpgradedByContactEdit() {
        resolver.notifyChange(CallLog.Calls.CONTENT_URI.buildUpon().appendPath("42").build(), null)
        resolver.notifyChange(ContactsContract.Contacts.CONTENT_URI.buildUpon().appendPath("7").build(), null)
        advance(180); assertEquals(listOf(true), batches)
    }
    @Test fun destructionCancelsPendingAndUnregistersBothSources() {
        contactChange(); callChange(); reloads.close(resolver)
        advance(180); assertTrue(batches.isEmpty())
        contactChange(); callChange(); advance(180); assertTrue(batches.isEmpty())
    }
}
