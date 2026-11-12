package com.mandu.liquiddialer.data
import kotlin.math.*

/** Selection uses displayed segment distance, never a shape's empty interior. */
object CanvasArrange {
 fun hit(strokes:List<CallCanvas.Stroke>,at:CallCanvas.Point,pageWidth:Float,pageHeight:Float,radiusPx:Float):Int? {
  require(pageWidth>0&&pageHeight>0&&radiusPx>=0)
  fun xy(p:CallCanvas.Point)=Pair(p.x*pageWidth/10000.0,p.y*pageHeight/10000.0)
  val (px,py)=xy(at)
  for(index in strokes.indices.reversed()){
   val ps=strokes[index].points
   fun distance(a:CallCanvas.Point,b:CallCanvas.Point):Double{val(ax,ay)=xy(a);val(bx,by)=xy(b);val dx=bx-ax;val dy=by-ay;val square=dx*dx+dy*dy;val t=if(square==0.0)0.0 else (((px-ax)*dx+(py-ay)*dy)/square).coerceIn(0.0,1.0);return hypot(px-ax-t*dx,py-ay-t*dy)}
   if(ps.size==1&&distance(ps[0],ps[0])<=radiusPx)return index
   if(ps.zipWithNext().any{distance(it.first,it.second)<=radiusPx})return index
  };return null
 }
 /** Clamp one shared translation so the whole stroke keeps its geometry. */
 fun move(stroke:CallCanvas.Stroke,dx:Int,dy:Int):CallCanvas.Stroke{
  val ps=stroke.points;val x=dx.coerceIn(-ps.minOf{it.x},10000-ps.maxOf{it.x});val y=dy.coerceIn(-ps.minOf{it.y},10000-ps.maxOf{it.y})
  return stroke.copy(points=ps.map{CallCanvas.Point(it.x+x,it.y+y)})
 }
 fun duplicate(stroke:CallCanvas.Stroke):CallCanvas.Stroke{val next=move(stroke,600,600);return if(next==stroke)move(stroke,-600,-600)else next}
}
