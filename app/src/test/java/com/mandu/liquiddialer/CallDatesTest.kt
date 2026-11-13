package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.CallDates as D
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.DayOfWeek
class CallDatesTest {
 @Test fun strictIsoDate(){assertEquals(LocalDate.of(2026,11,13),D.date("2026-11-13"));for(s in listOf("13-11-2026","2026-1-01"," 2026-01-01","2026-01-01 ","2026/01/01","２０２６-01-01"))assertTrue(s,runCatching{D.date(s)}.isFailure)}
 @Test fun invalidCalendarDatesRejected(){for(s in listOf("2025-02-29","2026-04-31","2026-13-01","2026-00-01","2026-01-00","2026-02-30"))assertTrue(runCatching{D.date(s)}.isFailure)}
 @Test fun leapYearCenturyRules(){assertEquals(29,D.date("2000-02-29").dayOfMonth);assertTrue(runCatching{D.date("2100-02-29")}.isFailure);assertTrue(runCatching{D.date("1900-02-29")}.isFailure)}
 @Test fun supportedRangeStrict(){assertEquals(1900,D.date("1900-01-01").year);assertEquals(2199,D.date("2199-12-31").year);for(s in listOf("1899-12-31","2200-01-01","0000-01-01"))assertTrue(runCatching{D.date(s)}.isFailure)}
 @Test fun positiveShiftWeekdayComputed(){assertEquals("2026-11-20 · Friday",D.shift("2026-11-13","7").title);assertEquals("2026-11-14 · Saturday",D.shift("2026-11-13","1").title)}
 @Test fun negativeShiftAndMonthBoundary(){assertEquals("2026-10-31 · Saturday",D.shift("2026-11-01","-1").title);assertEquals("2025-12-31 · Wednesday",D.shift("2026-01-01","-1").title)}
 @Test fun leapDayShift(){assertEquals("2024-02-29 · Thursday",D.shift("2024-02-28","1").title);assertEquals("2024-03-01 · Friday",D.shift("2024-02-28","2").title)}
 @Test fun zeroAndNegativeZeroSameDate(){assertEquals(D.shift("2026-11-13","0"),D.shift("2026-11-13","-0"));assertTrue(D.shift("2026-11-13","0").detail.contains("day 0"))}
 @Test fun dayInputStrictAndBounded(){for(s in listOf("","+7","7.5","1,000"," 7","36501","-36501","999999999999"))assertTrue(s,runCatching{D.shift("2026-11-13",s)}.isFailure);assertTrue(D.shift("2000-01-01","36500").title.startsWith("2099-12-07"))}
 @Test fun shiftOutOfSupportedRangeRejected(){assertTrue(runCatching{D.shift("1900-01-01","-1")}.isFailure);assertTrue(runCatching{D.shift("2199-12-31","1")}.isFailure)}
 @Test fun gapSignedAndSameDate(){assertEquals("7 calendar days",D.gap("2026-11-13","2026-11-20").title);assertEquals("-7 calendar days",D.gap("2026-11-20","2026-11-13").title);assertEquals("0 calendar days",D.gap("2026-11-13","2026-11-13").title)}
 @Test fun gapLeapAndYearBoundary(){assertEquals("2 calendar days",D.gap("2024-02-28","2024-03-01").title);assertEquals("1 calendar days",D.gap("2026-12-31","2027-01-01").title)}
 @Test fun weekdayRangeIsStartIncludedEndExcluded(){assertEquals("1 Monday-Friday dates",D.weekdays("2026-11-13","2026-11-16").title);assertEquals("0 Monday-Friday dates",D.weekdays("2026-11-14","2026-11-16").title);assertEquals("1 Monday-Friday dates",D.weekdays("2026-11-16","2026-11-17").title)}
 @Test fun weekdaySameDateAndReversed(){assertEquals("0 Monday-Friday dates",D.weekdays("2026-11-13","2026-11-13").title);assertTrue(runCatching{D.weekdays("2026-11-20","2026-11-13")}.isFailure)}
 @Test fun weekdayWholeWeeks(){assertEquals("5 Monday-Friday dates",D.weekdays("2026-11-13","2026-11-20").title);assertEquals("10 Monday-Friday dates",D.weekdays("2026-11-14","2026-11-28").title)}
 @Test fun holidayNotSilentlySkipped(){assertEquals("1 Monday-Friday dates",D.weekdays("2026-12-25","2026-12-26").title);assertTrue(D.weekdays("2026-12-25","2026-12-26").detail.contains("holidays NOT excluded"))}
 @Test fun optimizedWeekdaysMatchesBruteForceAcrossWeekdayAndLeapEdges(){for(start in listOf("2026-11-09","2024-02-25","2099-12-25")){val a=D.date(start);for(offset in 0..30)for(length in 0..35){val first=a.plusDays(offset.toLong());val end=first.plusDays(length.toLong());val expected=(0 until length).count{first.plusDays(it.toLong()).dayOfWeek !in listOf(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY)};assertEquals("$expected Monday-Friday dates",D.weekdays(first.toString(),end.toString()).title)}}}
 @Test fun historyBoundedNewestFirstAndCleared(){val h=D.History();repeat(13){h.add(D.shift("2026-11-13",it.toString()))};assertEquals(10,h.rows.size);assertEquals(D.shift("2026-11-13","12"),h.rows.first());assertEquals(D.shift("2026-11-13","3"),h.rows.last());val copy=h.rows;h.clear();assertTrue(h.rows.isEmpty());assertEquals(10,copy.size)}
 @Test fun calculationDispatchDoesNotParseUnusedField(){assertEquals(D.shift("2026-11-13","7"),D.calculate(D.Mode.SHIFT,"2026-11-13","invalid","7"));assertEquals(D.gap("2026-11-13","2026-11-20"),D.calculate(D.Mode.GAP,"2026-11-13","2026-11-20","invalid"))}
 @Test fun labelLocaleIndependent(){val old=java.util.Locale.getDefault();try{java.util.Locale.setDefault(java.util.Locale("tr","TR"));assertEquals("2026-11-13 · Friday",D.label(LocalDate.of(2026,11,13)))}finally{java.util.Locale.setDefault(old)}}
}
