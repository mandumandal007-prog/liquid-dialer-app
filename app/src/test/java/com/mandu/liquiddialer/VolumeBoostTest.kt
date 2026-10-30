package com.mandu.liquiddialer

import android.telecom.Call
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.media.VolumeBoost
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VolumeBoostTest {

    @Test
    fun gainMapsPercentToCappedMillibels() {
        assertEquals(0, VolumeBoost.gainToMb(0))
        assertEquals(375, VolumeBoost.gainToMb(25))
        assertEquals(1500, VolumeBoost.gainToMb(100))
        assertEquals(0, VolumeBoost.gainToMb(-5))     // low clamp
        assertEquals(1500, VolumeBoost.gainToMb(250)) // high clamp: +15 dB hard cap, kabhi auto-max nahi
    }

    @Test
    fun dbLabelShowsCappedGain() {
        assertEquals("+0 dB", VolumeBoost.dbLabel(0))
        assertEquals("+3 dB", VolumeBoost.dbLabel(25))
        assertEquals("+15 dB", VolumeBoost.dbLabel(100))
    }

    @Test
    fun boostDefaultsOffAndRoundTrips() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<LdApp>()
        assertFalse("boost must default OFF", app.repo.flow.first().volBoost)
        app.ldStore.edit { it[K.volBoost] = true; it[K.volBoostGain] = 65 }
        val s = app.repo.flow.first()
        assertTrue(s.volBoost)
        assertEquals(65, s.volBoostGain)
        app.ldStore.edit { it.remove(K.volBoost); it.remove(K.volBoostGain) }
        Unit
    }

    @Test
    fun conferenceControlsFollowReportedCapabilities() {
        val parent = FakeCall.make("+91 90000 00000", "Conference", Call.STATE_ACTIVE,
            System.currentTimeMillis(), props = Call.Details.PROPERTY_CONFERENCE)
        val capable = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_ACTIVE, System.currentTimeMillis(),
            caps = Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE)
        val plain = FakeCall.make("+91 91234 56789", "Papa", Call.STATE_ACTIVE, System.currentTimeMillis())
        FakeCall.setChildren(parent, listOf(capable, plain))
        val solo = FakeCall.make("+91 90000 11111", "Solo", Call.STATE_ACTIVE, System.currentTimeMillis())

        assertTrue(CallManager.isConference(parent))
        assertFalse(CallManager.isConference(solo))
        assertEquals(2, CallManager.conferenceChildren(parent).size)
        assertTrue(CallManager.canDisconnectChild(capable))
        assertFalse(CallManager.canDisconnectChild(plain))
        assertFalse(CallManager.canSeparateChild(capable))
        // unsupported actions must never throw or pretend to work
        CallManager.disconnectChild(plain)
        CallManager.separateChild(plain)
        // conference children kabhi "doosri call" swap card me nahi aane chahiye
        CallManager.calls.value = listOf(parent, capable, plain)
        assertTrue(CallManager.others().isEmpty())
        CallManager.calls.value = emptyList()
        CallManager.bump()
    }
}
