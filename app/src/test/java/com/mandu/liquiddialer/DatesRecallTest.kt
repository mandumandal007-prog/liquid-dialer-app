package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class DatesRecallTest{
 private fun entry(days:String="7")=DatesRecall.create(CallDates.Mode.SHIFT,"2026-11-16","unused",days)
 @Test fun defaultsOff(){assertFalse(LdSettings().datesRecall);assertFalse(LdSettings().datesHistoryFold)}
 @Test fun shiftOriginalNotResult(){val e=entry();assertEquals("2026-11-16",DatesRecall.input(e).first);assertEquals("7",e.input.days);assertTrue(e.result.title.startsWith("2026-11-23"))}
 @Test fun negativeDaysExact(){val e=entry("-7");assertEquals("-7",DatesRecall.input(e).days);assertTrue(e.result.title.startsWith("2026-11-09"))}
 @Test fun zeroDaysExact(){assertEquals("0",DatesRecall.input(entry("0")).days)}
 @Test fun shiftIrrelevantLastCleared(){assertEquals("",entry().input.last)}
 @Test fun gapOriginalBothDates(){val e=DatesRecall.create(CallDates.Mode.GAP,"2026-11-17","2026-11-16","unused");assertEquals("2026-11-17",e.input.first);assertEquals("2026-11-16",e.input.last);assertEquals("7",e.input.days);assertEquals(e.input,DatesRecall.input(e))}
 @Test fun weekdayOriginalBothDates(){val e=DatesRecall.create(CallDates.Mode.WEEKDAYS,"2026-11-16","2026-11-23","7");assertEquals(CallDates.Mode.WEEKDAYS,DatesRecall.input(e).mode);assertEquals("2026-11-23",e.input.last)}
 @Test fun invalidDateRefused(){try{DatesRecall.create(CallDates.Mode.SHIFT,"2026-02-30","","7");fail()}catch(_:IllegalArgumentException){}}
 @Test fun invalidDaysRefused(){try{entry("1.5");fail()}catch(_:IllegalArgumentException){}}
 @Test fun invalidRangeRefused(){try{DatesRecall.create(CallDates.Mode.SHIFT,"2199-12-31","","1");fail()}catch(_:IllegalArgumentException){}}
 @Test fun reversedWeekdayRefused(){try{DatesRecall.create(CallDates.Mode.WEEKDAYS,"2026-11-23","2026-11-16","7");fail()}catch(_:IllegalArgumentException){}}
 @Test fun tamperedResultRefused(){val e=entry().copy(result=CallDates.Result("wrong","wrong"));try{DatesRecall.input(e);fail()}catch(_:IllegalArgumentException){}}
 @Test fun tamperedInputsRefused(){val e=entry();try{DatesRecall.input(e.copy(input=e.input.copy(days="8")));fail()}catch(_:IllegalArgumentException){}}
 @Test fun historyNewestFirst(){val h=DatesRecall.History();h.add(entry("1"));h.add(entry("2"));assertEquals(listOf("2","1"),h.rows.map{it.input.days})}
 @Test fun historyBoundedTen(){val h=DatesRecall.History();repeat(15){h.add(entry(it.toString()))};assertEquals(10,h.rows.size);assertEquals("14",h.rows.first().input.days);assertEquals("5",h.rows.last().input.days)}
 @Test fun historySnapshotAndClear(){val h=DatesRecall.History();h.add(entry());val x=h.rows;h.clear();assertTrue(h.rows.isEmpty());assertEquals(1,x.size)}
 @Test fun invalidAddKeepsHistory(){val h=DatesRecall.History();val e=entry();h.add(e);try{h.add(e.copy(result=CallDates.Result("x","y")));fail()}catch(_:IllegalArgumentException){};assertEquals(listOf(e),h.rows)}
 @Test fun settingsRoutes(){for(q in listOf("reuse date inputs","fold date history"))assertTrue(SettingsFinder.search(q).any{it.route=="calldates"})}
}
