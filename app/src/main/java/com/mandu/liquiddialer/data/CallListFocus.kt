package com.mandu.liquiddialer.data

/** Read-only matching. Stored order, exact numbers and saved outcomes stay unchanged. */
object CallListFocus {
 fun find(entries:List<CallLists.Entry>,query:String="",state:String="Sab",outcome:String?=null):List<CallLists.Entry> {
  val q=query.trim();val digits=q.filter(Char::isDigit)
  val numeric=digits.isNotEmpty() && q.all {it.isDigit() || it in "+-(). "}
  return entries.filter {e->
   (state=="Sab" || e.done==(state=="Done")) && (outcome==null || e.outcome==outcome) &&
   (q.isEmpty() || e.name.contains(q,true) || e.number.contains(q,true) || e.note.contains(q,true) ||
    (numeric && e.number.filter(Char::isDigit).contains(digits)))
  }
 }
 fun session(list:CallLists.ListItem,ids:List<String>):CallLists.ListItem {
  val byId=list.entries.associateBy {it.id}
  return list.copy(entries=ids.distinct().mapNotNull {byId[it]})
 }
}
