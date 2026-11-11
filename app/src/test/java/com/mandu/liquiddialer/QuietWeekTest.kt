package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.QuietHours
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class QuietWeekTest{
 private fun w(days:Set<Int> =setOf(0),start:Int=1320,end:Int=420)=QuietWeek.Window("one","Night",days,start,end)
 private fun s(xs:List<QuietWeek.Window> =listOf(w()))=LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules=QuietWeek.encode(xs))
 @Test fun mondayNightBelongsToMondayAndTuesdayMorning(){val x=w();assertTrue(x.matches(0,1320));assertTrue(x.matches(1,419));assertFalse(x.matches(1,420));assertFalse(x.matches(0,419));assertFalse(x.matches(1,1320))}
 @Test fun sundayRollsIntoMonday(){val x=w(setOf(6));assertTrue(x.matches(0,0));assertTrue(x.matches(6,1439));assertFalse(x.matches(6,0))}
 @Test fun sameDayStartIncludedEndExcluded(){val x=w(setOf(2),540,1020);assertTrue(x.matches(2,540));assertFalse(x.matches(2,1020));assertFalse(x.matches(3,600))}
 @Test fun disabledWindowAndInvalidClockNeverMatch(){assertFalse(w().copy(enabled=false).matches(0,1400));assertFalse(w().matches(-1,1400));assertFalse(w().matches(0,1440))}
 @Test fun windowsUnionAndOverlap(){val raw=QuietWeek.encode(listOf(w(),w(setOf(1),300,500).copy(id="two")));assertTrue(QuietWeek.active(raw,1,450));assertFalse(QuietWeek.active(raw,3,450))}
 @Test fun strictRoundtrip(){val xs=listOf(w(),w(setOf(1,4),100,200).copy(id="two",enabled=false));assertEquals(xs,QuietWeek.decode(QuietWeek.encode(xs)))}
 @Test fun corruptAndTrailingDataFailOpen(){val raw=QuietWeek.encode(listOf(w()));listOf("broken",raw+"{}",raw+"\u0000",raw.replace("1320","1440"),raw.replace("true","\"true\"")).forEach{assertFalse(QuietWeek.active(it,0,1400));assertThrows(Exception::class.java){QuietWeek.decode(it)}}}
 @Test fun rejectZeroDaysEqualTimesAndInvalidNames(){listOf(w(emptySet()),w(start=420,end=420),w(setOf(7)),w().copy(name="\n"),w().copy(id="../x")).forEach{assertThrows(IllegalArgumentException::class.java){it.validate()}}}
 @Test fun limitsAndDuplicateIds(){assertThrows(IllegalArgumentException::class.java){QuietWeek.encode(List(9){w().copy(id="w$it")})};assertThrows(IllegalArgumentException::class.java){QuietWeek.encode(listOf(w(),w()))}}
 @Test fun timeInputStrict(){assertEquals(0,QuietWeek.time("00:00"));assertEquals(1439,QuietWeek.time("23:59"));listOf("24:00","1:00","12:60","-1:00"," 22:00").forEach{assertThrows(IllegalArgumentException::class.java){QuietWeek.time(it)}}}
 @Test fun previewIncludesPreviousDayAndEndBoundary(){val xs=listOf(w(setOf(6)));assertEquals(listOf("00:00-07:00 · Night (pichle din se)"),QuietWeek.segments(xs,0));assertEquals(listOf("22:00-24:00 · Night"),QuietWeek.segments(xs,6));assertTrue(QuietWeek.segments(xs,3).isEmpty())}
 @Test fun masterSdkDirectionAndExceptionsPreserved(){val s=s();assertTrue(QuietHours.forSettings(s,34,true,0,1400,false));assertFalse(QuietHours.forSettings(s.copy(quietHours=false),34,true,0,1400,false));assertFalse(QuietHours.forSettings(s,28,true,0,1400,false));assertFalse(QuietHours.forSettings(s,34,false,0,1400,false));assertFalse(QuietHours.forSettings(s,34,true,0,1400,true));assertFalse(QuietHours.forSettings(s,34,true,0,1400,false,blocked=true));assertFalse(QuietHours.forSettings(s,34,true,0,1400,false,favouriteUnchecked=true));assertFalse(QuietHours.forSettings(s,34,true,0,1400,false,contactBypass=true));assertTrue(QuietHours.forSettings(s.copy(quietAllowFav=false),34,true,0,1400,true))}
 @Test fun legacyDailyTimeWhenWeeklyOff(){val s=s().copy(quietWeekMode=false);assertTrue(QuietHours.forSettings(s,34,true,3,1400,false));assertFalse(QuietHours.forSettings(s,34,true,3,700,false));assertFalse(QuietHours.forSettings(s(),34,true,3,1400,false))}
 @Test fun emptyOrCorruptWeeklyDoesNotFallbackToDaily(){assertFalse(QuietHours.forSettings(s(emptyList()),34,true,0,1400,false));assertFalse(QuietHours.forSettings(s().copy(quietWeekRules="broken"),34,true,0,1400,false))}
 @Test fun saveAtomicAndDoesNotEnableMaster(){val p=mutablePreferencesOf();p[K.quietAllowFav]=false;QuietWeek.save(p,"",false,listOf(w()),true);assertTrue(p[K.quietWeekMode]!!);assertFalse(p.toSettings().quietHours);assertFalse(p.toSettings().quietAllowFav);assertEquals(listOf(w()),QuietWeek.decode(p[K.quietWeekRules]!!))}
 @Test fun staleAndInvalidSaveLeaveDataUntouched(){val p=mutablePreferencesOf();QuietWeek.save(p,"",false,listOf(w()),false);val before=p.toPreferences();assertThrows(IllegalArgumentException::class.java){QuietWeek.save(p,"",false,listOf(w()),true)};assertEquals(before,p.toPreferences());assertThrows(IllegalArgumentException::class.java){QuietWeek.save(p,p[K.quietWeekRules]!!,false,emptyList(),true)};assertEquals(before,p.toPreferences())}
 @Test fun corruptModeDisablePreservesRawAndChecksStale(){val p=mutablePreferencesOf(K.quietWeekRules to "broken",K.quietWeekMode to true);QuietWeek.disable(p,"broken",true);assertEquals("broken",p[K.quietWeekRules]);assertFalse(p[K.quietWeekMode]!!);val before=p.toPreferences();assertThrows(IllegalArgumentException::class.java){QuietWeek.disable(p,"old",false)};assertEquals(before,p.toPreferences())}
}
