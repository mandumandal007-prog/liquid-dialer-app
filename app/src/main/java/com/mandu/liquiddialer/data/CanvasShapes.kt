package com.mandu.liquiddialer.data
import kotlin.math.*

/** Drag-to-shape helpers emit ordinary bounded vector ink, compatible with saved canvases. */
object CanvasShapes {
 enum class Tool(val label:String){PEN("Pen"),LINE("Line"),ARROW("Arrow"),BOX("Box"),OVAL("Oval")}
 fun points(tool:Tool,a:CallCanvas.Point,b:CallCanvas.Point):List<CallCanvas.Point>{
  require(tool!=Tool.PEN)
  val dx=(b.x-a.x).toDouble();val dy=(b.y-a.y).toDouble();val length=hypot(dx,dy)
  if(length<80)return emptyList()
  fun point(x:Double,y:Double)=CallCanvas.Point(x.roundToInt().coerceIn(0,10000),y.roundToInt().coerceIn(0,10000))
  val left=min(a.x,b.x);val right=max(a.x,b.x);val top=min(a.y,b.y);val bottom=max(a.y,b.y)
  return when(tool){
   Tool.LINE->listOf(a,b)
   Tool.ARROW->{val h=min(1500.0,length*.25);val ux=dx/length;val uy=dy/length
    listOf(a,b,point(b.x-h*ux-h*.55*uy,b.y-h*uy+h*.55*ux),b,point(b.x-h*ux+h*.55*uy,b.y-h*uy-h*.55*ux))}
   Tool.BOX->{if(right-left<80||bottom-top<80)emptyList()else listOf(CallCanvas.Point(left,top),CallCanvas.Point(right,top),CallCanvas.Point(right,bottom),CallCanvas.Point(left,bottom),CallCanvas.Point(left,top))}
   Tool.OVAL->{if(right-left<80||bottom-top<80)emptyList()else {val cx=(left+right)/2.0;val cy=(top+bottom)/2.0;val rx=(right-left)/2.0;val ry=(bottom-top)/2.0;(0..48).map{i->val angle=2*PI*i/48;point(cx+rx*cos(angle),cy+ry*sin(angle))}}}
   Tool.PEN->error("Pen samples use the gesture path")
  }
 }
}
