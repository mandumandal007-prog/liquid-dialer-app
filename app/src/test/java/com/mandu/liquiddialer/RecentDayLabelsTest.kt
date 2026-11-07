package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.RecentDayLabels
import java.util.Calendar
import org.junit.Test
import org.junit.Assert.*

class RecentDayLabelsTest {
    @Test fun repeatedFiltersReuseLabelsAndPreserveTodayYesterday() {
        val now=Calendar.getInstance().apply{set(2026,10,7,12,0,0);set(Calendar.MILLISECOND,0)}
        val yesterday=(now.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,-1)}
        val labels=RecentDayLabels(now.timeInMillis)
        assertEquals("Aaj",labels.label(now.timeInMillis));assertEquals("Kal",labels.label(yesterday.timeInMillis))
        val rows=(0 until 5000).map{now.timeInMillis-it*60000L}
        repeat(20){rows.forEach{labels.label(it)}}
        assertEquals(5000,labels.computed)
        println("PERF day labels: 100000 row visits -> ${labels.computed} label computations")
    }
}
