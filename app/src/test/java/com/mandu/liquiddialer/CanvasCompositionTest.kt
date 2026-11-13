package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[34])
class CanvasCompositionTest {
 private fun line(x:Int,y:Int)=CallCanvas.Stroke(1,2,listOf(CallCanvas.Point(x,y),CallCanvas.Point(x+1000,y+1000)))
 private val rows=listOf(line(1000,1000),line(3000,3000),line(5000,5000),line(7000,7000))
 @Test fun colorChangesOnlySelectedWithoutGeometry(){val n=CanvasComposition.style(rows,setOf(0,2),ink=3);assertEquals(rows[1],n[1]);assertEquals(rows[3],n[3]);for(i in listOf(0,2)){assertEquals(3,n[i].ink);assertEquals(rows[i].width,n[i].width);assertEquals(rows[i].points,n[i].points)}}
 @Test fun allPaletteAndWidthsValidate(){for(c in 0..3)for(w in 1..3){val n=CanvasComposition.style(rows,rows.indices.toSet(),c,w);assertTrue(n.all{it.ink==c&&it.width==w});assertEquals(rows.map{it.points},n.map{it.points})}}
 @Test fun invalidStyleAtomic(){for(block in listOf<()->Any>({CanvasComposition.style(rows,setOf(0))},{CanvasComposition.style(rows,setOf(0),ink=4)},{CanvasComposition.style(rows,setOf(0),ink=-1)},{CanvasComposition.style(rows,setOf(0),width=0)},{CanvasComposition.style(rows,setOf(0),width=4)}))assertTrue(runCatching(block).isFailure);assertEquals(1,rows.first().ink)}
 @Test fun frontStablePartition(){assertEquals(listOf(rows[1],rows[3],rows[0],rows[2]),CanvasComposition.layer(rows,setOf(2,0),CanvasComposition.Layer.FRONT))}
 @Test fun backStablePartition(){assertEquals(listOf(rows[1],rows[3],rows[0],rows[2]),CanvasComposition.layer(rows,setOf(3,1),CanvasComposition.Layer.BACK))}
 @Test fun allToFrontOrBackIsNoOp(){for(l in CanvasComposition.Layer.entries)assertEquals(rows,CanvasComposition.layer(rows,rows.indices.toSet(),l))}
 @Test fun duplicatePreservesOriginalAndStyles(){val n=CanvasComposition.duplicate(rows,setOf(2,0),400,-400);assertEquals(rows,n.take(4));assertEquals(listOf(rows[0],rows[2]).map{s->s.copy(points=s.points.map{CallCanvas.Point(it.x+400,it.y-400)})},n.drop(4))}
 @Test fun allFourOffsetsAndPageBoundaryAccepted(){for((dx,dy)in listOf(-400 to 0,400 to 0,0 to -400,0 to 400)){val n=CanvasComposition.duplicate(rows,setOf(1),dx,dy).last();assertEquals(rows[1].points.map{CallCanvas.Point(it.x+dx,it.y+dy)},n.points)};assertEquals(0,CanvasComposition.duplicate(listOf(line(400,400)),setOf(0),-400,0).last().points.first().x)}
 @Test fun wholeEdgeCrossingRefusedNoClamping(){val s=rows+line(0,0);assertTrue(runCatching{CanvasComposition.duplicate(s,setOf(0,4),-400,0)}.isFailure);assertEquals(5,s.size);assertEquals(0,s.last().points.first().x)}
 @Test fun extremeAndZeroOffsetsRefused(){for((x,y)in listOf(0 to 0,2001 to 0,0 to -2001,Int.MAX_VALUE to Int.MIN_VALUE))assertTrue(runCatching{CanvasComposition.duplicate(rows,setOf(0),x,y)}.isFailure)}
 @Test fun strokeCapacityExactAndOverflow(){val s=List(79){rows[0]};assertEquals(80,CanvasComposition.duplicate(s,setOf(0),400,0).size);assertTrue(runCatching{CanvasComposition.duplicate(s,setOf(0,1),400,0)}.isFailure)}
 @Test fun totalPointCapacityExactAndOverflow(){val stroke=rows[0].copy(points=List(400){CallCanvas.Point(1000,1000)});val s=List(29){stroke};assertEquals(12000,CanvasComposition.duplicate(s,setOf(0),400,0).sumOf{it.points.size});assertTrue(runCatching{CanvasComposition.duplicate(s,setOf(0,1),400,0)}.isFailure)}
 @Test fun everyOperationRejectsInvalidSelection(){for(sel in listOf(emptySet(),setOf(-1),setOf(4),setOf(0,5))){assertTrue(runCatching{CanvasComposition.style(rows,sel,ink=2)}.isFailure);assertTrue(runCatching{CanvasComposition.layer(rows,sel,CanvasComposition.Layer.FRONT)}.isFailure);assertTrue(runCatching{CanvasComposition.duplicate(rows,sel,400,0)}.isFailure)}}
 @Test fun invalidInputRefused(){val s=listOf(rows[0].copy(ink=8));assertTrue(runCatching{CanvasComposition.Draft(s)}.isFailure);assertTrue(runCatching{CanvasComposition.style(s,setOf(0),ink=2)}.isFailure)}
 @Test fun historyUndoRedoAcrossDifferentCounts(){val d=CanvasComposition.Draft(rows);val a=CanvasComposition.duplicate(rows,setOf(0),400,0);d.change(a);d.change(CanvasComposition.style(a,setOf(4),ink=3));d.undo();assertEquals(a,d.rows);d.undo();assertEquals(rows,d.rows);d.redo();assertEquals(a,d.rows);d.redo();assertEquals(3,d.rows.last().ink)}
 @Test fun noOpDoesNotClearRedoOrAddHistory(){val d=CanvasComposition.Draft(rows);assertFalse(d.change(rows));assertFalse(d.canUndo);d.change(CanvasComposition.style(rows,setOf(0),ink=3));d.undo();assertTrue(d.canRedo);assertFalse(d.change(rows));assertTrue(d.canRedo)}
 @Test fun newEditClearsRedoAndInvalidEditPreservesDraft(){val d=CanvasComposition.Draft(rows);d.change(CanvasComposition.style(rows,setOf(0),ink=3));d.undo();d.change(CanvasComposition.style(rows,setOf(0),width=3));assertFalse(d.canRedo);val before=d.rows;assertTrue(runCatching{d.change(listOf(rows[0].copy(width=8)))}.isFailure);assertEquals(before,d.rows)}
 @Test fun historyBoundedForty(){val d=CanvasComposition.Draft(rows);repeat(45){d.change(CanvasComposition.style(d.rows,setOf(0),ink=if(it%2==0)2 else 3))};repeat(45){d.undo()};assertEquals(2,d.rows[0].ink);assertFalse(d.canUndo);repeat(40){d.redo()};assertEquals(2,d.rows[0].ink);assertFalse(d.canRedo)}
 @Test fun roundTripExistingFormatAndDot(){val s=rows+CallCanvas.Stroke(0,1,listOf(CallCanvas.Point(2000,2000)));val n=CanvasComposition.duplicate(s,setOf(4),400,400);assertEquals(1,n.last().points.size);assertEquals(n,CallCanvas.decode(CallCanvas.encode(mapOf("+919876543210" to n))).values.single())}
}
