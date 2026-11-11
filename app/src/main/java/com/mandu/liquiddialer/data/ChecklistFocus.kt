package com.mandu.liquiddialer.data

/** Stable-ID focus selection. Navigation is pure and never marks a topic. */
object ChecklistFocus {
 fun visible(rows:List<CallChecklist.Item>,pending:Boolean)=rows.filter{!pending||!it.done}
 fun selected(rows:List<CallChecklist.Item>,id:String?):CallChecklist.Item?=rows.firstOrNull{it.id==id}?:rows.firstOrNull()
 fun move(rows:List<CallChecklist.Item>,id:String?,step:Int):String?{if(rows.isEmpty())return null;val i=rows.indexOfFirst{it.id==id}.coerceAtLeast(0);return rows[(i+step).coerceIn(0,rows.lastIndex)].id}
 fun afterMark(before:List<CallChecklist.Item>,after:List<CallChecklist.Item>,id:String):String?{val i=before.indexOfFirst{it.id==id}.coerceAtLeast(0);return after.firstOrNull{it.id==id}?.id?:after.getOrNull(i.coerceAtMost((after.size-1).coerceAtLeast(0)))?.id}
}
