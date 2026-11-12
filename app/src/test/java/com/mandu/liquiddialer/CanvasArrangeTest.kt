package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class CanvasArrangeTest {
 private fun p(x:Int,y:Int)=CallCanvas.Point(x,y)
 private fun line()=CallCanvas.Stroke(2,3,listOf(p(1000,1000),p(7000,7000)))
 @Test fun segmentHitWorksBetweenSamples(){assertEquals(0,CanvasArrange.hit(listOf(line()),p(4000,4000),300f,200f,5f))}
 @Test fun emptyAreaDoesNotSelectBoundingBox(){assertNull(CanvasArrange.hit(listOf(line()),p(1000,7000),300f,200f,5f))}
 @Test fun closestOverlappingInkSelectsTopmost(){assertEquals(1,CanvasArrange.hit(listOf(line(),line().copy(ink=1)),p(4000,4000),300f,200f,5f))}
 @Test fun singleDotCanBeSelected(){val dot=line().copy(points=listOf(p(5000,5000)));assertEquals(0,CanvasArrange.hit(listOf(dot),p(5001,5001),300f,200f,5f))}
 @Test fun rectangularScreenUsesPixelDistance(){assertNull(CanvasArrange.hit(listOf(line().copy(points=listOf(p(5000,5000)))),p(5500,5000),1000f,100f,20f));assertEquals(0,CanvasArrange.hit(listOf(line().copy(points=listOf(p(5000,5000)))),p(5000,5500),1000f,100f,20f))}
 @Test fun shapeInteriorDoesNotSelect(){val s=line().copy(points=CanvasShapes.points(CanvasShapes.Tool.BOX,p(1000,1000),p(9000,9000)));assertNull(CanvasArrange.hit(listOf(s),p(5000,5000),300f,300f,10f))}
 @Test fun moveKeepsStyleOrderAndUniformDelta(){val before=line();val after=CanvasArrange.move(before,1000,500);assertEquals(2,after.ink);assertEquals(3,after.width);assertEquals(listOf(p(2000,1500),p(8000,7500)),after.points);assertEquals(p(1000,1000),before.points.first())}
 @Test fun edgeMoveClampsWholeStrokeWithoutDistortion(){val after=CanvasArrange.move(line(),10000,-10000);assertEquals(listOf(p(4000,0),p(10000,6000)),after.points)}
 @Test fun duplicateOffsetsAndPreservesOriginal(){val s=line();val d=CanvasArrange.duplicate(s);assertEquals(p(1600,1600),d.points.first());assertNotEquals(s,d)}
 @Test fun duplicateAtBottomRightMovesBack(){val s=line().copy(points=listOf(p(10000,10000)));assertEquals(listOf(p(9400,9400)),CanvasArrange.duplicate(s).points)}
 @Test fun moveUndoRedoIsOneEditNotStrokeRemoval(){val h=CallCanvas.History(listOf(line()));val moved=CanvasArrange.move(line(),1000,0);assertTrue(h.replace(0,moved));h.undo();assertEquals(listOf(line()),h.strokes);h.redo();assertEquals(listOf(moved),h.strokes)}
 @Test fun removeLastCanUndoAndRedoEmptyCanvas(){val h=CallCanvas.History(listOf(line()));assertTrue(h.remove(0));assertTrue(h.strokes.isEmpty());assertTrue(h.canUndo);h.undo();assertEquals(listOf(line()),h.strokes);h.redo();assertTrue(h.strokes.isEmpty())}
 @Test fun editAfterUndoDropsRedo(){val h=CallCanvas.History(listOf(line()));h.replace(0,CanvasArrange.move(line(),500,0));h.undo();h.remove(0);assertFalse(h.canRedo)}
 @Test fun noOpAndBadIndexDoNotAddHistory(){val h=CallCanvas.History(emptyList());assertFalse(h.replace(0,line()));assertFalse(h.remove(-1));assertFalse(h.canUndo);h.add(line());assertFalse(h.replace(0,line()));h.undo();assertTrue(h.strokes.isEmpty())}
 @Test fun copyAtCapacityDoesNotMutate(){val all=List(CallCanvas.MAX_STROKES){line()};val h=CallCanvas.History(all);assertFalse(h.add(line()));assertEquals(all,h.strokes)}
 @Test fun boundedEditUndoRetainsExactly80Changes(){val h=CallCanvas.History(listOf(line()));repeat(100){h.replace(0,CanvasArrange.move(line(),it+1,0))};var n=0;while(h.canUndo){h.undo();n++};assertEquals(80,n)}
 @Test fun clearStillResetsHistory(){val h=CallCanvas.History(listOf(line()));h.remove(0);h.clear();assertFalse(h.canUndo);assertFalse(h.canRedo)}
 @Test fun removeKeepsOtherStrokeIdentitiesAndOrder(){val a=line();val b=line().copy(ink=1);val c=line().copy(ink=3);val h=CallCanvas.History(listOf(a,b,c));h.remove(1);assertEquals(listOf(a,c),h.strokes);h.undo();assertEquals(listOf(a,b,c),h.strokes)}
}
