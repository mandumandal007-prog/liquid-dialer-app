package com.mandu.liquiddialer

import android.provider.CallLog
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.Repo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recents insights: dayStats (aaj ka strip) + unseenMissed (tab badge) ka exact math. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentsInsightsTest {

    private fun r(id: Long, type: Int, ageMs: Long, dur: Long = 0) =
        Recent(id, "+91 90000 0000$id", "Zed $id", type, System.currentTimeMillis() - ageMs, dur, null, null)

    @Test
    fun dayStatsMath() {
        val now = System.currentTimeMillis()
        val list = listOf(
            r(1, CallLog.Calls.OUTGOING_TYPE, 60_000, 245),   // aaj, 245s
            r(2, CallLog.Calls.INCOMING_TYPE, 120_000, 55),   // aaj, 55s
            r(3, CallLog.Calls.MISSED_TYPE, 180_000),         // aaj missed
            r(4, CallLog.Calls.OUTGOING_TYPE, 2 * 86_400_000L, 999) // parso - count nahi
        )
        val st = Repo.dayStats(list, now)
        assertEquals(3, st.calls)
        assertEquals(300, st.talkSecs)
        assertEquals(1, st.missed)
        // khali list = sab zero (strip hidden)
        assertEquals(Repo.DayStats(0, 0, 0), Repo.dayStats(emptyList(), now))
    }

    @Test
    fun unseenMissedMath() {
        val now = System.currentTimeMillis()
        val list = listOf(
            r(1, CallLog.Calls.MISSED_TYPE, 60_000),          // naya missed
            r(2, CallLog.Calls.MISSED_TYPE, 3 * 86_400_000L), // purana missed
            r(3, CallLog.Calls.REJECTED_TYPE, 30_000),        // user ne reject kiya = missed NAHI
            r(4, CallLog.Calls.OUTGOING_TYPE, 10_000)         // outgoing badge nahi
        )
        // seenAt 5 min pehle: sirf naya sacha missed (1); rejected count nahi hota
        assertEquals(1, Repo.unseenMissed(list, now - 300_000))
        // sab seen: 0
        assertEquals(0, Repo.unseenMissed(list, now))
        // kabhi nahi dekha: dono missed (rejected/outgoing nahi)
        assertEquals(2, Repo.unseenMissed(list, 0L))
    }
}
