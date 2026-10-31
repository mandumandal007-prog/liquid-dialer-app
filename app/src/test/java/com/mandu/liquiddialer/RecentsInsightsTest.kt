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

    private fun r(id: Long, type: Int, ageMs: Long, now: Long, dur: Long = 0) =
        Recent(id, "+91 90000 0000$id", "Zed $id", type, now - ageMs, dur, null, null)

    // Ancora em 13:00 do dia corrente: offsets de segundos/minutos continuam "aaj" e
    // 2 dias atras cai em outro dia, em qualquer horario que o teste rodar (sem flake de meia-noite).
    private fun anchoredNow(): Long {
        val zone = java.time.ZoneId.systemDefault()
        return java.time.LocalDate.now(zone).atTime(13, 0).atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun dayStatsMath() {
        val now = anchoredNow()
        val list = listOf(
            r(1, CallLog.Calls.OUTGOING_TYPE, 60_000, now, 245),   // aaj, 245s
            r(2, CallLog.Calls.INCOMING_TYPE, 120_000, now, 55),   // aaj, 55s
            r(3, CallLog.Calls.MISSED_TYPE, 180_000, now),         // aaj missed
            r(4, CallLog.Calls.OUTGOING_TYPE, 2 * 86_400_000L, now, 999) // parso - count nahi
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
        val now = anchoredNow()
        val list = listOf(
            r(1, CallLog.Calls.MISSED_TYPE, 60_000, now),          // naya missed
            r(2, CallLog.Calls.MISSED_TYPE, 3 * 86_400_000L, now), // purana missed
            r(3, CallLog.Calls.REJECTED_TYPE, 30_000, now),        // user ne reject kiya = missed NAHI
            r(4, CallLog.Calls.OUTGOING_TYPE, 10_000, now)         // outgoing badge nahi
        )
        // seenAt 5 min pehle: sirf naya sacha missed (1); rejected count nahi hota
        assertEquals(1, Repo.unseenMissed(list, now - 300_000))
        // sab seen: 0
        assertEquals(0, Repo.unseenMissed(list, now))
        // kabhi nahi dekha: dono missed (rejected/outgoing nahi)
        assertEquals(2, Repo.unseenMissed(list, 0L))
    }
}
