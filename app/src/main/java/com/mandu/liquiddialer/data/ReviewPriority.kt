package com.mandu.liquiddialer.data
/** Manual temporary reading order, never saved importance or source edits. */
object ReviewPriority {
 const val MAX=10
 fun toggle(rows:List<BriefingReview.Row>,selected:Set<String>,id:String):Set<String>{
  require(rows.map{it.id}.distinct().size==rows.size){"Duplicate row identity"}
  val valid=rows.map{it.id}.toSet();require(id in valid){"Item not in this snapshot"}
  val clean=selected.intersect(valid)
  if(id in clean)return clean-id
  require(clean.size<MAX){"Up to10 session priorities. Undo one first."}
  return clean+id
 }
 fun order(rows:List<BriefingReview.Row>,selected:Set<String>,only:Boolean):List<BriefingReview.Row> = if(only)rows.filter{it.id in selected}else rows.filter{it.id in selected}+rows.filter{it.id !in selected}
}
