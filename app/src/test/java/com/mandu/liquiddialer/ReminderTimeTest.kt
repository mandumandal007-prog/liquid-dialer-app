package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.ReminderTime
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone
class ReminderTimeTest {
 @Test fun strictDateTimeAndFutureValidation(){
  val zone=TimeZone.getTimeZone("Asia/Kolkata")
  assertTrue(ReminderTime.parse("2028-02-29","09:30",0,zone)>0)
  for(pair in listOf("2027-02-29" to "09:00","2026-11-31" to "10:00","2027-01-01" to "24:00","2027-01-01" to "12:60","2027-1-1" to "09:00","2027-01-01x" to "09:00")){
   try{ReminderTime.parse(pair.first,pair.second,0,zone);fail("Accepted $pair")}catch(_:IllegalArgumentException){}
  }
  val at=ReminderTime.parse("2027-01-01","09:00",0,zone)
  try{ReminderTime.parse("2027-01-01","09:00",at,zone);fail("Accepted past")}catch(_:IllegalArgumentException){}
 }
 @Test fun localTimeZoneAndDaylightSavingGap(){
  val india=ReminderTime.parse("2027-01-01","09:00",0,TimeZone.getTimeZone("Asia/Kolkata"))
  val utc=ReminderTime.parse("2027-01-01","09:00",0,TimeZone.getTimeZone("UTC"))
  assertEquals(19800000L,utc-india)
  try{ReminderTime.parse("2027-03-14","02:30",0,TimeZone.getTimeZone("America/New_York"));fail("Accepted DST gap")}catch(_:IllegalArgumentException){}
 }
}
