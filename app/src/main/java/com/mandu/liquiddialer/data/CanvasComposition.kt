package com.mandu.liquiddialer.data

/** Local stroke composition, never clipboard copying. Later rows draw on top. */
object CanvasComposition {
 enum class Layer { FRONT, BACK }
 private fun selection(rows:List<CallCanvas.Stroke>,selected:Set<Int>){CallCanvas.validate(rows);require(selected.isNotEmpty()&&selected.all{it in rows.indices}){"Ink select karo"}}
 fun style(rows:List<CallCanvas.Stroke>,selected:Set<Int>,ink:Int?=null,width:Int?=null):List<CallCanvas.Stroke>{selection(rows,selected);require(ink!=null||width!=null);require(ink==null||ink in 0..3);require(width==null||width in 1..3);return rows.mapIndexed{i,s->if(i in selected)s.copy(ink=ink?:s.ink,width=width?:s.width)else s}.also{CallCanvas.validate(it)}}
 fun layer(rows:List<CallCanvas.Stroke>,selected:Set<Int>,layer:Layer):List<CallCanvas.Stroke>{selection(rows,selected);val group=rows.filterIndexed{i,_->i in selected};val other=rows.filterIndexed{i,_->i !in selected};return (if(layer==Layer.FRONT)other+group else group+other).also{CallCanvas.validate(it)}}
 fun duplicate(rows:List<CallCanvas.Stroke>,selected:Set<Int>,dx:Int,dy:Int):List<CallCanvas.Stroke>{selection(rows,selected);require(dx in -2000..2000&&dy in -2000..2000&&(dx!=0||dy!=0)){"Choose a nonzero offset within 20%"};val group=rows.filterIndexed{i,_->i in selected};require(rows.size+group.size<=CallCanvas.MAX_STROKES&&rows.sumOf{it.points.size}+group.sumOf{it.points.size}<=CallCanvas.MAX_TOTAL_POINTS){"Canvas limit: whole duplicate refused"};require(group.all{s->s.points.all{it.x+dx in 0..10000&&it.y+dy in 0..10000}}){"Paper edge: whole duplicate refused; no clamping"};val added=group.map{s->s.copy(points=s.points.map{CallCanvas.Point(it.x+dx,it.y+dy)})};return (rows+added).also{CallCanvas.validate(it)}}
 class Draft(initial:List<CallCanvas.Stroke>){var rows=initial.toList();private set;private val past=mutableListOf<List<CallCanvas.Stroke>>();private val future=mutableListOf<List<CallCanvas.Stroke>>()
  init{CallCanvas.validate(rows)}
  val canUndo get()=past.isNotEmpty();val canRedo get()=future.isNotEmpty()
  fun change(next:List<CallCanvas.Stroke>):Boolean{CallCanvas.validate(next);if(next==rows)return false;past.add(rows);if(past.size>40)past.removeAt(0);rows=next.toList();future.clear();return true}
  fun undo(){if(canUndo){future.add(rows);rows=past.removeAt(past.lastIndex)}}
  fun redo(){if(canRedo){past.add(rows);rows=future.removeAt(future.lastIndex)}}
 }
}
