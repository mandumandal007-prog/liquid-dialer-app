package com.mandu.liquiddialer.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ReminderTime {
    /** Strict local wall time. Invalid dates and daylight-saving gaps are rejected. */
    fun parse(date:String,time:String,now:Long=System.currentTimeMillis(),zone:TimeZone=TimeZone.getDefault()):Long {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date) && Regex("\\d{2}:\\d{2}").matches(time)) {"Date YYYY-MM-DD aur time HH:mm mein likho"}
        val f=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).apply {isLenient=false;timeZone=zone}
        val at=try {f.parse("$date $time")!!.time}catch(_:Exception){throw IllegalArgumentException("Date ya time sahi nahi hai")}
        require(at>now) {"Aane wala date/time chuno"}
        return at
    }
}
