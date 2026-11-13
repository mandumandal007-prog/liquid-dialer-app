package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class TallyBatchTest {
 private val rows=listOf(CallTally.Item("a","Rice bags",12),CallTally.Item("b","Sugar bags",7),CallTally.Item("c","Other kg",50))
 private fun p(op:TallyBatch.Operation,raw:String="3",selected:Set<String> = setOf("a","b"))=TallyBatch.preview(rows,selected,op,raw)
 @Test fun addPerCounterOnlySelected(){val x=p(TallyBatch.Operation.ADD);assertEquals(listOf(15L,10L,50L),x.after.map{it.value});assertEquals(rows,x.before);assertEquals(2,x.changed)}
 @Test fun subtractExact(){assertEquals(listOf(9L,4L,50L),p(TallyBatch.Operation.SUBTRACT).after.map{it.value})}
 @Test fun setExactNotSelectionTotal(){assertEquals(listOf(3L,3L,50L),p(TallyBatch.Operation.SET).after.map{it.value})}
 @Test fun multiplyExactNotSelectionTotal(){assertEquals(listOf(36L,21L,50L),p(TallyBatch.Operation.MULTIPLY).after.map{it.value})}
 @Test fun namesIdsOrderAndUnselectedPreserved(){for(op in TallyBatch.Operation.entries){val x=p(op);assertEquals(rows.map{it.id},x.after.map{it.id});assertEquals(rows.map{it.label},x.after.map{it.label});assertEquals(rows.last(),x.after.last())}}
 @Test fun zeroOperandsAllowedNoFalseChange(){for(op in listOf(TallyBatch.Operation.ADD,TallyBatch.Operation.SUBTRACT)){val x=p(op,"0");assertEquals(0,x.changed);assertEquals(rows,TallyBatch.apply(rows,x))};for(op in listOf(TallyBatch.Operation.SET,TallyBatch.Operation.MULTIPLY))assertEquals(listOf(0L,0L,50L),p(op,"0").after.map{it.value})}
 @Test fun multiplierOneNoOp(){assertEquals(0,p(TallyBatch.Operation.MULTIPLY,"1").changed)}
 @Test fun exactUpperBoundary(){val a=rows.map{it.copy(value=500_000_000)};for(op in listOf(TallyBatch.Operation.ADD,TallyBatch.Operation.MULTIPLY)){val n=if(op==TallyBatch.Operation.ADD)"500000000"else"2";assertTrue(TallyBatch.preview(a,a.map{it.id}.toSet(),op,n).after.all{it.value==1_000_000_000L})}}
 @Test fun exactLowerBoundary(){assertEquals(0,p(TallyBatch.Operation.SUBTRACT,"12",setOf("a")).after.first().value)}
 @Test fun overflowAnyCounterRefusesWholeBatch(){val a=rows.map{if(it.id=="b")it.copy(value=CallTally.MAX_VALUE)else it};assertTrue(runCatching{TallyBatch.preview(a,setOf("a","b"),TallyBatch.Operation.ADD,"1")}.isFailure);assertEquals(12,rows.first().value)}
 @Test fun underflowAnyCounterRefusesWholeBatch(){assertTrue(runCatching{p(TallyBatch.Operation.SUBTRACT,"8")}.isFailure);assertEquals(7,rows[1].value)}
 @Test fun multiplyOverflowRefusedBeforeReturn(){val x=runCatching{p(TallyBatch.Operation.MULTIPLY,"1000000",setOf("c"))};assertTrue(x.isSuccess);val huge=listOf(rows.first().copy(value=1_000_000_000));assertTrue(runCatching{TallyBatch.preview(huge,setOf("a"),TallyBatch.Operation.MULTIPLY,"1000000")}.isFailure)}
 @Test fun malformedNumbersRejected(){for(raw in listOf("","-1","+1"," 2","2 ","1.0","1,000","1e3","१२","99999999999"))for(op in TallyBatch.Operation.entries)assertTrue(raw,runCatching{p(op,raw)}.isFailure)}
 @Test fun operandLimitsByOperation(){assertTrue(runCatching{p(TallyBatch.Operation.MULTIPLY,"1000001")}.isFailure);assertEquals(1_000_000_000L,p(TallyBatch.Operation.SET,"1000000000").after.first().value);assertTrue(runCatching{p(TallyBatch.Operation.SET,"1000000001")}.isFailure)}
 @Test fun emptyUnknownAndMixedSelectionsRefused(){for(sel in listOf(emptySet(),setOf("missing"),setOf("a","missing")))assertTrue(runCatching{p(TallyBatch.Operation.ADD,selected=sel)}.isFailure)}
 @Test fun invalidBoardRefusedEvenIfBadRowUnselected(){val bad=rows+rows.first();assertTrue(runCatching{TallyBatch.preview(bad,setOf("b"),TallyBatch.Operation.SET,"2")}.isFailure)}
 @Test fun applyRequiresExactSourceValuesOrderAndLabels(){val x=p(TallyBatch.Operation.ADD);for(a in listOf(rows.reversed(),rows.dropLast(1),rows.map{if(it.id=="a")it.copy(value=13)else it},rows.map{if(it.id=="c")it.copy(label="Renamed kg")else it}))assertTrue(runCatching{TallyBatch.apply(a,x)}.isFailure);assertEquals(x.after,TallyBatch.apply(rows,x))}
 @Test fun alteredPreviewCannotApplyUnselectedChanges(){val x=p(TallyBatch.Operation.ADD);assertTrue(runCatching{TallyBatch.apply(rows,x.copy(after=x.after.map{it.copy(value=0)}))}.isFailure);assertTrue(runCatching{TallyBatch.apply(rows,x.copy(operand=4))}.isFailure)}
 @Test fun oneBoardUndoRestoresAllAndRedoReapplies(){val b=CallTally.Board(rows);val x=p(TallyBatch.Operation.SET,"0");b.change(TallyBatch.apply(b.rows,x));assertEquals(1,b.undoCount);b.undo();assertEquals(rows,b.rows);b.redo();assertEquals(x.after,b.rows)}
 @Test fun noOpPreservesHistoryAndRedo(){val b=CallTally.Board(rows);b.change(p(TallyBatch.Operation.SET,"0").after);b.undo();assertFalse(b.change(p(TallyBatch.Operation.ADD,"0").after));assertEquals(0,b.undoCount);assertTrue(b.canRedo)}
}
