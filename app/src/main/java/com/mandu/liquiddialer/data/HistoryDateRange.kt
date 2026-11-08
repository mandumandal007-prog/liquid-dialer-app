package com.mandu.liquiddialer.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Inclusive calendar dates, including today. Fills fields only, never queries a provider. */
object HistoryDateRange {
 data class Range(val from:String,val to:String)
 fun recent(days:Int,now:Long=System.currentTimeMillis(),zone:TimeZone=TimeZone.getDefault()):Range {
  require(days in listOf(1,7,30))
  val end=Calendar.getInstance(zone).apply {timeInMillis=now}
  val start=(end.clone() as Calendar).apply {add(Calendar.DAY_OF_YEAR,1-days)}
  val format=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply {timeZone=zone}
  return Range(format.format(start.time),format.format(end.time))
 }
}
