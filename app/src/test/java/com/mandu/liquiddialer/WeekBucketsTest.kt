package com.mandu.liquiddialer

import android.provider.CallLog
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.Repo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/** weekBuckets: device-tz calendar-day anchoring, type truthfulness (rejected/blocked alag),
 *  coverage/partial honesty. Koi real call log nahi chahiye - pure data tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeekBucketsTest {

    private fun at(dayOffset: Int, hour: Int, min: Int, nowMs: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = nowMs
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_YEAR, -dayOffset)
        c.add(Calendar.HOUR_OF_DAY, hour); c.add(Calendar.MINUTE, min)
        return c.timeInMillis
    }

    private fun rec(id: Long, type: Int, date: Long, dur: Long = 60) =
        Recent(id, "+91 90000 000$id", "N$id", type, date, dur, null, null)

    private val now = System.currentTimeMillis()

    @Test
    fun midnightBoundaryCallsGoToCorrectCalendarDay() {
        // Aaj 00:01 ki call AAJ me, kal 23:59 ki KAL me - rolling-24h window nahi.
        val recents = listOf(
            rec(1, CallLog.Calls.OUTGOING_TYPE, at(0, 0, 1, now)),
            rec(2, CallLog.Calls.INCOMING_TYPE, at(1, 23, 59, now)),
        )
        val b = Repo.weekBuckets(recents, now, 300)
        assertEquals(1, b[6].outgoing)   // aaj
        assertEquals(1, b[5].incoming)   // kal
        assertEquals(0, b[6].incoming)
    }

    @Test
    fun rejectedAndBlockedAreNotCountedAsMissed() {
        val recents = listOf(
            rec(1, CallLog.Calls.MISSED_TYPE, at(0, 10, 0, now)),
            rec(2, CallLog.Calls.REJECTED_TYPE, at(0, 11, 0, now)),
            rec(3, CallLog.Calls.BLOCKED_TYPE, at(0, 12, 0, now)),
        )
        val t = Repo.weekBuckets(recents, now, 300)[6]
        assertEquals(1, t.missed)
        assertEquals(1, t.rejected)
        assertEquals(1, t.blocked)
        assertEquals(3, t.total)
        // talk sirf answered (incoming+outgoing) ka
        assertEquals(0L, t.talkSecs)
    }

    @Test
    fun daysOutsideLoadedWindowAreNotCoveredAndNotZero() {
        // Sirf aaj ki 1 entry - purane 6 din window ke bahar: covered=false (unhe 0 nahi kehna)
        val recents = listOf(rec(1, CallLog.Calls.INCOMING_TYPE, at(0, 9, 0, now)))
        val b = Repo.weekBuckets(recents, now, 300)
        assertTrue(b[6].covered)
        (0..5).forEach { assertFalse("day -${6-it} covered nahi honi chahiye", b[it].covered) }
    }

    @Test
    fun fullCoverageInsideWindowMeansTrueZero() {
        // Oldest entry 5 din purani: us din se aaj tak covered - wahan 0 entries = sach me 0 calls.
        // 6 din purana din window ke bahar hai (oldest 5-din-purani entry us din ke END ke baad hai).
        val recents = listOf(
            rec(1, CallLog.Calls.OUTGOING_TYPE, at(5, 9, 0, now)),
            rec(2, CallLog.Calls.INCOMING_TYPE, at(0, 9, 0, now)),
        )
        val b = Repo.weekBuckets(recents, now, 300)
        assertFalse("6 din purana din window ke bahar", b[0].covered)
        (1..6).forEach { assertTrue("day -${6-it} covered honi chahiye", b[it].covered) }
        assertEquals(0, b[3].total)  // covered din, koi entry nahi = sach me 0
        assertFalse(b[1].partial)    // limit nahi lagi -> oldest din bhi partial nahi
    }

    @Test
    fun oldestDayIsPartialOnlyWhenLimitHit() {
        val recents = (1..300).map { i ->
            rec(i.toLong(), CallLog.Calls.INCOMING_TYPE, at(3, 10, 0, now) + i * 60_000L)
        }
        val b = Repo.weekBuckets(recents, now, 300)
        assertTrue("limit hit + oldest us din ke beech -> partial", b[3].partial)
        val b2 = Repo.weekBuckets(recents.take(50), now, 300)
        assertFalse("limit nahi lagi -> partial nahi", b2[3].partial)
    }

    @Test
    fun emptyRecentsAllUncovered() {
        val b = Repo.weekBuckets(emptyList(), now, 300)
        assertEquals(7, b.size)
        b.forEach { assertFalse(it.covered); assertFalse(it.partial); assertEquals(0, it.total) }
    }

    @Test
    fun voicemailAndAnsweredElsewhereGoToOther() {
        val recents = listOf(
            rec(1, CallLog.Calls.VOICEMAIL_TYPE, at(0, 8, 0, now)),
            rec(2, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE, at(0, 9, 0, now)),
        )
        val t = Repo.weekBuckets(recents, now, 300)[6]
        assertEquals(2, t.other)
        assertEquals(0, t.missed)
    }
}
