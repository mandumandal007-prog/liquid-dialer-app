package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class WeekdayChoicesTest{
 @Test fun defaultsOff(){assertFalse(LdSettings().weekdayShortcuts);assertFalse(LdSettings().weekdayExclusionsFold)}
 @Test fun weekdaysExact(){assertEquals(setOf(1,2,3,4,5),WeekdayChoices.Shortcut.WEEKDAYS.days)}
 @Test fun allExact(){assertEquals((1..7).toSet(),WeekdayChoices.Shortcut.ALL.days)}
 @Test fun weekendsExact(){assertEquals(setOf(6,7),WeekdayChoices.Shortcut.WEEKENDS.days)}
 @Test fun emptySummary(){assertEquals("No manual exclusions",WeekdayChoices.summary("  "))}
 @Test fun duplicatesUnique(){assertEquals("1 unique manual excluded dates",WeekdayChoices.summary("2026-11-16,2026-11-16"))}
 @Test fun twoSummary(){assertEquals("2 unique manual excluded dates",WeekdayChoices.summary("2026-11-16 2026-11-17"))}
 @Test fun invalidSummary(){assertEquals("Manual exclusions need checking",WeekdayChoices.summary("2026-02-30"))}
 @Test fun tooManySummary(){assertEquals("Manual exclusions need checking",WeekdayChoices.summary(List(21){"2026-11-16"}.joinToString(",")))}
 @Test fun allCountsWeekend(){assertEquals("2026-11-14",WeekdayShift.calculate("2026-11-13","1",WeekdayChoices.Shortcut.ALL.days,"").target.toString())}
 @Test fun weekendBackward(){assertEquals("2026-11-15",WeekdayShift.calculate("2026-11-16","-1",WeekdayChoices.Shortcut.WEEKENDS.days,"").target.toString())}
 @Test fun exclusionsStillApply(){assertEquals("2026-11-15",WeekdayShift.calculate("2026-11-13","1",WeekdayChoices.Shortcut.WEEKENDS.days,"2026-11-14").target.toString())}
 @Test fun finderRoutes(){for(q in listOf("weekday selection shortcuts","fold manual exclusions"))assertTrue(SettingsFinder.search(q).any{it.route=="weekdayshift"})}
}
