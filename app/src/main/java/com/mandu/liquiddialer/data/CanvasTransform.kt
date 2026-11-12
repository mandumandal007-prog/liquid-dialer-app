package com.mandu.liquiddialer.data
import kotlin.math.*

/** One selected stroke, one explicit edit. No file access and no point clipping. */
object CanvasTransform {
 enum class Edit(val label:String){SMALLER("Size -10%"),BIGGER("Size +10%"),LEFT("Rotate -15°"),RIGHT("Rotate +15°"),MIRROR_X("Mirror ↔"),MIRROR_Y("Mirror ↕")}
 /** Positive rotation is clockwise on screen. Aspect is the actual drawable width/height. */
 fun apply(stroke:CallCanvas.Stroke,edit:Edit,aspect:Double):CallCanvas.Stroke? {
  stroke.validate();require(aspect.isFinite()&&aspect>0)
  val ps=stroke.points;val cx=(ps.minOf{it.x}+ps.maxOf{it.x})/2.0;val cy=(ps.minOf{it.y}+ps.maxOf{it.y})/2.0
  val angle=when(edit){Edit.LEFT->-PI/12;Edit.RIGHT->PI/12;else->0.0};val c=cos(angle);val s=sin(angle)
  val points=ps.map{p->
   val x=p.x-cx;val y=p.y-cy
   val (dx,dy)=when(edit){
    Edit.SMALLER->Pair(x*.9,y*.9);Edit.BIGGER->Pair(x*1.1,y*1.1)
    Edit.MIRROR_X->Pair(-x,y);Edit.MIRROR_Y->Pair(x,-y)
    else->Pair(x*c-y*s/aspect,x*aspect*s+y*c)
   }
   Pair(cx+dx,cy+dy)
  }
  // A rejected operation leaves the original geometry and history untouched.
  if(points.any{(x,y)->!x.isFinite()||!y.isFinite()||x < -0.000001||x > 10000.000001||y < -0.000001||y > 10000.000001})return null
  return stroke.copy(points=points.map{(x,y)->CallCanvas.Point(x.roundToInt().coerceIn(0,10000),y.roundToInt().coerceIn(0,10000))})
 }
}
