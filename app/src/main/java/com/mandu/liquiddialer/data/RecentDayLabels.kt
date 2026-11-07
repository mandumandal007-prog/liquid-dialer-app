package com.mandu.liquiddialer.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Per-screen/day formatter cache. Search/filter reuse labels instead of allocating calendars per row. */
class RecentDayLabels(now: Long = System.currentTimeMillis()) {
    private val today = Calendar.getInstance().apply { timeInMillis=now }
    private val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR,-1) }
    private val scratch = Calendar.getInstance()
    private val formatter = SimpleDateFormat("d MMM",Locale.getDefault())
    private val cache = HashMap<Long,String>()
    internal var computed = 0
        private set
    fun label(ts:Long):String = cache.getOrPut(ts) {
        computed++
        scratch.timeInMillis=ts
        fun same(other:Calendar)=scratch.get(Calendar.YEAR)==other.get(Calendar.YEAR)&&scratch.get(Calendar.DAY_OF_YEAR)==other.get(Calendar.DAY_OF_YEAR)
        when {same(today)->"Aaj";same(yesterday)->"Kal";else->formatter.format(Date(ts))}
    }
}
