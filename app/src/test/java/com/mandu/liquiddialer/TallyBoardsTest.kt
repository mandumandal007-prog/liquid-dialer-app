package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class TallyBoardsTest {
 private fun board()=listOf(CallTally.Item("a","Rice bags",12),CallTally.Item("b","Oil tins",7))
 private fun all()=TallyBoards.capture(emptyList(),"Morning",board())
 private fun raw()=TallyBoards.encode(all())
 private fun fail(f:()->Unit){assertThrows(Exception::class.java,f)}
 @Test fun defaultOffEmpty(){assertFalse(LdSettings().tallyBoards);assertEquals(emptyList<TallyBoards.Saved>(),TallyBoards.decode(LdSettings().tallyBoardLibrary))}
 @Test fun captureKeepsCountsOrderNamesFreshIdentity(){val b=all()[0];assertEquals("Morning",b.name);assertEquals(listOf(12L,7L),b.rows.map{it.value});assertEquals(listOf("Rice bags","Oil tins"),b.rows.map{it.label});assertTrue(b.rows.none{it.id in setOf("a","b")})}
 @Test fun unicodeRoundtrip(){val b=TallyBoards.capture(emptyList(),"सुबह वाला",listOf(CallTally.Item("x","चावल bags",123)));assertEquals(b,TallyBoards.decode(TallyBoards.encode(b)))}
 @Test fun sixLimitNoTruncation(){var r=all();repeat(5){r=TallyBoards.capture(r,"Board$it",board())};fail{TallyBoards.capture(r,"Seventh",board())};assertEquals(6,TallyBoards.decode(TallyBoards.encode(r)).size)}
 @Test fun emptyAndOverfullSessionRefused(){fail{TallyBoards.capture(emptyList(),"x",emptyList())};fail{TallyBoards.capture(emptyList(),"x",(0..12).map{CallTally.Item("$it","Item$it",1)})}}
 @Test fun strictNameRules(){for(n in listOf(""," ","x".repeat(33),"A\nB","A\tB"))fail{TallyBoards.capture(emptyList(),n,board())};assertEquals("Work",TallyBoards.capture(emptyList()," Work ",board())[0].name);fail{TallyBoards.capture(all(),"MORNING",board())}}
 @Test fun renamePreservesCountsAndOthers(){val a=TallyBoards.capture(all(),"Evening",board());val b=TallyBoards.rename(a,a[0].id,"Work");assertEquals(a[0].copy(name="Work"),b[0]);assertEquals(a[1],b[1]);fail{TallyBoards.rename(a,a[0].id,"evening")};fail{TallyBoards.rename(a,"missing","x")}}
 @Test fun deleteOnlyOneNoSessionMutation(){val a=TallyBoards.capture(all(),"Evening",board());assertEquals(listOf(a[1]),TallyBoards.remove(a,a[0].id));assertEquals(2,board().size);fail{TallyBoards.remove(a,"missing")}}
 @Test fun malformedAndOversizeLocked(){for(s in listOf("null","[]","{}","broken",raw()+"x","x".repeat(40001)))fail{TallyBoards.decode(s)}}
 @Test fun versionStrict(){for(v in listOf(2,"1",true,1.5)){val o=org.json.JSONObject(raw()).put("version",v);fail{TallyBoards.decode(o.toString())}}}
 @Test fun quantityTypeBoundsStrict(){for(v in listOf(-1,1000000001L,"12",1.5,true)){val o=org.json.JSONObject(raw());o.getJSONArray("boards").getJSONObject(0).getJSONArray("rows").getJSONObject(0).put("value",v);fail{TallyBoards.decode(o.toString())}}}
 @Test fun zeroAndMaxSurvive(){val a=TallyBoards.capture(emptyList(),"Bounds",listOf(CallTally.Item("a","Zero",0),CallTally.Item("b","Max",CallTally.MAX_VALUE)));assertEquals(a,TallyBoards.decode(TallyBoards.encode(a)))}
 @Test fun duplicateBoardIdsAndNamesRefused(){val b=all()[0];fail{TallyBoards.encode(listOf(b,b))};fail{TallyBoards.encode(listOf(b,b.copy(id="new")))}}
 @Test fun duplicateCounterIdsNamesAndBadLabelsRefused(){val b=all()[0];for(rows in listOf(listOf(b.rows[0],b.rows[0]),listOf(b.rows[0],b.rows[0].copy(id="new")),listOf(b.rows[0].copy(label=" bad "))))fail{TallyBoards.encode(listOf(b.copy(rows=rows)))}}
 @Test fun planReplacesDoesNotAppendAndFreshIds(){val a=all();val before=listOf(CallTally.Item("z","Old",200));val p=TallyBoards.plan(before,TallyBoards.encode(a),a[0].id);assertEquals(before,p.before);assertEquals(listOf(12L,7L),p.after.map{it.value});assertEquals(2,p.after.size);assertTrue(p.after.none{r->a[0].rows.any{it.id==r.id}});assertEquals(p.after,TallyBoards.apply(p,before,TallyBoards.encode(a)))}
 @Test fun emptySessionAccepted(){val a=all();val p=TallyBoards.plan(emptyList(),TallyBoards.encode(a),a[0].id);assertEquals(2,p.after.size)}
 @Test fun staleSessionAndLibraryRefused(){val a=all();val lib=TallyBoards.encode(a);val p=TallyBoards.plan(board(),lib,a[0].id);fail{TallyBoards.apply(p,board().map{it.copy(value=0)},lib)};fail{TallyBoards.apply(p,board(),TallyBoards.encode(TallyBoards.rename(a,a[0].id,"Later")))}}
 @Test fun tamperedPlanRefused(){val a=all();val lib=TallyBoards.encode(a);val p=TallyBoards.plan(board(),lib,a[0].id);fail{TallyBoards.apply(p.copy(after=emptyList()),board(),lib)};fail{TallyBoards.apply(p.copy(token="bad"),board(),lib)}}
 @Test fun boardUndoRedoRestoresSingleReplacement(){val original=listOf(CallTally.Item("old","Existing",99));val b=CallTally.Board(original);val a=all();val p=TallyBoards.plan(b.rows,TallyBoards.encode(a),a[0].id);b.change(TallyBoards.apply(p,b.rows,TallyBoards.encode(a)));assertEquals(1,b.undoCount);b.undo();assertEquals(original,b.rows);b.redo();assertEquals(p.after,b.rows)}
 @Test fun separateSavedSessionMutationDoesNotUpdateLibrary(){val a=all();val lib=TallyBoards.encode(a);val p=TallyBoards.plan(emptyList(),lib,a[0].id);val changed=CallTally.adjust(p.after,p.after[0].id,1);assertEquals(13L,changed[0].value);assertEquals(12L,TallyBoards.decode(lib)[0].rows[0].value)}
 @Test fun finderRoute(){assertTrue(SettingsFinder.search("saved board snapshot quantity").any{it.route=="tallyboards"})}
}
