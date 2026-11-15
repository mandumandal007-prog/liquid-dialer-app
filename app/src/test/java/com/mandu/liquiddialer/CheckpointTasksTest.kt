package com.mandu.liquiddialer
import android.telecom.Call
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CheckpointTasksTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+919876543210"
 private val s=LdSettings(liveCallDesk=true,callCheckpoints=true,checkpointTasks=true)
 private fun board()=CallCheckpoints.Board().add(1,"Ask delivery").add(4,"Confirm rate")
 private fun session():LiveCallDesk.Session {val c=deskShadowCall();CallManager.calls.value=listOf(c);return LiveCallDesk.open(s,c)!!}
 private fun reject(f:()->Unit){try{f();Assert.fail("Must reject")}catch(_:Exception){}}
 private fun file()=File(ctx.filesDir,"call_checklists.json")
 @Before fun setup(){file().delete();File(file().path+".bak").delete();File(ctx.filesDir,"notes.json").delete();runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=true;it[K.callCheckpoints]=true;it[K.checkpointTasks]=true;it[K.hiddenFeatures]=emptySet()};ctx.repo.flow.first{it.liveCallDesk&&it.callCheckpoints&&it.checkpointTasks&&it.hiddenFeatures.isEmpty()}};LdApp.settings.value=s;CallManager.videoUpgradeFrom.value=null}
 @After fun clean(){file().delete();File(file().path+".bak").delete();File(ctx.filesDir,"notes.json").delete();LdApp.settings.value=LdSettings();CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null}
 @Test fun defaultOffAndFinder(){assertFalse(LdSettings().checkpointTasks);assertEquals("checkpointtasks",SettingsFinder.all.single{it.id=="checkpoint-tasks"}.route)}
 @Test fun selectedOnlySourceOrderAndNoTimestamps(){val p=CheckpointTasks.prepare(board(),setOf(1),emptyList());assertEquals(listOf("Confirm rate"),p.topics);assertEquals(0,p.skipped)}
 @Test fun allSelectedKeepBoardOrder(){assertEquals(listOf("Ask delivery","Confirm rate"),CheckpointTasks.prepare(board(),linkedSetOf(1,0),emptyList()).topics)}
 @Test fun duplicateSelectionKeepsFirstLabel(){val b=board().add(5," ASK   DELIVERY ");val p=CheckpointTasks.prepare(b,setOf(0,1,2),emptyList());assertEquals(listOf("Ask delivery","Confirm rate"),p.topics);assertEquals(1,p.skipped)}
 @Test fun existingDoneDuplicatesSkipped(){val e=listOf(CallChecklist.Item("a"," ask  DELIVERY ",true));val p=CheckpointTasks.prepare(board(),setOf(0,1),e);assertEquals(listOf("Confirm rate"),p.topics);assertEquals(e,p.existing);assertEquals(1,p.skipped)}
 @Test fun noSelectedRefused(){reject{CheckpointTasks.prepare(board(),emptySet(),emptyList())}}
 @Test fun invalidSelectedRefused(){reject{CheckpointTasks.prepare(board(),setOf(-1),emptyList())};reject{CheckpointTasks.prepare(board(),setOf(2),emptyList())}}
 @Test fun malformedBoardRefused(){reject{CheckpointTasks.prepare(CallCheckpoints.Board(listOf(CallCheckpoints.Mark(1,"\n"))),setOf(0),emptyList())}}
 @Test fun fullCapacityRefusesWholeBatch(){val e=(1..29).map{CallChecklist.Item("$it","Existing $it")};reject{CheckpointTasks.prepare(board(),setOf(0,1),e)};assertEquals(29,e.size)}
 @Test fun exactCapacityAllowed(){val e=(1..29).map{CallChecklist.Item("$it","Existing $it")};assertEquals(1,CheckpointTasks.prepare(board(),setOf(1),e).topics.size)}
 @Test fun savePreservesExistingAndOtherCountry(){val x=session();CallChecklist.add(ctx,number,"Keep");val e=CallChecklist.done(ctx,number,CallChecklist.read(ctx,number).single().id,true);val foreign=CallChecklist.add(ctx,"+449876543210","Foreign");val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0,1));val saved=CheckpointTasks.Commit().save(ctx,x,p,b);assertEquals(e,saved.take(1));assertEquals(listOf("Ask delivery","Confirm rate"),saved.drop(1).map{it.text});assertTrue(saved.drop(1).none{it.done});assertEquals(foreign,CallChecklist.read(ctx,"+449876543210"));assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun staleListRefusesNoPartialWrite(){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0,1));CallChecklist.add(ctx,number,"Other tool");val raw=file().readText();reject{CheckpointTasks.Commit().save(ctx,x,p,b)};assertEquals(raw,file().readText())}
 @Test fun staleBoardRefused(){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));reject{CheckpointTasks.Commit().save(ctx,x,p,b.rename(0,"Changed"))};assertFalse(file().exists())}
 @Test fun tamperedTopicsRefused(){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0)).copy(topics=listOf("Injected"));reject{CheckpointTasks.Commit().save(ctx,x,p,b)};assertFalse(file().exists())}
 @Test fun repeatCommitRefuses(){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));val c=CheckpointTasks.Commit();c.save(ctx,x,p,b);val raw=file().readText();reject{c.save(ctx,x,p,b)};assertEquals(raw,file().readText())}
 @Test fun allDuplicateNoWrite(){val x=session();CallChecklist.add(ctx,number,"Ask delivery");val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));assertTrue(p.topics.isEmpty());val raw=file().readText();reject{CheckpointTasks.Commit().save(ctx,x,p,b)};assertEquals(raw,file().readText())}
 @Test fun savedNoteBoardAllowedNotWrittenAgain(){val x=session();val b=board().copy(saved=true);val p=CheckpointTasks.preview(ctx,x,b,setOf(1));CheckpointTasks.Commit().save(ctx,x,p,b);assertEquals("Confirm rate",CallChecklist.read(ctx,number).single().text);assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 private fun blocked(change:()->Unit){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));change();reject{CheckpointTasks.Commit().save(ctx,x,p,b)};assertFalse(file().exists())}
 @Test fun ownOffRefused(){blocked{LdApp.settings.value=s.copy(checkpointTasks=false)}}
 @Test fun checkpointsOffRefused(){blocked{LdApp.settings.value=s.copy(callCheckpoints=false)}}
 @Test fun deskOffRefused(){blocked{LdApp.settings.value=s.copy(liveCallDesk=false)}}
 @Test fun hiddenChecklistRefused(){blocked{LdApp.settings.value=s.copy(hiddenFeatures=setOf("checklist"))}}
 @Test fun hiddenNotesRefused(){blocked{LdApp.settings.value=s.copy(hiddenFeatures=setOf("notes"))}}
 @Test fun sameNumberReplacementRefused(){blocked{CallManager.calls.value=listOf(deskShadowCall())}}
 @Test fun secondCallRefused(){blocked{CallManager.calls.value=CallManager.calls.value+deskShadowCall("+919999999999",Call.STATE_RINGING)}}
 @Test fun holdRefused(){blocked{setDeskState(CallManager.calls.value.single(),Call.STATE_HOLDING)}}
 @Test fun videoRequestRefused(){blocked{CallManager.videoUpgradeFrom.value=CallManager.calls.value.single()}}
 @Test fun disconnectedRefused(){blocked{CallManager.calls.value=emptyList()}}
 @Test fun corruptReadPreservesBytes(){val x=session();file().writeText("broken");reject{CheckpointTasks.preview(ctx,x,board(),setOf(0))};assertEquals("broken",file().readText())}
 @Test fun corruptionAfterPreviewPreserved(){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));file().writeText("broken");reject{CheckpointTasks.Commit().save(ctx,x,p,b)};assertEquals("broken",file().readText())}
 @Test fun otherNumberChangesPreservedNotStale(){val x=session();val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));val foreign=CallChecklist.add(ctx,"+449876543210","Foreign");CheckpointTasks.Commit().save(ctx,x,p,b);assertEquals(foreign,CallChecklist.read(ctx,"+449876543210"))}
 @Test fun staleTickRefusesAndPreserves(){val x=session();CallChecklist.add(ctx,number,"Keep");val b=board();val p=CheckpointTasks.preview(ctx,x,b,setOf(0));CallChecklist.done(ctx,number,p.existing.single().id,true);val raw=file().readText();reject{CheckpointTasks.Commit().save(ctx,x,p,b)};assertEquals(raw,file().readText());assertTrue(CallChecklist.read(ctx,number).single().done)}
 @Test fun previewOffCannotRead(){val x=session();LdApp.settings.value=s.copy(checkpointTasks=false);file().writeText("broken");reject{CheckpointTasks.preview(ctx,x,board(),setOf(0))};assertEquals("broken",file().readText())}
 @Test fun hindiAndEightyCharsPreserved(){val b=CallCheckpoints.Board().add(1,"ऑर्डर तैयार करना").add(2,"x".repeat(80));assertEquals(b.marks.map{it.label},CheckpointTasks.prepare(b,setOf(0,1),emptyList()).topics)}
}
