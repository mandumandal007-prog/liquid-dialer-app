package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class ChecklistDuplicatesTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210";private val foreign="+449876543210"
 private fun row(id:String,text:String,done:Boolean=false)=CallChecklist.Item(id,text,done)
 private val rows=listOf(row("a","Delivery time",true),row("x","Unique",true),row("b","DELIVERY   TIME",false),row("c","Rate?",true),row("d","rate?",true))
 private fun view(s:Set<String> =setOf("delivery time"))=ChecklistDuplicates.preview(rows,s)
 private fun refuse(f:()->Unit){try{f();fail("Should refuse")}catch(_:IllegalArgumentException){}}
 @Before fun clear(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete()}
 @Test fun normalizationCaseWhitespaceOnly(){val g=ChecklistDuplicates.groups(rows);assertEquals(2,g.size);assertEquals(listOf(1,3),g[0].positions);assertEquals("delivery time",g[0].key)}
 @Test fun firstIdTextPositionKeptPendingIfAnyPending(){val v=view();assertEquals(listOf("a","x","c","d"),v.result.map{it.id});assertEquals("Delivery time",v.result[0].text);assertFalse(v.result[0].done);assertEquals(rows[1],v.result[1]);assertEquals(1,v.removed)}
 @Test fun allDiscussedRemainsDiscussed(){val v=view(setOf("rate?"));assertTrue(v.result.last().done);assertEquals("c",v.result.last().id)}
 @Test fun firstPendingNeverPromotedByLaterDone(){val r=listOf(row("a","Same"),row("b","same",true));assertFalse(ChecklistDuplicates.preview(r,setOf("same")).result.single().done)}
 @Test fun allPendingRemainsPending(){val r=listOf(row("a","Same"),row("b","same"));assertFalse(ChecklistDuplicates.preview(r,setOf("same")).result.single().done)}
 @Test fun selectAllPreservesUnrelatedAndOrder(){val v=view(setOf("delivery time","rate?"));assertEquals(listOf("a","x","c"),v.result.map{it.id});assertEquals(rows[1],v.result[1]);assertEquals(2,v.removed)}
 @Test fun punctuationAccentsDigitsNotMerged(){assertTrue(ChecklistDuplicates.groups(listOf(row("a","Rate?"),row("b","Rate!"),row("c","Café"),row("d","Cafe"),row("e","Item 1"),row("f","Item 2"))).isEmpty())}
 @Test fun unicodeCaseAndInternalWhitespaceMatching(){val r=listOf(row("a","घर के लिए समय"),row("b","घर  के\nलिए\tसमय"));assertEquals(1,ChecklistDuplicates.groups(r).size)}
 @Test fun emptyAndUniqueHaveNoGroups(){assertTrue(ChecklistDuplicates.groups(emptyList()).isEmpty());assertTrue(ChecklistDuplicates.groups(listOf(row("a","One"))).isEmpty())}
 @Test fun selectionEmptyUnknownOrMixedRefuses(){refuse{view(emptySet())};refuse{view(setOf("no"))};refuse{view(setOf("delivery time","no"))}}
 @Test fun duplicateIdentityBlankTextControlOversizeRefuse(){for(r in listOf(rows+rows[0],listOf(row("","One")),listOf(row("a"," ")),listOf(row("a","A\u0000")),listOf(row("a","a".repeat(251))))){refuse{ChecklistDuplicates.groups(r)}}}
 @Test fun thirtyCopiesConsolidateOneNoTruncation(){val r=(1..30).map{row("$it","Same",it!=30)};val v=ChecklistDuplicates.preview(r,setOf("same"));assertEquals(29,v.removed);assertEquals("1",v.result.single().id);assertFalse(v.result.single().done);refuse{ChecklistDuplicates.groups(r+row("31","Same"))}}
 @Test fun validApplyMatchesAndLeavesInputUntouched(){val v=view();assertEquals(v.result,ChecklistDuplicates.apply(rows,v));assertEquals(5,rows.size);assertTrue(rows[0].done)}
 @Test fun staleTextTickOrderAdditionDeletionRefuse(){val v=view();for(r in listOf(rows.reversed(),rows.drop(1),rows+row("z","New"),rows.map{if(it.id=="x")it.copy(text="Changed")else it},rows.map{if(it.id=="x")it.copy(done=false)else it})){refuse{ChecklistDuplicates.apply(r,v)}}}
 @Test fun tamperedResultsGroupsAndSelectionRefuse(){val v=view();refuse{ChecklistDuplicates.apply(rows,v.copy(result=rows))};refuse{ChecklistDuplicates.apply(rows,v.copy(groups=emptyList()))};refuse{ChecklistDuplicates.apply(rows,v.copy(selected=setOf("rate?")))}}
 @Test fun callerSelectionMutationDoesNotChangePreview(){val s=mutableSetOf("delivery time");val v=ChecklistDuplicates.preview(rows,s);s.clear();assertEquals(setOf("delivery time"),v.selected)}
 private fun saved():List<CallChecklist.Item>{CallChecklist.add(ctx,n,"Delivery time");CallChecklist.add(ctx,n,"Unique");val r=CallChecklist.add(ctx,n,"DELIVERY   TIME");CallChecklist.done(ctx,n,r[0].id,true);CallChecklist.add(ctx,foreign,"Foreign");return CallChecklist.read(ctx,n)}
 @Test fun savedCleanupPreservesForeignAndExactFirstIdentity(){val old=saved();val v=ChecklistDuplicates.preview(old,setOf("delivery time"));val next=CallChecklist.consolidateChecked(ctx,n,v);assertEquals(old[0].id,next[0].id);assertEquals(old[0].text,next[0].text);assertFalse(next[0].done);assertEquals(old[1],next[1]);assertEquals(next,CallChecklist.read(ctx,n));assertEquals("Foreign",CallChecklist.read(ctx,foreign).single().text)}
 @Test fun staleSavedCleanupBytesUnchanged(){val old=saved();val v=ChecklistDuplicates.preview(old,setOf("delivery time"));CallChecklist.done(ctx,n,old[1].id,true);val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();refuse{CallChecklist.consolidateChecked(ctx,n,v)};assertArrayEquals(bytes,f.readBytes())}
 @Test fun foreignChangeBetweenPreviewSavePreserved(){val old=saved();val v=ChecklistDuplicates.preview(old,setOf("delivery time"));CallChecklist.add(ctx,foreign,"New foreign");CallChecklist.consolidateChecked(ctx,n,v);assertEquals(listOf("Foreign","New foreign"),CallChecklist.read(ctx,foreign).map{it.text})}
 @Test fun corruptedStorageNotOverwritten(){val old=saved();val v=ChecklistDuplicates.preview(old,setOf("delivery time"));val f=File(ctx.filesDir,"call_checklists.json");f.writeText("broken");try{CallChecklist.consolidateChecked(ctx,n,v);fail()}catch(_:Exception){};assertEquals("broken",f.readText())}
 @Test fun tamperedSaveBytesUnchanged(){val old=saved();val v=ChecklistDuplicates.preview(old,setOf("delivery time"));val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();refuse{CallChecklist.consolidateChecked(ctx,n,v.copy(result=old))};assertArrayEquals(bytes,f.readBytes())}
 @Test fun secondSaveOldPreviewRefuses(){val old=saved();val v=ChecklistDuplicates.preview(old,setOf("delivery time"));CallChecklist.consolidateChecked(ctx,n,v);val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();refuse{CallChecklist.consolidateChecked(ctx,n,v)};assertArrayEquals(bytes,f.readBytes())}
}
