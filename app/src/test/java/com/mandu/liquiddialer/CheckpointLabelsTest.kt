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
class CheckpointLabelsTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val s=LdSettings(liveCallDesk=true,callCheckpoints=true,checkpointLabels=true)
 private fun board()=CallCheckpoints.Board().add(12,"First").add(45,"Checkpoint").add(60,"Last")
 private fun open():LiveCallDesk.Session{val c=deskShadowCall();CallManager.calls.value=listOf(c);return LiveCallDesk.open(s,c)!!}
 private fun apply(v:LdSettings){runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=v.liveCallDesk;it[K.callCheckpoints]=v.callCheckpoints;it[K.checkpointLabels]=v.checkpointLabels;it[K.hiddenFeatures]=v.hiddenFeatures};ctx.repo.flow.first{it.liveCallDesk==v.liveCallDesk&&it.callCheckpoints==v.callCheckpoints&&it.checkpointLabels==v.checkpointLabels&&it.hiddenFeatures==v.hiddenFeatures}};LdApp.settings.value=v}
 private fun rejected(f:()->Unit){try{f();Assert.fail("Must reject")}catch(_:IllegalArgumentException){}}
 @Before fun seed(){apply(s);CallManager.videoUpgradeFrom.value=null;File(ctx.filesDir,"notes.json").delete()}
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null;File(ctx.filesDir,"notes.json").delete();apply(LdSettings())}
 @Test fun defaultOffAndFinder(){assertFalse(LdSettings().checkpointLabels);assertEquals("checkpointlabels",SettingsFinder.all.single{it.id=="checkpoint-labels"}.route)}
 @Test fun onlyTargetLabelChanges(){val b=board();val x=b.rename(1," Reference 42 ");assertEquals(listOf(12L,45L,60L),x.marks.map{it.seconds});assertEquals("Reference 42",x.marks[1].label);assertEquals(b.marks[0],x.marks[0]);assertEquals(b.marks[2],x.marks[2]);assertEquals("Checkpoint",b.marks[1].label)}
 @Test fun blankDefaults(){assertEquals("Checkpoint",board().rename(0,"  ").marks[0].label)}
 @Test fun unicodePreserved(){assertEquals("नमस्ते #42",board().rename(1,"नमस्ते #42").marks[1].label)}
 @Test fun eightyAllowed(){assertEquals(80,board().rename(0,"x".repeat(80)).marks[0].label.length)}
 @Test fun overLimitRejected(){rejected{board().rename(0,"x".repeat(81))}}
 @Test fun controlsRejected(){for(c in listOf('\n','\r','\t','\u0000','\u007f'))rejected{board().rename(0,"x${c}y")}}
 @Test fun invalidIndexRejected(){rejected{board().rename(-1,"x")};rejected{board().rename(3,"x")}}
 @Test fun emptyRejected(){rejected{CallCheckpoints.Board().rename(0,"x")}}
 @Test fun savedLocked(){rejected{board().copy(saved=true).rename(1,"x")}}
 @Test fun duplicatesTimeKeepOrder(){val b=CallCheckpoints.Board().add(1,"a").add(1,"b");assertEquals(listOf(CallCheckpoints.Mark(1,"a"),CallCheckpoints.Mark(1,"new")),b.rename(1,"new").marks)}
 @Test fun exactSummaryUsesAppliedLabel(){assertEquals("Manual call checkpoints (elapsed from connection, not a recording):\n0:00:12 | First\n0:00:45 | Reference 42\n0:01:00 | Last",board().rename(1,"Reference 42").summary())}
 @Test fun exactCurrentCallAllows(){val x=open();val b=board();assertEquals(b.rename(1,"new"),CallCheckpoints.rename(x,b,b,1,"new"));assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 @Test fun expectedBoardMismatchRefuses(){val x=open();val b=board();assertNull(CallCheckpoints.rename(x,b,b.remove(0),1,"new"))}
 @Test fun editorOffRefuses(){val x=open();val b=board();apply(s.copy(checkpointLabels=false));assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun checkpointsOffRefuses(){val x=open();val b=board();apply(s.copy(callCheckpoints=false));assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun deskOffRefuses(){val x=open();val b=board();apply(s.copy(liveCallDesk=false));assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun notesHiddenRefuses(){val x=open();val b=board();apply(s.copy(hiddenFeatures=setOf("notes")));assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun replacementSameNumberRefuses(){val x=open();val b=board();CallManager.calls.value=listOf(deskShadowCall());assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun secondCallRefuses(){val x=open();val b=board();CallManager.calls.value=listOf(x.target,deskShadowCall("+919999999999",Call.STATE_RINGING));assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun holdRefuses(){val x=open();val b=board();setDeskState(x.target,Call.STATE_HOLDING);assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun videoRequestRefuses(){val x=open();val b=board();CallManager.videoUpgradeFrom.value=x.target;assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun disconnectedRefuses(){val x=open();val b=board();CallManager.calls.value=emptyList();assertNull(CallCheckpoints.rename(x,b,b,1,"new"))}
 @Test fun oldPreviewCannotCommitAfterEdit(){val x=open();val b=board();val edited=CallCheckpoints.rename(x,b,b,1,"Corrected")!!;assertFalse(CallCheckpoints.Commit().save(ctx,x,b,edited));assertTrue(Repo.notesFor(ctx,x.number).isEmpty())}
 @Test fun editedPreviewSavesExactOncePreservesExisting(){val x=open();Repo.addNote(ctx,x.number,"Before","Keep");val old=Repo.notesFor(ctx,x.number).single();val b=board().rename(1,"Corrected");val commit=CallCheckpoints.Commit();assertTrue(commit.save(ctx,x,b,b));assertFalse(commit.save(ctx,x,b,b));val rows=Repo.notesFor(ctx,x.number);assertTrue(rows.contains(old));assertEquals(b.summary(),rows.single{it.id!=old.id}.text);assertTrue(Repo.notesFor(ctx,"+449876543210").isEmpty())}
}
