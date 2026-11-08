package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.HistoryDateRange
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone
class HistoryDateRangeTest {
 private fun range(days:Int,at:String,zone:String="Asia/Kolkata")=HistoryDateRange.recent(days,Instant.parse(at).toEpochMilli(),TimeZone.getTimeZone(zone))
 @Test fun inclusiveTodaySevenAndThirtyDays(){
  assertEquals(HistoryDateRange.Range("2026-11-08","2026-11-08"),range(1,"2026-11-08T15:00:00Z"))
  assertEquals(HistoryDateRange.Range("2026-11-02","2026-11-08"),range(7,"2026-11-08T15:00:00Z"))
  assertEquals(HistoryDateRange.Range("2026-10-10","2026-11-08"),range(30,"2026-11-08T15:00:00Z"))
 }
 @Test fun leapDayAndYearBoundary(){
  assertEquals(HistoryDateRange.Range("2024-02-24","2024-03-01"),range(7,"2024-03-01T12:00:00Z"))
  assertEquals(HistoryDateRange.Range("2025-12-27","2026-01-02"),range(7,"2026-01-02T12:00:00Z"))
 }
 @Test fun localMidnightNotUtcDeterminesToday(){
  assertEquals(HistoryDateRange.Range("2026-11-09","2026-11-09"),range(1,"2026-11-08T20:00:00Z"))
  assertEquals(HistoryDateRange.Range("2026-11-08","2026-11-08"),range(1,"2026-11-08T20:00:00Z","America/Los_Angeles"))
 }
 @Test fun dstUsesCalendarDaysNotFixedMilliseconds(){
  assertEquals(HistoryDateRange.Range("2026-03-03","2026-03-09"),range(7,"2026-03-09T07:30:00Z","America/Los_Angeles"))
  assertEquals(HistoryDateRange.Range("2026-10-27","2026-11-02"),range(7,"2026-11-02T08:30:00Z","America/Los_Angeles"))
 }
 @Test fun invalidPresetRejected(){
  for(n in listOf(0,-1,2,31,Int.MAX_VALUE))try{range(n,"2026-11-08T15:00:00Z");fail()}catch(_:IllegalArgumentException){}
 }
}
