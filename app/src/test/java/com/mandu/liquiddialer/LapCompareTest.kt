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
class LapCompareTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val s=LdSettings(liveCallDesk=true,deskStopwatch=true,lapCompare=true)
 private fun board()=DeskStopwatch.Board().start(0).lap(1000,"First").lap(5000,"Second").lap(7000,"Third").pause(9000)
 private fun reject(f:()->Unit){try{f();Assert.fail("Must refuse")}catch(_:Exception){}}
 private fun session():LiveCallDesk.Session{val c=deskShadowCall();CallManager.calls.value=listOf(c);return LiveCallDesk.open(s,c)!!}
 @Before fun seed(){runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=true;it[K.deskStopwatch]=true;it[K.lapCompare]=true;it[K.hiddenFeatures]=emptySet()};ctx.repo.flow.first{it.liveCallDesk&&it.deskStopwatch&&it.lapCompare&&it.hiddenFeatures.isEmpty()}};LdApp.settings.value=s;CallManager.videoUpgradeFrom.value=null}
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null;LdApp.settings.value=LdSettings()}
 @Test fun defaultOffAndFinder(){assertFalse(LdSettings().lapCompare);assertEquals("lapcompare",SettingsFinder.all.single{it.id=="lap-compare"}.route)}
 @Test fun durationsNotCumulative(){assertEquals(listOf(1000L,4000L,2000L),LapCompare.rows(board()).map{it.durationMs});assertEquals(listOf(1000L,5000L,7000L),LapCompare.rows(board()).map{it.totalMs})}
 @Test fun selectionOnly(){val r=LapCompare.analyze(board(),setOf(0,2));assertEquals(3000L,r.subtotalMs);assertEquals(1500L,r.averageMs);assertEquals(listOf(0,2),r.rows.map{it.index})}
 @Test fun rangeAndRemainder(){val r=LapCompare.analyze(board(),setOf(0,1,2));assertEquals(1000L,r.shortestMs);assertEquals(4000L,r.longestMs);assertEquals(2000L,r.remainderMs);assertEquals(7000L,r.subtotalMs)}
 @Test fun averageRoundedDown(){val b=DeskStopwatch.Board().start(0).lap(1,"a").lap(3,"b").pause(3);assertEquals(1L,LapCompare.analyze(b,setOf(0,1)).averageMs)}
 @Test fun singletonSameRange(){val r=LapCompare.analyze(board(),setOf(1));assertEquals(4000L,r.subtotalMs);assertEquals(r.subtotalMs,r.averageMs);assertEquals(r.shortestMs,r.longestMs)}
 @Test fun noneSelectedRefused(){reject{LapCompare.analyze(board(),emptySet())}}
 @Test fun invalidSelectionRefused(){reject{LapCompare.analyze(board(),setOf(-1))};reject{LapCompare.analyze(board(),setOf(3))}}
 @Test fun runningRefused(){reject{LapCompare.rows(board().start(10000))}}
 @Test fun emptyBoardRowsEmptySelectionRefused(){assertTrue(LapCompare.rows(DeskStopwatch.Board()).isEmpty());reject{LapCompare.analyze(DeskStopwatch.Board(),setOf(0))}}
 @Test fun malformedBoardRefused(){reject{LapCompare.rows(DeskStopwatch.Board(1,laps=listOf(DeskStopwatch.Lap(2,"x"))))}}
 @Test fun originalOrderIndependentOfSelectionSetOrder(){assertEquals(listOf(0,2),LapCompare.analyze(board(),linkedSetOf(2,0)).rows.map{it.index})}
 @Test fun longestOrderPreservesIndices(){assertEquals(listOf(1,2,0),LapCompare.ordered(LapCompare.rows(board()),true).map{it.index})}
 @Test fun restoreOriginalOrder(){val r=LapCompare.ordered(LapCompare.rows(board()),true);assertEquals(listOf(0,1,2),LapCompare.ordered(r,false).map{it.index})}
 @Test fun tiesKeepSourceOrder(){val b=DeskStopwatch.Board().start(0).lap(1000,"A").lap(2000,"B").lap(3000,"C").pause(3000);assertEquals(listOf(0,1,2),LapCompare.ordered(LapCompare.rows(b),true).map{it.index})}
 @Test fun savedBoardAllowedAndUnchanged(){val b=board().copy(saved=true);val copy=b.copy(laps=b.laps.toList());LapCompare.analyze(b,setOf(0,1,2));assertEquals(copy,b);assertEquals(copy.summary(),b.summary())}
 @Test fun noStorageWrites(){val x=session();val b=board();val files=ctx.filesDir.listFiles().orEmpty().filter{it.isFile}.associate{it.name to it.readBytes().toList()};assertTrue(LapCompare.current(x,b,b));LapCompare.analyze(b,setOf(0));val after=ctx.filesDir.listFiles().orEmpty().filter{it.isFile}.associate{it.name to it.readBytes().toList()};assertEquals(files,after)}
 @Test fun allTwelveBoundedTotal(){var b=DeskStopwatch.Board().start(0);repeat(12){b=b.lap((it+1)*50000000L,"$it")};b=b.pause(DeskStopwatch.MAX_MS);val r=LapCompare.analyze(b,(0..11).toSet());assertEquals(600000000L,r.subtotalMs);assertEquals(4800000L,r.remainderMs)}
 @Test fun exactCurrentBoardRequired(){val x=session();val b=board();assertTrue(LapCompare.current(x,b,b));assertFalse(LapCompare.current(x,b,b.copy(accumulatedMs=10000)))}
 @Test fun runningCannotInherit(){val x=session();val b=board().start(10000);assertFalse(LapCompare.current(x,b,b))}
 private fun blocked(change:()->Unit){val x=session();val b=board();change();assertFalse(LapCompare.current(x,b,b))}
 @Test fun ownOff(){blocked{LdApp.settings.value=s.copy(lapCompare=false)}}
 @Test fun timerOff(){blocked{LdApp.settings.value=s.copy(deskStopwatch=false)}}
 @Test fun deskOff(){blocked{LdApp.settings.value=s.copy(liveCallDesk=false)}}
 @Test fun hiddenNotes(){blocked{LdApp.settings.value=s.copy(hiddenFeatures=setOf("notes"))}}
 @Test fun replacement(){blocked{CallManager.calls.value=listOf(deskShadowCall())}}
 @Test fun secondCall(){blocked{CallManager.calls.value=CallManager.calls.value+deskShadowCall("+919999999999",Call.STATE_RINGING)}}
 @Test fun hold(){blocked{setDeskState(CallManager.calls.value.single(),Call.STATE_HOLDING)}}
 @Test fun videoRequest(){blocked{CallManager.videoUpgradeFrom.value=CallManager.calls.value.single()}}
 @Test fun disconnect(){blocked{CallManager.calls.value=emptyList()}}
 @Test fun hindiLabelsExact(){val b=DeskStopwatch.Board().start(0).lap(500,"ऑर्डर तैयार करना").pause(500);assertEquals("ऑर्डर तैयार करना",LapCompare.analyze(b,setOf(0)).rows.single().label)}
}
