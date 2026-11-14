package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class ChecklistLinesTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210"
 private val existing=listOf(CallChecklist.Item("a","Confirm delivery",true))
 @Before fun clean(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete()}
 @Test fun actualLinesPreservePunctuationAndBullets(){val p=ChecklistLines.preview(emptyList(),"- First; second?\n2. Next, details\nLast sentence.");assertEquals(listOf("- First; second?","2. Next, details","Last sentence."),p.added.map{it.text})}
 @Test fun blankLinesIgnoredButSourceLineNumbersPreserved(){val p=ChecklistLines.preview(emptyList(),"\n  First  \n\t\nSecond\n");assertEquals(listOf(2,4),p.added.map{it.line});assertEquals(listOf("First","Second"),p.added.map{it.text})}
 @Test fun windowsAndOldMacLineEndings(){assertEquals(listOf("A","B","C"),ChecklistLines.preview(emptyList(),"A\r\nB\rC").added.map{it.text})}
 @Test fun duplicateClassificationNormalizedButTextNotCollapsed(){val p=ChecklistLines.preview(existing," CONFIRM   delivery\nNew   words\nnew words\nNew\twords");assertEquals(listOf(ChecklistLines.Kind.EXISTING,ChecklistLines.Kind.NEW,ChecklistLines.Kind.REPEATED,ChecklistLines.Kind.REPEATED),p.lines.map{it.kind});assertEquals("New   words",p.added.single().text)}
 @Test fun existingDuplicatesAllClassifiedExisting(){val p=ChecklistLines.preview(existing,"confirm delivery\nCONFIRM DELIVERY");assertEquals(2,p.skipped);assertTrue(p.lines.all{it.kind==ChecklistLines.Kind.EXISTING})}
 @Test fun unicodeAndLiteralRegexNotInterpreted(){val p=ChecklistLines.preview(emptyList(),"घर का समय\n.*[a]+?\n💡 next");assertEquals(3,p.added.size);assertEquals(".*[a]+?",p.added[1].text)}
 @Test fun emptyAndOnlyWhitespaceRefused(){for(s in listOf(""," ","\n\t\r\n"))assertTrue(runCatching{ChecklistLines.preview(emptyList(),s)}.isFailure)}
 @Test fun allInputBoundariesRefuseWithoutTruncation(){assertEquals(1,ChecklistLines.preview(emptyList(),"x".repeat(250)).added.size);assertTrue(runCatching{ChecklistLines.preview(emptyList(),"x".repeat(251))}.isFailure);assertTrue(runCatching{ChecklistLines.preview(emptyList(),"x".repeat(16001))}.isFailure);assertEquals(60,ChecklistLines.preview(emptyList(),(1..60).joinToString("\n"){"T$it"}).added.size);assertTrue(runCatching{ChecklistLines.preview(emptyList(),(1..61).joinToString("\n"){"T$it"})}.isFailure)}
 @Test fun controlCharactersRefused(){for(c in listOf('\u0000','\u0001','\u007f'))assertTrue(runCatching{ChecklistLines.preview(emptyList(),"First\nBad${c}topic")}.isFailure)}
 @Test fun selectSubsetKeepsInputOrder(){val p=ChecklistLines.preview(existing,"A\nB\nC");assertEquals(listOf("A","C"),ChecklistLines.selected(p,setOf(3,1)))}
 @Test fun skippedOrUnknownLineCannotBeSelected(){val p=ChecklistLines.preview(existing,"Confirm delivery\nA\na");for(s in listOf(emptySet(),setOf(1),setOf(3),setOf(4),setOf(2,3)))assertTrue(runCatching{ChecklistLines.selected(p,s)}.isFailure)}
 @Test fun capacityCanBeResolvedByDeselectingNotSilentTruncation(){val source=List(29){CallChecklist.Item("$it","Old $it")};val p=ChecklistLines.preview(source,"A\nB");assertTrue(runCatching{ChecklistLines.selected(p,setOf(1,2))}.isFailure);assertEquals(listOf("B"),ChecklistLines.selected(p,setOf(2)))}
 @Test fun fullChecklistCannotAppend(){val p=ChecklistLines.preview(List(30){CallChecklist.Item("$it","Old $it")},"New");assertTrue(runCatching{ChecklistLines.selected(p,setOf(1))}.isFailure)}
 @Test fun appendPreservesOldIdsOrderAndTicksNewPending(){val a=CallChecklist.add(ctx,n,"Old");val before=CallChecklist.done(ctx,n,a.single().id,true);val next=CallChecklist.appendChecked(ctx,n,before,listOf("A","B"));assertEquals(before,next.take(1));assertEquals(listOf("A","B"),next.drop(1).map{it.text});assertTrue(next.drop(1).none{it.done});assertEquals(3,next.map{it.id}.distinct().size);assertEquals(next,CallChecklist.read(ctx,n))}
 @Test fun staleTextTickOrderAndAddedRowsRefused(){val a=CallChecklist.add(ctx,n,"Old");CallChecklist.edit(ctx,n,a[0].id,"Newer");val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();assertTrue(runCatching{CallChecklist.appendChecked(ctx,n,a,listOf("A"))}.isFailure);assertArrayEquals(bytes,f.readBytes());val b=CallChecklist.read(ctx,n);CallChecklist.done(ctx,n,b[0].id,true);assertTrue(runCatching{CallChecklist.appendChecked(ctx,n,b,listOf("A"))}.isFailure)}
 @Test fun otherNumbersCanChangeWithoutOverwrite(){val a=CallChecklist.add(ctx,n,"Local");val foreign=CallChecklist.add(ctx,"+449876543210","Foreign");CallChecklist.appendChecked(ctx,n,a,listOf("New"));assertEquals(foreign,CallChecklist.read(ctx,"+449876543210"))}
 @Test fun duplicateAndInvalidAppendRefuseAtomic(){val a=CallChecklist.add(ctx,n,"Old");for(v in listOf(listOf("old"),listOf("A","a"),listOf("A","x".repeat(251)),emptyList())){val bytes=File(ctx.filesDir,"call_checklists.json").readBytes();assertTrue(runCatching{CallChecklist.appendChecked(ctx,n,a,v)}.isFailure);assertArrayEquals(bytes,File(ctx.filesDir,"call_checklists.json").readBytes())}}
 @Test fun capacitySaveRefusesWholeBatch(){repeat(29){CallChecklist.add(ctx,n,"Old $it")};val a=CallChecklist.read(ctx,n);assertTrue(runCatching{CallChecklist.appendChecked(ctx,n,a,listOf("A","B"))}.isFailure);assertEquals(a,CallChecklist.read(ctx,n));assertEquals(30,CallChecklist.appendChecked(ctx,n,a,listOf("A")).size)}
 @Test fun corruptReadAndWritePreserveBytes(){val f=File(ctx.filesDir,"call_checklists.json");f.writeText("broken");assertTrue(runCatching{CallChecklist.read(ctx,n)}.isFailure);assertTrue(runCatching{CallChecklist.appendChecked(ctx,n,emptyList(),listOf("A"))}.isFailure);assertEquals("broken",f.readText())}
 @Test fun noPreviewFileEffectsAndInternationalNumbersStaySeparate(){val a=CallChecklist.add(ctx,n,"India");val f=File(ctx.filesDir,"call_checklists.json");val b=f.readBytes();ChecklistLines.preview(a,"A\nB");assertArrayEquals(b,f.readBytes());CallChecklist.appendChecked(ctx,"+449876543210",emptyList(),listOf("UK"));assertEquals(a,CallChecklist.read(ctx,n))}
}
