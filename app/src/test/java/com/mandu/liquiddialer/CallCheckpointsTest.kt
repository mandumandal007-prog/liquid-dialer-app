package com.mandu.liquiddialer
import android.telecom.Call
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallCheckpointsTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val s=LdSettings(liveCallDesk=true,callCheckpoints=true)
 private fun apply(v:LdSettings){runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=v.liveCallDesk;it[K.callCheckpoints]=v.callCheckpoints;it[K.hiddenFeatures]=v.hiddenFeatures};ctx.repo.flow.first{it.liveCallDesk==v.liveCallDesk&&it.callCheckpoints==v.callCheckpoints&&it.hiddenFeatures==v.hiddenFeatures}};LdApp.settings.value=v}
 private fun board()=CallCheckpoints.Board().add(92," Order 123 ").add(121,"Resolution")
 private fun open():LiveCallDesk.Session{val c=deskShadowCall();CallManager.calls.value=listOf(c);return LiveCallDesk.open(s,c)!!}
 private fun fail(action:()->Unit){try{action();Assert.fail("Must reject")}catch(_:IllegalArgumentException){}}
 @Before fun seed(){runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=true;it[K.callCheckpoints]=true;it[K.hiddenFeatures]=emptySet()};ctx.repo.flow.first{it.liveCallDesk&&it.callCheckpoints}};LdApp.settings.value=s;CallManager.videoUpgradeFrom.value=null;File(ctx.filesDir,"notes.json").delete()}
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null;File(ctx.filesDir,"notes.json").delete();runBlocking{ctx.repo.set(K.callCheckpoints,false)};LdApp.settings.value=LdSettings()}
 @Test fun defaultOffFinder(){assertFalse(LdSettings().callCheckpoints);assertEquals("callcheckpoints",SettingsFinder.all.single{it.id=="call-checkpoints"}.route)}
 @Test fun addTrimsLabel(){assertEquals("Order 123",board().marks.first().label)}
 @Test fun blankLabelIsCheckpoint(){assertEquals("Checkpoint",CallCheckpoints.Board().add(0,"  ").marks.single().label)}
 @Test fun exactFormatting(){assertEquals("Manual call checkpoints (elapsed from connection, not a recording):\n0:01:32 | Order 123\n0:02:01 | Resolution",board().summary())}
 @Test fun hoursAndSevenDays(){assertEquals("1:01:01",CallCheckpoints.time(3661));assertEquals("168:00:00",CallCheckpoints.time(604800))}
 @Test fun negativeRejected(){fail{CallCheckpoints.Board().add(-1,"")};fail{CallCheckpoints.time(-1)}}
 @Test fun excessiveElapsedRejected(){fail{CallCheckpoints.Board().add(604801,"")};fail{CallCheckpoints.time(604801)}}
 @Test fun labelLimit(){assertEquals(80,CallCheckpoints.Board().add(1,"x".repeat(80)).marks.single().label.length);fail{CallCheckpoints.Board().add(1,"x".repeat(81))}}
 @Test fun noInjectedRows(){for(c in listOf('\n','\r','\t','\u0000','\u007f'))fail{CallCheckpoints.Board().add(1,"x${c}y")}}
 @Test fun allowsHindiAndPunctuation(){val text="ऑर्डर #123 | बात";assertEquals(text,CallCheckpoints.Board().add(1,text).marks.single().label)}
 @Test fun backwardsRefused(){fail{board().add(90,"Bad clock")}}
 @Test fun twoMarksSameSecondAllowed(){assertEquals(2,CallCheckpoints.Board().add(1,"a").add(1,"b").marks.size)}
 @Test fun twelveCap(){var b=CallCheckpoints.Board();repeat(12){b=b.add(it.toLong(),"$it")};assertEquals(12,b.marks.size);fail{b.add(12,"13")}}
 @Test fun removeExactRow(){assertEquals(listOf(CallCheckpoints.Mark(121,"Resolution")),board().remove(0).marks)}
 @Test fun removeInvalidRejected(){fail{board().remove(-1)};fail{board().remove(2)}}
 @Test fun emptyNotSavable(){fail{CallCheckpoints.Board().summary()}}
 @Test fun savedBoardLocked(){val b=board().copy(saved=true);fail{b.add(150,"x")};fail{b.remove(0)}}
 @Test fun malformedBoardRejected(){fail{CallCheckpoints.Board(listOf(CallCheckpoints.Mark(1,""))).summary()};fail{CallCheckpoints.Board(listOf(CallCheckpoints.Mark(2,"a"),CallCheckpoints.Mark(1,"b"))).summary()}}
 @Test fun connectedTimeUsed(){val x=open();assertEquals(123L,CallCheckpoints.elapsed(x,x.target.details.connectTimeMillis+123999))}
 @Test fun elapsedClockBackRejected(){val x=open();assertNull(CallCheckpoints.elapsed(x,x.target.details.connectTimeMillis-1))}
 @Test fun elapsedTooOldRejected(){val x=open();assertNull(CallCheckpoints.elapsed(x,x.target.details.connectTimeMillis+604801000))}
 @Test fun saveExactNumberSummaryPreserveOld(){val x=open();Repo.addNote(ctx,x.number,"Existing","Keep me");val old=Repo.notesFor(ctx,x.number).single();val b=board();assertTrue(CallCheckpoints.Commit().save(ctx,x,b,b));val all=Repo.notesFor(ctx,x.number);assertEquals(2,all.size);assertTrue(all.contains(old));assertEquals(b.summary(),all.single{it.id!=old.id}.text);assertTrue(Repo.notesFor(ctx,"+449876543210").isEmpty())}
 @Test fun repeatCommitNoDuplicate(){val x=open();val b=board();val c=CallCheckpoints.Commit();assertTrue(c.save(ctx,x,b,b));assertFalse(c.save(ctx,x,b,b));assertEquals(1,Repo.notesFor(ctx,x.number).size)}
 @Test fun changedBoardCannotSavePreview(){val x=open();val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b.remove(0)));assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 @Test fun alreadySavedBoardRefused(){val x=open();val b=board().copy(saved=true);assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b));assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 @Test fun offRefusesElapsedAndSave(){val x=open();apply(s.copy(callCheckpoints=false));assertNull(CallCheckpoints.elapsed(x,System.currentTimeMillis()));val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun masterOffRefuses(){val x=open();apply(s.copy(liveCallDesk=false));val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun hiddenNotesRefuses(){val x=open();apply(s.copy(hiddenFeatures=setOf("notes")));val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun sameNumberReplacementRefuses(){val x=open();CallManager.calls.value=listOf(deskShadowCall());val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b));assertNull(CallCheckpoints.elapsed(x,System.currentTimeMillis()));assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 @Test fun secondCallRefuses(){val x=open();CallManager.calls.value=listOf(x.target,deskShadowCall("+919999999999",Call.STATE_RINGING));val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun holdRefuses(){val x=open();setDeskState(x.target,Call.STATE_HOLDING);val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun videoRequestRefuses(){val x=open();CallManager.videoUpgradeFrom.value=x.target;val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun disconnectRefuses(){val x=open();CallManager.calls.value=emptyList();val b=board();assertFalse(CallCheckpoints.Commit().save(ctx,x,b,b))}
 @Test fun failedWritePreservesCorruptionThenExplicitRetry(){val x=open();val f=File(ctx.filesDir,"notes.json");f.writeText("broken");val c=CallCheckpoints.Commit();val b=board();try{c.save(ctx,x,b,b);Assert.fail()}catch(_:Exception){};assertEquals("broken",f.readText());f.delete();assertTrue(c.save(ctx,x,b,b));assertEquals(1,Repo.notesFor(ctx,x.number).size)}
}
