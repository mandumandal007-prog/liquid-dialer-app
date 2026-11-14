package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class TallyTransferTest {
 private val rows=listOf(CallTally.Item("a","Rice bags",24),CallTally.Item("b","Packed bags",7),CallTally.Item("c","Delivered bags",5),CallTally.Item("d","Other kg",9))
 private fun p(raw:Map<String,String> = mapOf("b" to "6","c" to "3"))=TallyTransfer.preview(rows,"a",raw)
 @Test fun oneDestinationExact(){val x=p(mapOf("b" to "6"));assertEquals(listOf(18L,13L,5L,9L),x.after.map{it.value});assertEquals(6L,x.moved)}
 @Test fun multipleDestinationSumNotPerTarget(){val x=p();assertEquals(listOf(15L,13L,8L,9L),x.after.map{it.value});assertEquals(9L,x.moved)}
 @Test fun rawTotalConserved(){for(a in 1..20)for(b in 1..(24-a)){val x=p(mapOf("b" to "$a","c" to "$b"));assertEquals(CallTally.total(rows),CallTally.total(x.after))}}
 @Test fun preserveNamesIdsOrderAndUnchangedRow(){val x=p();assertEquals(rows.map{it.id},x.after.map{it.id});assertEquals(rows.map{it.label},x.after.map{it.label});assertEquals(rows.last(),x.after.last());assertEquals(rows,x.before)}
 @Test fun exactSourceExhaustionAllowed(){assertEquals(0L,p(mapOf("b" to "24")).after.first().value)}
 @Test fun blankDestinationsOmitted(){val x=p(mapOf("b" to "2","c" to "","d" to ""));assertEquals(mapOf("b" to 2L),x.allocations);assertEquals(rows[2],x.after[2])}
 @Test fun stableBoardOrderNotMapInsertion(){assertEquals(p(),p(linkedMapOf("c" to "3","b" to "6")))}
 @Test fun cannotTransferToSelfEvenBlank(){for(v in listOf("","1"))assertTrue(runCatching{p(mapOf("a" to v,"b" to "1"))}.isFailure)}
 @Test fun unknownOrMissingSourceRefused(){for(id in listOf("","x"))assertTrue(runCatching{TallyTransfer.preview(rows,id,mapOf("b" to "1"))}.isFailure)}
 @Test fun unknownDestinationRefusedEvenBlank(){assertTrue(runCatching{p(mapOf("x" to "","b" to "1"))}.isFailure)}
 @Test fun noDestinationsOrOnlyBlankRefused(){for(m in listOf(emptyMap(),mapOf("b" to "")))assertTrue(runCatching{p(m)}.isFailure)}
 @Test fun zeroAndMalformedRefusedWhole(){for(v in listOf("0","-1","+1"," 1","1 "," ","1.5","1,000","1e2","१","99999999999","1000000001"))assertTrue(v,runCatching{p(mapOf("b" to "1","c" to v))}.isFailure)}
 @Test fun shortageRefusesWholeWithoutClamping(){assertTrue(runCatching{p(mapOf("b" to "23","c" to "2"))}.isFailure);assertEquals(24L,rows.first().value)}
 @Test fun anyTargetOverflowRefusesWhole(){val r=rows.map{if(it.id=="c")it.copy(value=CallTally.MAX_VALUE)else it};assertTrue(runCatching{TallyTransfer.preview(r,"a",mapOf("b" to "1","c" to "1"))}.isFailure);assertEquals(7L,r[1].value)}
 @Test fun targetExactUpperBoundAllowed(){val r=rows.map{if(it.id=="b")it.copy(value=CallTally.MAX_VALUE-2)else it};val x=TallyTransfer.preview(r,"a",mapOf("b" to "2"));assertEquals(CallTally.MAX_VALUE,x.after[1].value)}
 @Test fun zeroSourceCannotMove(){assertTrue(runCatching{TallyTransfer.preview(rows.map{if(it.id=="a")it.copy(value=0)else it},"a",mapOf("b" to "1"))}.isFailure)}
 @Test fun maximumBoardAndLongSumCannotWrap(){val r=(0..11).map{CallTally.Item("$it","Bags $it",CallTally.MAX_VALUE)};assertTrue(runCatching{TallyTransfer.preview(r,"0",(1..11).associate{"$it" to "1000000000"})}.isFailure);assertEquals(12_000_000_000L,CallTally.total(r))}
 @Test fun invalidUnselectedBoardRefused(){assertTrue(runCatching{TallyTransfer.preview(rows+rows.last(),"a",mapOf("b" to "1"))}.isFailure)}
 @Test fun staleAnyValueLabelOrderIdentityRefused(){val x=p();for(r in listOf(rows.reversed(),rows.dropLast(1),rows.map{if(it.id=="d")it.copy(label="Changed kg")else it},rows.map{if(it.id=="a")it.copy(value=25)else it},rows.map{if(it.id=="b")it.copy(id="new")else it}))assertTrue(runCatching{TallyTransfer.apply(r,x)}.isFailure);assertEquals(x.after,TallyTransfer.apply(rows,x))}
 @Test fun tamperedPreviewAfterOrAllocationsRefused(){val x=p();for(t in listOf(x.copy(after=x.after.map{it.copy(value=0)}),x.copy(allocations=mapOf("b" to 1L)),x.copy(source="b")))assertTrue(runCatching{TallyTransfer.apply(rows,t)}.isFailure)}
 @Test fun oneUndoRedoAndNewEditClearsRedo(){val b=CallTally.Board(rows);val x=p();assertTrue(b.change(TallyTransfer.apply(b.rows,x)));assertEquals(1,b.undoCount);b.undo();assertEquals(rows,b.rows);b.redo();assertEquals(x.after,b.rows);b.undo();b.change(p(mapOf("b" to "2")).after);assertFalse(b.canRedo)}
}
