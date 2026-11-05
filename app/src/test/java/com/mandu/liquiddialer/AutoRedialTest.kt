package com.mandu.liquiddialer

import android.telecom.DisconnectCause
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.telecom.AutoRedial
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AutoRedial state machine: start, busy retry, cap, answered, cancel, wrong-number ignore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoRedialTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()
    private val dialed = mutableListOf<String>()
    private var pendingTick: (() -> Unit)? = null

    @Before
    fun setUp() {
        dialed.clear(); pendingTick = null
        AutoRedial.session.value = null
        AutoRedial.placeCall = { _, num -> dialed.add(num); true }
        AutoRedial.now = { 100_000L }
        AutoRedial.scheduler = { _, tick -> pendingTick = tick }
    }

    @After
    fun tearDown() {
        AutoRedial.session.value = null
        AutoRedial.placeCall = { c, n -> com.mandu.liquiddialer.telecom.CallManager.place(c, n, null) }
        AutoRedial.now = { System.currentTimeMillis() }
        AutoRedial.scheduler = null
    }

    @Test
    fun shouldRetryOnlyUnansweredBusyOrError() {
        assertTrue(AutoRedial.shouldRetry(false, DisconnectCause.BUSY))
        assertTrue(AutoRedial.shouldRetry(false, DisconnectCause.ERROR))
        assertFalse(AutoRedial.shouldRetry(true, DisconnectCause.BUSY))
        assertFalse(AutoRedial.shouldRetry(false, DisconnectCause.CANCELED))
        assertFalse(AutoRedial.shouldRetry(false, null))
    }

    @Test
    fun startDialsImmediately() {
        AutoRedial.start(ctx, "+91 98765 43210", "Rahul")
        assertEquals(listOf("+91 98765 43210"), dialed)
        assertEquals(1, AutoRedial.session.value?.attempt)
        assertEquals(0L, AutoRedial.session.value?.waitingUntil)
    }

    @Test
    fun busySchedulesNextAttemptAndTickDials() {
        AutoRedial.start(ctx, "9876543210", "Rahul")
        AutoRedial.onCallRemoved(ctx, "+91 98765 43210", answered = false, cause = DisconnectCause.BUSY)
        val s = AutoRedial.session.value!!
        assertEquals(2, s.attempt)
        assertEquals(100_000L + AutoRedial.GAP_SEC * 1000L, s.waitingUntil)
        assertEquals(1, dialed.size) // abhi dobara dial NAHI - countdown ke baad
        pendingTick!!.invoke()
        assertEquals(2, dialed.size)
        assertEquals(0L, AutoRedial.session.value?.waitingUntil)
    }

    @Test
    fun capStopsAfterMaxAttempts() {
        AutoRedial.start(ctx, "9876543210", "Rahul")
        repeat(AutoRedial.MAX_ATTEMPTS - 1) {
            AutoRedial.onCallRemoved(ctx, "9876543210", false, DisconnectCause.BUSY)
            pendingTick!!.invoke(); pendingTick = null
        }
        assertEquals(AutoRedial.MAX_ATTEMPTS, dialed.size)
        AutoRedial.onCallRemoved(ctx, "9876543210", false, DisconnectCause.BUSY)
        assertNull(AutoRedial.session.value) // cap ke baad session khatam
        assertEquals(AutoRedial.MAX_ATTEMPTS, dialed.size) // koi extra dial nahi
    }

    @Test
    fun answeredEndsSession() {
        AutoRedial.start(ctx, "9876543210", "Rahul")
        AutoRedial.onCallRemoved(ctx, "9876543210", answered = true, cause = DisconnectCause.LOCAL)
        assertNull(AutoRedial.session.value)
        assertEquals(1, dialed.size)
    }

    @Test
    fun cancelStopsPendingRetry() {
        AutoRedial.start(ctx, "9876543210", "Rahul")
        AutoRedial.onCallRemoved(ctx, "9876543210", false, DisconnectCause.BUSY)
        val tick = pendingTick!!
        AutoRedial.cancel(ctx)
        assertNull(AutoRedial.session.value)
        tick.invoke() // stale tick safe: kuch nahi karega
        assertEquals(1, dialed.size)
    }

    @Test
    fun otherNumberCallEndIgnored() {
        AutoRedial.start(ctx, "9876543210", "Rahul")
        AutoRedial.onCallRemoved(ctx, "+91 11111 22222", false, DisconnectCause.BUSY)
        assertEquals(1, AutoRedial.session.value?.attempt) // session waisa ka waisa
        assertNull(pendingTick)
    }
}
