package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
class WeekdayShiftTest {
 private val mf=setOf(1,2,3,4,5)
 private fun c(first:String="2026-11-13",n:String="1",days:Set<Int> =mf,skip:String="")=WeekdayShift.calculate(first,n,days,skip)
 private fun bad(f:()->Unit){try{f();fail("Expected refusal")}catch(_:IllegalArgumentException){}}
 @Test fun defaultOff(){assertFalse(LdSettings().weekdayShift)}
 @Test fun fridayForward(){assertEquals(LocalDate.of(2026,11,16),c().target)}
 @Test fun mondayBackward(){assertEquals(LocalDate.of(2026,11,13),c("2026-11-16","-1").target)}
 @Test fun fiveForward(){assertEquals(LocalDate.of(2026,11,20),c(n="5").target)}
 @Test fun fiveBackward(){assertEquals(LocalDate.of(2026,11,6),c(n="-5").target)}
 @Test fun allWeek(){assertEquals(LocalDate.of(2026,11,14),c(days=(1..7).toSet()).target)}
 @Test fun sundaysOnly(){assertEquals(LocalDate.of(2026,11,22),c(n="2",days=setOf(7)).target)}
 @Test fun customWeekend(){assertEquals(LocalDate.of(2026,11,14),c(days=setOf(6,7)).target)}
 @Test fun startExcludedEvenSelected(){assertEquals(7,c(days=setOf(5)).walked)}
 @Test fun excludedMonday(){assertEquals(LocalDate.of(2026,11,17),c(skip="2026-11-16").target)}
 @Test fun excludedWeekendNotDoubleCounted(){val r=c(skip="2026-11-14");assertEquals(2,r.skippedWeekdays);assertEquals(0,r.skippedExclusions)}
 @Test fun backwardExclusion(){assertEquals(LocalDate.of(2026,11,12),c("2026-11-16","-1",skip="2026-11-13").target)}
 @Test fun exclusionDeduplicates(){assertEquals(1,WeekdayShift.exclusions("2026-11-16, 2026-11-16").size)}
 @Test fun whitespaceAndComma(){assertEquals(3,WeekdayShift.exclusions("2026-11-16,2026-11-17\n2026-11-18").size)}
 @Test fun emptyExclusions(){assertTrue(WeekdayShift.exclusions(" \n ").isEmpty())}
 @Test fun outsideRangeExcludedRefused(){bad{c(skip="2200-01-01")}}
 @Test fun badExclusionRefusedWhole(){bad{c(skip="2026-11-16,no")}}
 @Test fun tooManyExclusions(){bad{c(skip=(1..21).joinToString(","){"2026-11-%02d".format(it)})}}
 @Test fun tooLongExclusions(){bad{c(skip=" ".repeat(501))}}
 @Test fun strictStart(){listOf("2026-02-30","26-11-13","2026-11-13 ","1899-12-31").forEach{bad{c(it)}}}
 @Test fun strictCount(){listOf("0","-0","+1","1.0","1,000"," 1","3661","-3661","99999").forEach{bad{c(n=it)}}}
 @Test fun emptyDays(){bad{c(days=emptySet())}}
 @Test fun invalidDays(){bad{c(days=setOf(0,1))};bad{c(days=setOf(8))}}
 @Test fun lowerBoundary(){bad{c("1900-01-01","-1")}}
 @Test fun upperBoundary(){bad{c("2199-12-31","1")}}
 @Test fun leapDate(){assertEquals(LocalDate.of(2024,2,29),c("2024-02-28","1",(1..7).toSet()).target)}
 @Test fun leapCentury(){assertEquals(LocalDate.of(2000,2,29),c("2000-02-28","1",(1..7).toSet()).target)}
 @Test fun accountingEqualsWalk(){val r=c(n="40",skip="2026-11-16,2026-11-17");assertEquals(40+r.skippedExclusions+r.skippedWeekdays,r.walked)}
 @Test fun boundSupported(){assertEquals(3660,c(n="3660").count);assertEquals(-3660,c(n="-3660").count)}
 @Test fun exclusionsOutsideWalkStillDocumented(){val r=c(skip="2027-01-01");assertEquals(0,r.skippedExclusions);assertEquals(1,r.exclusions.size)}
 @Test fun descriptionClear(){val s=c().description();listOf("Start excluded","target is counted","No official holiday","delivery promise").forEach{assertTrue(s.contains(it))}}
 @Test fun finderRoute(){assertEquals("weekdayshift",SettingsFinder.search("selected weekday shift").first().route)}
 @Test fun inputSetNotMutated(){val ds=mutableSetOf(1,2,3,4,5);val r=c(days=ds);ds.clear();assertEquals(5,r.weekdays.size)}
}
