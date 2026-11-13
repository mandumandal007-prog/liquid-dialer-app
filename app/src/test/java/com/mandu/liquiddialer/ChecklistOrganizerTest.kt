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
class ChecklistOrganizerTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val number="+919876543210"
 private val rows=listOf(CallChecklist.Item("a","Confirm delivery",false),CallChecklist.Item("b","Ask rate",true),CallChecklist.Item("c","घर की delivery",false))
 @Before fun clear(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete()}
 private fun seed():List<CallChecklist.Item>{CallChecklist.add(ctx,number,"Confirm delivery");CallChecklist.add(ctx,number,"Ask rate");return CallChecklist.add(ctx,number,"घर की delivery")}
 @Test fun literalUnicodeAndCaseSearch(){assertEquals(listOf(rows[0],rows[2]),ChecklistOrganizer.visible(rows,"DELIVERY",ChecklistOrganizer.Filter.ALL));assertEquals(listOf(rows[2]),ChecklistOrganizer.visible(rows,"घर",ChecklistOrganizer.Filter.ALL));assertTrue(ChecklistOrganizer.visible(rows,".*",ChecklistOrganizer.Filter.ALL).isEmpty())}
 @Test fun statusFiltersPreserveOrder(){assertEquals(listOf(rows[0],rows[2]),ChecklistOrganizer.visible(rows,"",ChecklistOrganizer.Filter.PENDING));assertEquals(listOf(rows[1]),ChecklistOrganizer.visible(rows,"",ChecklistOrganizer.Filter.DONE))}
 @Test fun blankAndTrimmedQuery(){assertEquals(rows,ChecklistOrganizer.visible(rows,"  ",ChecklistOrganizer.Filter.ALL));assertEquals(listOf(rows[1]),ChecklistOrganizer.visible(rows," rate ",ChecklistOrganizer.Filter.DONE))}
 @Test fun markSelectedOnlyKeepsIdsTextAndOrder(){val n=ChecklistOrganizer.mark(rows,setOf("a","c"),true);assertTrue(n.all{it.done});assertEquals(rows.map{it.id to it.text},n.map{it.id to it.text});assertFalse(rows.first().done)}
 @Test fun pendingAndUnknownSelection(){assertEquals(rows.map{it.copy(done=false)},ChecklistOrganizer.mark(rows,setOf("b","missing"),false));assertEquals(rows,ChecklistOrganizer.mark(rows,emptySet(),true))}
 @Test fun moveUpAndDownPreservesData(){assertEquals(listOf(rows[1],rows[0],rows[2]),ChecklistOrganizer.move(rows,"b",-1));assertEquals(listOf(rows[0],rows[2],rows[1]),ChecklistOrganizer.move(rows,"b",1))}
 @Test fun boundaryMoveNoOp(){assertEquals(rows,ChecklistOrganizer.move(rows,"a",-1));assertEquals(rows,ChecklistOrganizer.move(rows,"c",1));assertEquals(rows,ChecklistOrganizer.move(rows,"missing",1))}
 @Test fun moveStepStrict(){assertTrue(runCatching{ChecklistOrganizer.move(rows,"b",4)}.isFailure)}
 @Test fun noAddDeleteTextOrDuplicateIds(){val invalid=listOf(rows.drop(1),rows+CallChecklist.Item("d","new"),rows.mapIndexed{i,r->if(i==0)r.copy(text="changed")else r},listOf(rows[0],rows[0],rows[2]));invalid.forEach{assertTrue(runCatching{ChecklistOrganizer.validate(rows,it)}.isFailure)}}
 @Test fun draftUndoOneOperationAtTimeAndNoOp(){val d=ChecklistOrganizer.Draft(rows);assertFalse(d.change(rows));assertFalse(d.canUndo);d.change(ChecklistOrganizer.mark(d.rows,setOf("a"),true));val marked=d.rows;d.change(ChecklistOrganizer.move(d.rows,"b",-1));d.undo();assertEquals(marked,d.rows);d.undo();assertEquals(rows,d.rows);assertFalse(d.canUndo)}
 @Test fun emptyDraftAndBoundedHistory(){val empty=ChecklistOrganizer.Draft(emptyList());assertFalse(empty.change(emptyList()));val d=ChecklistOrganizer.Draft(rows);repeat(60){d.change(ChecklistOrganizer.mark(d.rows,setOf("a"),it%2==0))};repeat(40){assertTrue(d.canUndo);d.undo()};assertFalse(d.canUndo)}
 @Test fun saveReorderMarksPersistOtherExactNumberUntouched(){val before=seed();val other=CallChecklist.add(ctx,"+449876543210","Foreign only");val n=ChecklistOrganizer.mark(before.reversed(),setOf(before[0].id),true);assertEquals(n,CallChecklist.organizeChecked(ctx,number,before,n));assertEquals(n,CallChecklist.read(ctx,number));assertEquals(other,CallChecklist.read(ctx,"+449876543210"));assertTrue(CallChecklist.read(ctx,"+119876543210").isEmpty())}
 @Test fun staleChangedTextRejectedPreservesBytes(){val b=seed();CallChecklist.edit(ctx,number,b[0].id,"Changed elsewhere");val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();assertTrue(runCatching{CallChecklist.organizeChecked(ctx,number,b,b.reversed())}.isFailure);assertArrayEquals(bytes,f.readBytes())}
 @Test fun staleTickOrAdditionRejected(){val b=seed();CallChecklist.done(ctx,number,b[0].id,true);assertTrue(runCatching{CallChecklist.organizeChecked(ctx,number,b,b.reversed())}.isFailure);val c=CallChecklist.read(ctx,number);CallChecklist.add(ctx,number,"new elsewhere");assertTrue(runCatching{CallChecklist.organizeChecked(ctx,number,c,c.reversed())}.isFailure)}
 @Test fun invalidSaveKeepsOriginalFile(){val b=seed();val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();assertTrue(runCatching{CallChecklist.organizeChecked(ctx,number,b,b.dropLast(1))}.isFailure);assertArrayEquals(bytes,f.readBytes())}
 @Test fun corruptFileNeverOverwritten(){seed();val f=File(ctx.filesDir,"call_checklists.json");f.writeText("broken");assertTrue(runCatching{CallChecklist.organizeChecked(ctx,number,emptyList(),emptyList())}.isFailure);assertEquals("broken",f.readText())}
 @Test fun noOpSaveDoesNotRewriteOrCreateEmpty(){assertTrue(CallChecklist.organizeChecked(ctx,number,emptyList(),emptyList()).isEmpty());assertFalse(File(ctx.filesDir,"call_checklists.json").exists());val b=seed();val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();CallChecklist.organizeChecked(ctx,number,b,b);assertArrayEquals(bytes,f.readBytes())}
 @Test fun formattedEquivalentKeepsFullIdentity(){val b=seed();CallChecklist.organizeChecked(ctx,"+91 98765 43210",b,b.reversed());assertEquals(b.reversed(),CallChecklist.read(ctx,number))}
}
