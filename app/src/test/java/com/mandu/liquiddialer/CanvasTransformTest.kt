package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[34])
class CanvasTransformTest {
 private fun stroke(vararg p:Pair<Int,Int>)=CallCanvas.Stroke(2,3,p.map{CallCanvas.Point(it.first,it.second)})
 private val line=stroke(2000 to 3000,8000 to 7000)
 private fun edit(e:CanvasTransform.Edit,s:CallCanvas.Stroke=line,a:Double=1.1)=CanvasTransform.apply(s,e,a)!!
 @Test fun resizeUsesBoundingCenter(){assertEquals(stroke(2300 to 3200,7700 to 6800),edit(CanvasTransform.Edit.SMALLER));assertEquals(stroke(1700 to 2800,8300 to 7200),edit(CanvasTransform.Edit.BIGGER))}
 @Test fun mirrorHorizontalKeepsCenterAndY(){assertEquals(stroke(8000 to 3000,2000 to 7000),edit(CanvasTransform.Edit.MIRROR_X))}
 @Test fun mirrorVerticalKeepsCenterAndX(){assertEquals(stroke(2000 to 7000,8000 to 3000),edit(CanvasTransform.Edit.MIRROR_Y))}
 @Test fun mirrorTwiceRestoresExactPoints(){for(e in listOf(CanvasTransform.Edit.MIRROR_X,CanvasTransform.Edit.MIRROR_Y))assertEquals(line,edit(e,edit(e)))}
 @Test fun clockwiseUsesActualAspect(){val s=stroke(3000 to 5000,7000 to 5000);val n=edit(CanvasTransform.Edit.RIGHT,s,2.0);assertEquals(3068,n.points[0].x);assertEquals(3965,n.points[0].y);assertEquals(6035,n.points[1].y)}
 @Test fun counterclockwiseUsesActualAspect(){val s=stroke(3000 to 5000,7000 to 5000);val n=edit(CanvasTransform.Edit.LEFT,s,.5);assertEquals(5259,n.points[0].y);assertEquals(4741,n.points[1].y)}
 @Test fun rotationPreservesPhysicalDistance(){for(a in listOf(.5,1.1,2.0)){val n=edit(CanvasTransform.Edit.RIGHT,line,a);fun length(s:CallCanvas.Stroke)=hypot((s.points[1].x-s.points[0].x)*a,(s.points[1].y-s.points[0].y).toDouble())
assertEquals(length(line),length(n),2.0)}}
 @Test fun edgeResizeRefusedNotClipped(){assertNull(CanvasTransform.apply(stroke(0 to 0,10000 to 10000),CanvasTransform.Edit.BIGGER,1.1))}
 @Test fun edgeRotationRefusedNotClipped(){assertNull(CanvasTransform.apply(stroke(0 to 0,10000 to 10000),CanvasTransform.Edit.RIGHT,1.1))}
 @Test fun centeredResizeToBoundaryAllowed(){val s=stroke(500 to 500,9500 to 9500);assertNotNull(CanvasTransform.apply(s,CanvasTransform.Edit.BIGGER,1.1))}
 @Test fun allEditsKeepStyleCountAndInput(){for(e in CanvasTransform.Edit.entries){val n=edit(e);assertEquals(line.ink,n.ink);assertEquals(line.width,n.width);assertEquals(line.points.size,n.points.size)};assertEquals(stroke(2000 to 3000,8000 to 7000),line)}
 @Test fun closedShapeStaysClosed(){val s=stroke(3000 to 3000,7000 to 3000,7000 to 7000,3000 to 3000);for(e in CanvasTransform.Edit.entries){val p=edit(e,s).points;assertEquals(p.first(),p.last())}}
 @Test fun dotHasNoArtificialSize(){val s=stroke(4200 to 3900);for(e in CanvasTransform.Edit.entries)assertEquals(s,edit(e,s))}
 @Test fun noOpDoesNotCreateUndo(){val s=stroke(4200 to 3900);val h=CallCanvas.History(emptyList());h.add(s);h.undo();h.redo();assertFalse(h.replace(0,edit(CanvasTransform.Edit.BIGGER,s)));h.undo();assertTrue(h.strokes.isEmpty())}
 @Test fun eachEditHasSingleUndoRedoAndOtherInkUnchanged(){val other=stroke(1000 to 1000,1500 to 1500);val h=CallCanvas.History(listOf(line,other));for(e in CanvasTransform.Edit.entries){val before=h.strokes.toList();val n=edit(e,before[0]);assertTrue(h.replace(0,n));assertEquals(other,h.strokes[1]);h.undo();assertEquals(before,h.strokes);h.redo();assertEquals(n,h.strokes[0])}}
 @Test fun invalidAspectRejected(){for(a in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))assertTrue(runCatching{CanvasTransform.apply(line,CanvasTransform.Edit.RIGHT,a)}.isFailure)}
 @Test fun serializeWithoutFormatChange(){val s=edit(CanvasTransform.Edit.RIGHT);val encoded=CallCanvas.encode(mapOf("+919876543210" to listOf(s)));assertEquals(s,CallCanvas.decode(encoded).values.single().single());assertTrue(encoded.contains("\"version\":1"))}
 @Test fun repeatedShrinkRemainsValidAndBounded(){var s=line;repeat(100){s=edit(CanvasTransform.Edit.SMALLER,s);s.validate();assertTrue(s.points.all{it.x in 0..10000&&it.y in 0..10000})}}
}
