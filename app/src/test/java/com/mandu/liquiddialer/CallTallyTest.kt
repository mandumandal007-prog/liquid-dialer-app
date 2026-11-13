package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.CallTally as T
import org.junit.Test
import org.junit.Assert.*
class CallTallyTest {
 private fun one(value:Long=0)=listOf(T.Item("a","Rice bags",value))
 @Test fun labelTrimsAndCollapsesWhitespace(){assertEquals("Rice bags",T.label("  Rice   bags  "));assertEquals("चावल बोरी",T.label("चावल बोरी"))}
 @Test fun labelsRejectBlankLongAndControl(){for(s in listOf("", " ","a".repeat(61),"a\u0000b"))assertTrue(runCatching{T.label(s)}.isFailure)}
 @Test fun valuesStrictAndBounded(){assertEquals(0L,T.whole("0"));assertEquals(1000000000L,T.whole("1000000000"));for(s in listOf("-1","+1","1.0","1,000"," 1","1 ","1000000001","999999999999","१२"))assertTrue(s,runCatching{T.whole(s)}.isFailure)}
 @Test fun stepStrictPositiveAndBounded(){assertEquals(1L,T.step("1"));assertEquals(1000000L,T.step("1000000"));for(s in listOf("0","-1","1000001","1.5",""))assertTrue(runCatching{T.step(s)}.isFailure)}
 @Test fun addPreservesOriginalAndStableIds(){val original=one(5);val n=T.add(original,"Sugar bags","7","b");assertEquals(1,original.size);assertEquals(listOf("a","b"),n.map{it.id});assertEquals(12L,T.total(n))}
 @Test fun duplicateNamesRejectedAfterNormalization(){val rows=one();for(s in listOf("rice BAGS"," Rice   bags "))assertTrue(runCatching{T.add(rows,s,"1","b")}.isFailure)}
 @Test fun maxRowsAndUniqueIds(){var rows=emptyList<T.Item>();repeat(12){rows=T.add(rows,"Item $it","1","id$it")};assertEquals(12,rows.size);assertTrue(runCatching{T.add(rows,"13","1")}.isFailure);assertTrue(runCatching{T.add(one(),"Sugar","1","a")}.isFailure)}
 @Test fun editKeepsOrderAndId(){val rows=T.add(one(),"Sugar","5","b");val n=T.edit(rows,"a","Rice sacks","20");assertEquals("a",n.first().id);assertEquals(rows[1],n[1]);assertEquals(20L,n.first().value)}
 @Test fun editDuplicateOrMissingRejected(){val rows=T.add(one(),"Sugar","5","b");assertTrue(runCatching{T.edit(rows,"a","sugar","7")}.isFailure);assertTrue(runCatching{T.edit(rows,"missing","New","7")}.isFailure)}
 @Test fun addSubtractDoNotClamp(){assertEquals(15L,T.adjust(one(10),"a",5).first().value);assertEquals(0L,T.adjust(one(10),"a",-10).first().value);assertTrue(runCatching{T.adjust(one(10),"a",-11)}.isFailure);assertTrue(runCatching{T.adjust(one(T.MAX_VALUE),"a",1)}.isFailure)}
 @Test fun invalidDeltaAndIdentityRejected(){for(d in listOf(0L,1000001L,-1000001L,Long.MAX_VALUE))assertTrue(runCatching{T.adjust(one(),"a",d)}.isFailure);assertTrue(runCatching{T.adjust(one(),"no",1)}.isFailure)}
 @Test fun totalsUseLongAndAllTwelveRows(){val rows=(0..11).map{T.Item("$it","Counter $it",T.MAX_VALUE)};assertEquals(12000000000L,T.total(rows))}
 @Test fun zeroKeepsNamesIdsAndOrder(){val rows=T.add(one(5),"Sugar","7","b");val z=T.zero(rows);assertEquals(rows.map{it.label},z.map{it.label});assertEquals(rows.map{it.id},z.map{it.id});assertEquals(0L,T.total(z));assertEquals(12L,T.total(rows))}
 @Test fun removeExactItemAndMissingRejected(){val rows=T.add(one(5),"Sugar","7","b");assertEquals(listOf(rows[1]),T.remove(rows,"a"));assertTrue(runCatching{T.remove(rows,"missing")}.isFailure)}
 @Test fun validateRejectsMalformedRawRows(){for(rows in listOf(listOf(T.Item("","A",0)),listOf(T.Item("a"," A ",0)),listOf(T.Item("a","A",-1)),listOf(T.Item("a","A",T.MAX_VALUE+1))))assertTrue(runCatching{T.Board(rows)}.isFailure)}
 @Test fun boardUndoRedoFullSnapshot(){val b=T.Board(one(10));b.change(T.edit(b.rows,"a","New bags","20"));b.change(T.remove(b.rows,"a"));assertTrue(b.rows.isEmpty());b.undo();assertEquals("New bags",b.rows.first().label);b.undo();assertEquals(one(10),b.rows);b.redo();b.redo();assertTrue(b.rows.isEmpty());assertFalse(b.canRedo)}
 @Test fun newEditClearsRedoButNoOpDoesNot(){val b=T.Board(one());b.change(T.adjust(b.rows,"a",1));b.undo();assertTrue(b.canRedo);assertFalse(b.change(b.rows));assertTrue(b.canRedo);b.change(T.adjust(b.rows,"a",5));assertFalse(b.canRedo);assertEquals(5L,b.rows.first().value)}
 @Test fun undoBoundFortyAndRedoRestores(){val b=T.Board(one());repeat(45){b.change(T.adjust(b.rows,"a",1))};assertEquals(40,b.undoCount);repeat(45){b.undo()};assertEquals(5L,b.rows.first().value);assertEquals(40,b.redoCount);repeat(45){b.redo()};assertEquals(45L,b.rows.first().value);assertEquals(40,b.undoCount)}
 @Test fun failedChangePreservesBoardAndHistory(){val b=T.Board(one(1));b.change(T.adjust(b.rows,"a",1));val before=b.rows;assertTrue(runCatching{b.change(listOf(T.Item("a","Bad",-1)))}.isFailure);assertEquals(before,b.rows);assertEquals(1,b.undoCount)}
 @Test fun boardsAndSnapshotsIndependent(){val initial=one(3).toMutableList();val b=T.Board(initial);initial.clear();assertEquals(3L,b.rows.first().value);val snapshot=b.rows;b.change(T.adjust(b.rows,"a",2));assertEquals(3L,snapshot.first().value);assertTrue(T.Board().rows.isEmpty())}
}
