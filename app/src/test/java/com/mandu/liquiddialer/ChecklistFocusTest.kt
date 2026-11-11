package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class ChecklistFocusTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+44 2025550123";private val other="+1 2025550123"
 @Before fun clean(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete()}
 @Test fun selectionAndNavigationArePureBoundedAndStable(){val a=CallChecklist.Item("a","One");val b=CallChecklist.Item("b","Two",true);val c=CallChecklist.Item("c","Three");val rows=listOf(a,b,c);assertEquals(listOf(a,c),ChecklistFocus.visible(rows,true));assertEquals(rows,ChecklistFocus.visible(rows,false));assertEquals(a,ChecklistFocus.selected(rows,"missing"));assertEquals(c,ChecklistFocus.selected(rows,"c"));assertEquals("a",ChecklistFocus.move(rows,"a",-1));assertEquals("c",ChecklistFocus.move(rows,"c",1));assertEquals("b",ChecklistFocus.move(rows,"a",1));assertEquals(rows,listOf(a,b,c));assertNull(ChecklistFocus.selected(emptyList(),null));assertNull(ChecklistFocus.move(emptyList(),null,1))}
 @Test fun afterMarkKeepsNextSamePositionOrPreviousAtEnd(){val rows=(0..2).map{CallChecklist.Item("$it","Topic $it")};assertEquals("1",ChecklistFocus.afterMark(rows,rows.drop(1),"0"));assertEquals("2",ChecklistFocus.afterMark(rows,listOf(rows[0],rows[2]),"1"));assertEquals("1",ChecklistFocus.afterMark(rows,rows.dropLast(1),"2"));assertEquals("1",ChecklistFocus.afterMark(rows,rows,"1"));assertNull(ChecklistFocus.afterMark(rows,emptyList(),"0"))}
 @Test fun checkedMarkAndUndoPreserveOtherIdentityAndSavedTexts(){val original=CallChecklist.add(ctx,n,"घर 👪\nLong topic").single();val otherRows=CallChecklist.add(ctx,other,"Other");val marked=CallChecklist.markChecked(ctx,n,original,true).single();assertEquals(original.copy(done=true),marked);assertEquals(original,CallChecklist.markChecked(ctx,n,marked,false).single());assertEquals(otherRows,CallChecklist.read(ctx,other));assertEquals(original,CallChecklist.read(ctx,"0044 2025550123").single())}
 @Test fun staleEditedOrTickedOrRemovedTopicNeverOverwritten(){val o=CallChecklist.add(ctx,n,"Keep").single();CallChecklist.edit(ctx,n,o.id,"Edited");var bytes=File(ctx.filesDir,"call_checklists.json").readBytes();try{CallChecklist.markChecked(ctx,n,o,true);fail()}catch(_:IllegalArgumentException){};assertArrayEquals(bytes,File(ctx.filesDir,"call_checklists.json").readBytes());val current=CallChecklist.read(ctx,n).single();CallChecklist.done(ctx,n,current.id,true);bytes=File(ctx.filesDir,"call_checklists.json").readBytes();try{CallChecklist.markChecked(ctx,n,current,false);fail()}catch(_:IllegalArgumentException){};assertArrayEquals(bytes,File(ctx.filesDir,"call_checklists.json").readBytes());CallChecklist.remove(ctx,n,current.id);try{CallChecklist.markChecked(ctx,n,current,true);fail()}catch(_:IllegalArgumentException){};assertTrue(CallChecklist.read(ctx,n).isEmpty())}
 @Test fun corruptStorageNeverChangesAndUnrelatedAddsSurvive(){val o=CallChecklist.add(ctx,n,"One").single();CallChecklist.add(ctx,n,"Two");assertEquals(2,CallChecklist.markChecked(ctx,n,o,true).size);val f=File(ctx.filesDir,"call_checklists.json");f.writeText("broken");try{CallChecklist.markChecked(ctx,n,o,false);fail()}catch(_:Exception){};assertEquals("broken",f.readText())}
}
