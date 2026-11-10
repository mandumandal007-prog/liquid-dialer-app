package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class ChecklistTemplatesTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val tf get()=File(ctx.filesDir,"checklist_templates.json")
 private val cf get()=File(ctx.filesDir,"call_checklists.json")
 private val a="+44 2025550123";private val b="+1 2025550123"
 @Before fun clean(){android.util.AtomicFile(tf).delete();android.util.AtomicFile(cf).delete()}
 private fun rejects(f:()->Unit){try{f();fail("must reject")}catch(_:Exception){}}
 @Test fun libraryCrudNeverCopiesNumbersOrTicks(){val x=ChecklistTemplates.save(ctx,null,"Order",listOf("Ask quantity","घर 👪" )).single();assertFalse(x.builtIn);assertEquals(x,ChecklistTemplates.read(ctx).single());ChecklistTemplates.save(ctx,x.id,"Updated",listOf("New"));assertEquals("Updated",ChecklistTemplates.read(ctx).single().title);assertFalse(cf.exists());assertFalse(tf.readText().contains(a));assertFalse(tf.readText().contains("done"));ChecklistTemplates.remove(ctx,x.id);assertTrue(ChecklistTemplates.read(ctx).isEmpty());assertEquals(3,ChecklistTemplates.starters.size)}
 @Test fun appendDeduplicatesWhitespaceCasePreservesOldIdsTicksAndOtherIdentity(){val old=CallChecklist.add(ctx,a,"  Confirm   TIME ").single();CallChecklist.done(ctx,a,old.id,true);val other=CallChecklist.add(ctx,b,"Other");val rows=CallChecklist.appendTopics(ctx,a,listOf("confirm time","New topic"));assertEquals(2,rows.size);assertEquals(old.id,rows.first().id);assertEquals("Confirm   TIME",rows.first().text);assertTrue(rows.first().done);assertFalse(rows.last().done);assertEquals(other,CallChecklist.read(ctx,b));val bytes=cf.readBytes();CallChecklist.appendTopics(ctx,a,listOf("CONFIRM time","new topic"));assertArrayEquals(bytes,cf.readBytes())}
 @Test fun staleCapacityIsAllOrNothingAndRejectsInvalidIdentity(){repeat(29){CallChecklist.add(ctx,a,"$it")};val bytes=cf.readBytes();rejects{CallChecklist.appendTopics(ctx,a,listOf("One","Two"))};assertArrayEquals(bytes,cf.readBytes());for(n in listOf("","*123#","Private","++123"))rejects{CallChecklist.appendTopics(ctx,n,listOf("New"))};assertArrayEquals(bytes,cf.readBytes())}
 @Test fun validationRejectsWithoutTouchingFiles(){ChecklistTemplates.save(ctx,null,"Keep",listOf("One"));val bytes=tf.readBytes();for(topics in listOf(emptyList(),listOf("x".repeat(251)),List(31){"$it"},listOf("a"," A ")))rejects{ChecklistTemplates.save(ctx,null,"New",topics)};for(title in listOf(" ","x".repeat(61),"keep"))rejects{ChecklistTemplates.save(ctx,null,title,listOf("New"))};rejects{ChecklistTemplates.save(ctx,"missing","New",listOf("New"))};rejects{ChecklistTemplates.remove(ctx,"missing")};assertArrayEquals(bytes,tf.readBytes());assertEquals(listOf("One","Two"),ChecklistTemplates.lines("One\n \nTwo\n"))}
 @Test fun corruptFilesNeverOverwritten(){for(raw in listOf("broken","{\"version\":99,\"templates\":[]}","{\"version\":1,\"templates\":[]}{}","{\"version\":1,\"templates\":[]}\u0000","x".repeat(512*1024+1))){tf.writeText(raw);rejects{ChecklistTemplates.save(ctx,null,"New",listOf("New"))};assertEquals(raw,tf.readText())};cf.writeText("broken checklist");rejects{CallChecklist.appendTopics(ctx,a,listOf("New"))};assertEquals("broken checklist",cf.readText())}
 @Test fun capacityAndAtomicRecovery(){repeat(20){ChecklistTemplates.save(ctx,null,"Template $it",listOf("Topic"))};val bytes=tf.readBytes();rejects{ChecklistTemplates.save(ctx,null,"21",listOf("New"))};assertArrayEquals(bytes,tf.readBytes());File(tf.path+".bak").writeBytes(bytes);tf.writeText("partial");assertEquals(20,ChecklistTemplates.read(ctx).size);val first=ChecklistTemplates.read(ctx).first();ChecklistTemplates.remove(ctx,first.id);ChecklistTemplates.save(ctx,null,"Space freed",listOf("New"));assertEquals(20,ChecklistTemplates.read(ctx).size)}
 @Test fun concurrentAppendNeverDuplicatesOrLosesExisting(){val first=CallChecklist.add(ctx,a,"Old").single();CallChecklist.done(ctx,a,first.id,true);val pool=Executors.newFixedThreadPool(4);try{(0..9).map{pool.submit{CallChecklist.appendTopics(ctx,a,listOf("Shared","Item $it"))}}.forEach{it.get()}}finally{pool.shutdown()};val rows=CallChecklist.read(ctx,a);assertEquals(12,rows.size);assertEquals(1,rows.count{it.text=="Shared"});assertTrue(rows.first().done)}
 @Test fun privacyAndLocalTemplatesLeaveOtherFilesUnchanged(){val nf=File(ctx.filesDir,"notes.json");Repo.addNote(ctx,a,"Name","Keep note");val before=nf.readBytes();ChecklistTemplates.save(ctx,null,"Call preparation",listOf("Question"));CallChecklist.appendTopics(ctx,a,ChecklistTemplates.starters.first().topics);assertArrayEquals(before,nf.readBytes());assertFalse(tf.readText().contains(a));assertFalse(tf.readText().contains("Name"))}
}
