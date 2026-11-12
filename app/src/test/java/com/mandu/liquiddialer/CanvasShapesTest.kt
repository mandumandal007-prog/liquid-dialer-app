package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class CanvasShapesTest {
 private val a=CallCanvas.Point(1000,2000);private val b=CallCanvas.Point(8000,7000)
 @Test fun lineRetainsExactEndpoints(){assertEquals(listOf(a,b),CanvasShapes.points(CanvasShapes.Tool.LINE,a,b))}
 @Test fun reversedBoxIsClosedAndNormalized(){val p=CanvasShapes.points(CanvasShapes.Tool.BOX,b,a);assertEquals(5,p.size);assertEquals(a,p.first());assertEquals(p.first(),p.last());assertEquals(CallCanvas.Point(8000,2000),p[1])}
 @Test fun ovalIsClosedWithinDragBounds(){val p=CanvasShapes.points(CanvasShapes.Tool.OVAL,a,b);assertEquals(49,p.size);assertEquals(p.first(),p.last());assertTrue(p.all{it.x in a.x..b.x&&it.y in a.y..b.y});assertEquals(1000,p.minOf{it.x});assertEquals(8000,p.maxOf{it.x})}
 @Test fun reversedOvalHasSameBounds(){assertEquals(CanvasShapes.points(CanvasShapes.Tool.OVAL,a,b),CanvasShapes.points(CanvasShapes.Tool.OVAL,b,a))}
 @Test fun arrowHasTwoWingsAndExactTip(){val p=CanvasShapes.points(CanvasShapes.Tool.ARROW,a,b);assertEquals(5,p.size);assertEquals(a,p[0]);assertEquals(b,p[1]);assertEquals(b,p[3]);assertNotEquals(p[2],p[4]);assertTrue(p[2].x<b.x&&p[4].x<b.x)}
 @Test fun edgeArrowsStayInPage(){for(tip in listOf(CallCanvas.Point(0,0),CallCanvas.Point(10000,0),CallCanvas.Point(0,10000),CallCanvas.Point(10000,10000))){assertTrue(CanvasShapes.points(CanvasShapes.Tool.ARROW,CallCanvas.Point(5000,5000),tip).all{it.x in 0..10000&&it.y in 0..10000})}}
 @Test fun tinyDragDoesNotMakeAccidentalDot(){CanvasShapes.Tool.entries.filter{it!=CanvasShapes.Tool.PEN}.forEach{assertTrue(CanvasShapes.points(it,a,a).isEmpty());assertTrue(CanvasShapes.points(it,a,CallCanvas.Point(1005,2005)).isEmpty())}}
 @Test fun collapsedAreaDoesNotCreateBoxOrOval(){listOf(CanvasShapes.Tool.BOX,CanvasShapes.Tool.OVAL).forEach{assertTrue(CanvasShapes.points(it,a,CallCanvas.Point(1000,7000)).isEmpty());assertTrue(CanvasShapes.points(it,a,CallCanvas.Point(8000,2000)).isEmpty())}}
 @Test(expected=IllegalArgumentException::class) fun penIsNotAReplacementForFreehand(){CanvasShapes.points(CanvasShapes.Tool.PEN,a,b)}
 @Test fun allShapesRespectExistingStorageBounds(){CanvasShapes.Tool.entries.filter{it!=CanvasShapes.Tool.PEN}.forEach{val p=CanvasShapes.points(it,a,b);assertTrue(p.size<=CallCanvas.MAX_POINTS);assertTrue(p.isNotEmpty())}}
}
