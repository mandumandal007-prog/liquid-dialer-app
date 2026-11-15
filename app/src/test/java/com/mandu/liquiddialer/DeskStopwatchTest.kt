package com.mandu.liquiddialer
import android.telecom.Call
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class DeskStopwatchTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val s=LdSettings(liveCallDesk=true,deskStopwatch=true)
 private fun reject(f:()->Unit){try{f();Assert.fail("Must reject")}catch(_:Exception){}}
 private fun board()=DeskStopwatch.Board().start(1000).lap(3500,"Check price").pause(5000)
 private fun session():LiveCallDesk.Session {val c=deskShadowCall();CallManager.calls.value=listOf(c);return LiveCallDesk.open(s,c)!!}
 @Before fun seed(){runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=true;it[K.deskStopwatch]=true;it[K.hiddenFeatures]=emptySet()};ctx.repo.flow.first{it.liveCallDesk&&it.deskStopwatch&&it.hiddenFeatures.isEmpty()}};LdApp.settings.value=s;CallManager.videoUpgradeFrom.value=null;File(ctx.filesDir,"notes.json").delete()}
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null;File(ctx.filesDir,"notes.json").delete();LdApp.settings.value=LdSettings()}
 @Test fun defaultsAndFinder(){assertFalse(LdSettings().deskStopwatch);assertEquals("deskstopwatch",SettingsFinder.all.single{it.id=="desk-stopwatch"}.route)}
 @Test fun blankBoardNotSavable(){reject{DeskStopwatch.Board().summary()};assertEquals(0,DeskStopwatch.Board().elapsed(100))}
 @Test fun startUsesOnlyMonotonicDifference(){val b=DeskStopwatch.Board().start(12345);assertEquals(1000,b.elapsed(13345));assertEquals(0,b.accumulatedMs)}
 @Test fun pauseFreezes(){val b=DeskStopwatch.Board().start(1000).pause(4000);assertEquals(3000,b.elapsed(999999));assertNull(b.runningSince)}
 @Test fun resumeExcludesPause(){val b=DeskStopwatch.Board().start(1000).pause(4000).start(9000).pause(11000);assertEquals(5000,b.accumulatedMs)}
 @Test fun lapsDoNotLoseElapsed(){val b=DeskStopwatch.Board().start(1000).lap(3500,"A").lap(5000,"B").pause(6000);assertEquals(listOf(2500L,4000L),b.laps.map{it.totalMs});assertEquals(5000,b.accumulatedMs)}
 @Test fun lapAfterResumeUsesPreviousCumulative(){val b=DeskStopwatch.Board().start(1000).lap(2000,"A").pause(3000).start(9000).lap(10000,"B");assertEquals(3000,b.laps.last().totalMs)}
 @Test fun blankLabelDefaultsAndTrim(){val b=DeskStopwatch.Board().start(0).lap(100,"  ").lap(200," A ");assertEquals(listOf("Interval 1","A"),b.laps.map{it.label})}
 @Test fun noDuplicateZeroLengthLap(){reject{DeskStopwatch.Board().start(0).lap(0,"")};val b=DeskStopwatch.Board().start(0).lap(10,"");reject{b.lap(10,"")}}
 @Test fun labelBoundaries(){val b=DeskStopwatch.Board().start(0);assertEquals(80,b.lap(1,"x".repeat(80)).laps.single().label.length);reject{b.lap(1,"x".repeat(81))};for(c in listOf('\n','\t','\r','\u0000'))reject{b.lap(1,"a${c}b")}}
 @Test fun twelveLapCap(){var b=DeskStopwatch.Board().start(0);repeat(12){b=b.lap(it+1L,"$it")};assertEquals(12,b.laps.size);reject{b.lap(13,"")};assertEquals(14,b.pause(14).accumulatedMs)}
 @Test fun backwardsClockRefuses(){val b=DeskStopwatch.Board().start(10);reject{b.elapsed(9)};reject{b.pause(9)};reject{b.lap(9,"")}}
 @Test fun sevenDayCapAndOverflowRefuse(){assertEquals(DeskStopwatch.MAX_MS,DeskStopwatch.Board().start(0).elapsed(DeskStopwatch.MAX_MS));reject{DeskStopwatch.Board().start(0).elapsed(Long.MAX_VALUE)};reject{DeskStopwatch.Board(DeskStopwatch.MAX_MS).start(1)}}
 @Test fun invalidTransitions(){reject{DeskStopwatch.Board().pause(1)};reject{DeskStopwatch.Board().lap(1,"")};reject{DeskStopwatch.Board().start(0).start(1)};reject{DeskStopwatch.Board().start(-1)}}
 @Test fun runningSummaryRefuses(){reject{DeskStopwatch.Board().start(0).summary()}}
 @Test fun savedLocks(){val b=board().copy(saved=true);reject{b.start(10000)};reject{b.lap(10000,"")};reject{b.pause(10000)}}
 @Test fun malformedBoardsRefuse(){reject{DeskStopwatch.Board(-1).elapsed(1)};reject{DeskStopwatch.Board(100,laps=listOf(DeskStopwatch.Lap(200,"Bad"))).summary()};reject{DeskStopwatch.Board(100,laps=listOf(DeskStopwatch.Lap(50,"A"),DeskStopwatch.Lap(50,"B"))).summary()}}
 @Test fun subTenthNeverDisplayedAsZero(){assertEquals("<0.1s",DeskStopwatch.time(1));assertEquals("<0.1s",DeskStopwatch.time(99));assertEquals("0:00:00.1",DeskStopwatch.time(100))}
 @Test fun formatting(){assertEquals("1:01:01.2",DeskStopwatch.time(3661234));assertEquals("168:00:00.0",DeskStopwatch.time(DeskStopwatch.MAX_MS));reject{DeskStopwatch.time(-1)}}
 @Test fun exactSummaryAndRemainder(){assertEquals("Manual desk stopwatch (not call duration, billing or recording):\nTimed total: 0:00:04.0\nManual pauses excluded. Precision shown: 0.1 second.\n1. Check price | lap 0:00:02.5 | total 0:00:02.5\nUnmarked remainder | 0:00:01.5",board().summary())}
 @Test fun wholeTimeWithoutLapsIsRemainder(){val text=DeskStopwatch.Board().start(0).pause(1000).summary();assertTrue(text.endsWith("Unmarked remainder | 0:00:01.0"))}
 @Test fun noZeroRemainder(){assertFalse(DeskStopwatch.Board().start(0).lap(1000,"ऑर्डर").pause(1000).summary().contains("Unmarked remainder"))}
 @Test fun explicitSaveExactNumberPreservesOld(){val x=session();Repo.addNote(ctx,x.number,"","Keep");val old=Repo.notesFor(ctx,x.number).single();val b=board();assertTrue(DeskStopwatch.Commit().save(ctx,x,b,b));val all=Repo.notesFor(ctx,x.number);assertEquals(2,all.size);assertTrue(old in all);assertEquals(b.summary(),all.single{it.id!=old.id}.text);assertTrue(Repo.notesFor(ctx,"+449876543210").isEmpty())}
 @Test fun repeatSaveRefuses(){val x=session();val b=board();val c=DeskStopwatch.Commit();assertTrue(c.save(ctx,x,b,b));assertFalse(c.save(ctx,x,b,b));assertEquals(1,Repo.notesFor(ctx,x.number).size)}
 @Test fun staleBoardRefuses(){val x=session();val b=board();assertFalse(DeskStopwatch.Commit().save(ctx,x,b,b.start(10000)));assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 private fun blocked(change:()->Unit){val x=session();val b=board();change();assertFalse(DeskStopwatch.Commit().save(ctx,x,b,b));reject{DeskStopwatch.change(x,b,10000,"start")};assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 @Test fun ownOffRefuses(){blocked{LdApp.settings.value=s.copy(deskStopwatch=false)}}
 @Test fun deskOffRefuses(){blocked{LdApp.settings.value=s.copy(liveCallDesk=false)}}
 @Test fun notesHiddenRefuses(){blocked{LdApp.settings.value=s.copy(hiddenFeatures=setOf("notes"))}}
 @Test fun sameNumberReplacementRefuses(){blocked{CallManager.calls.value=listOf(deskShadowCall())}}
 @Test fun secondCallRefuses(){blocked{CallManager.calls.value=CallManager.calls.value+deskShadowCall("+919999999999",Call.STATE_RINGING)}}
 @Test fun holdRefuses(){blocked{setDeskState(CallManager.calls.value.single(),Call.STATE_HOLDING)}}
 @Test fun videoRefuses(){blocked{CallManager.videoUpgradeFrom.value=CallManager.calls.value.single()}}
 @Test fun disconnectRefuses(){blocked{CallManager.calls.value=emptyList()}}
 @Test fun corruptedFilePreservedThenExplicitRetry(){val x=session();val b=board();val c=DeskStopwatch.Commit();val f=File(ctx.filesDir,"notes.json");f.writeText("broken");reject{c.save(ctx,x,b,b)};assertEquals("broken",f.readText());f.delete();assertTrue(c.save(ctx,x,b,b));assertEquals(1,Repo.notesFor(ctx,x.number).size)}
 @Test fun guardedTransitionsWorkWithoutCheckpoints(){val x=session();val started=DeskStopwatch.change(x,DeskStopwatch.Board(),100,"start");val l=DeskStopwatch.change(x,started,1200,"lap","Order");val p=DeskStopwatch.change(x,l,2100,"pause");assertEquals(2000,p.accumulatedMs);assertEquals("Order",p.laps.single().label);reject{DeskStopwatch.change(x,p,3000,"unknown")}}
}
