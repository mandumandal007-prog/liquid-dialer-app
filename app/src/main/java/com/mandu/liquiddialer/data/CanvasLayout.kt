package com.mandu.liquiddialer.data
import kotlin.math.roundToInt

/** Translation only, preserving stroke order, style, point count and geometry. */
object CanvasLayout {
 enum class Edit(val label:String){LEFT("Align left"),RIGHT("Align right"),TOP("Align top"),BOTTOM("Align bottom"),CENTER_X("Centers X"),CENTER_Y("Centers Y"),SPACE_X("Space X"),SPACE_Y("Space Y"),NUDGE_LEFT("Left 2%"),NUDGE_RIGHT("Right 2%"),NUDGE_UP("Up 2%"),NUDGE_DOWN("Down 2%")}
 data class Bounds(val left:Int,val top:Int,val right:Int,val bottom:Int){val centerX get()=(left+right)/2.0;val centerY get()=(top+bottom)/2.0}
 fun bounds(s:CallCanvas.Stroke)=Bounds(s.points.minOf{it.x},s.points.minOf{it.y},s.points.maxOf{it.x},s.points.maxOf{it.y})
 fun apply(rows:List<CallCanvas.Stroke>,selected:Set<Int>,edit:Edit):List<CallCanvas.Stroke>{
  CallCanvas.validate(rows);require(selected.isNotEmpty()&&selected.all{it in rows.indices}){"Ink select karo"}
  val distribute=edit==Edit.SPACE_X||edit==Edit.SPACE_Y;val nudge=edit.name.startsWith("NUDGE_")
  require(nudge||selected.size>=if(distribute)3 else 2){if(distribute)"Spacing ke liye 3 strokes select karo"else"Alignment ke liye 2 strokes select karo"}
  val indices=selected.sorted();val boxes=indices.associateWith{bounds(rows[it])};val left=boxes.values.minOf{it.left};val right=boxes.values.maxOf{it.right};val top=boxes.values.minOf{it.top};val bottom=boxes.values.maxOf{it.bottom}
  val targets=mutableMapOf<Int,Double>()
  if(distribute){val horizontal=edit==Edit.SPACE_X;val ordered=indices.sortedWith(compareBy<Int>{if(horizontal)boxes.getValue(it).centerX else boxes.getValue(it).centerY}.thenBy{it});val first=boxes.getValue(ordered.first()).let{if(horizontal)it.centerX else it.centerY};val last=boxes.getValue(ordered.last()).let{if(horizontal)it.centerX else it.centerY};ordered.forEachIndexed{i,id->targets[id]=first+(last-first)*i/(ordered.size-1)}}
  val delta=indices.associateWith{i->val b=boxes.getValue(i);when(edit){
   Edit.LEFT->left-b.left to 0;Edit.RIGHT->right-b.right to 0;Edit.TOP->0 to top-b.top;Edit.BOTTOM->0 to bottom-b.bottom
   Edit.CENTER_X->((left+right)/2.0-b.centerX).roundToInt() to 0;Edit.CENTER_Y->0 to ((top+bottom)/2.0-b.centerY).roundToInt()
   Edit.SPACE_X->(targets.getValue(i)-b.centerX).roundToInt() to 0;Edit.SPACE_Y->0 to (targets.getValue(i)-b.centerY).roundToInt()
   Edit.NUDGE_LEFT-> -200 to 0;Edit.NUDGE_RIGHT->200 to 0;Edit.NUDGE_UP->0 to -200;Edit.NUDGE_DOWN->0 to 200
  }}
  require(indices.all{i->val b=boxes.getValue(i);val(dx,dy)=delta.getValue(i);b.left+dx>=0&&b.right+dx<=10000&&b.top+dy>=0&&b.bottom+dy<=10000}){"Paper edge: whole edit refused; no ink moved"}
  return rows.mapIndexed{i,s->delta[i]?.let{(dx,dy)->s.copy(points=s.points.map{CallCanvas.Point(it.x+dx,it.y+dy)})}?:s}.also{CallCanvas.validate(it)}
 }
 class Draft(initial:List<CallCanvas.Stroke>){var rows=initial.toList();private set;private val past=mutableListOf<List<CallCanvas.Stroke>>();private val future=mutableListOf<List<CallCanvas.Stroke>>()
  init{CallCanvas.validate(rows)}
  val canUndo get()=past.isNotEmpty();val canRedo get()=future.isNotEmpty()
  fun change(next:List<CallCanvas.Stroke>):Boolean{CallCanvas.validate(next);require(next.size==rows.size);if(next==rows)return false;past.add(rows);if(past.size>40)past.removeAt(0);rows=next.toList();future.clear();return true}
  fun undo(){if(canUndo){future.add(rows);rows=past.removeAt(past.lastIndex)}}
  fun redo(){if(canRedo){past.add(rows);rows=future.removeAt(future.lastIndex)}}
 }
}
