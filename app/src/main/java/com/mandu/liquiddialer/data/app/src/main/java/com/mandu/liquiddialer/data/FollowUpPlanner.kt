package com.mandu.liquiddialer.data

import java.util.Calendar
import java.util.TimeZone

/** Pure cross-list view. A plan is not an alarm and a call never completes a plan. */
object FollowUpPlanner {
 data class Item(val listId:String,val title:String,val entry:CallLists.Entry)
 enum class View { TODAY, OVERDUE, UPCOMING, UNSCHEDULED, ALL }
 fun find(lists:List<CallLists.ListItem>,view:View,query:String="",outcome:String?=null,
          now:Long=System.currentTimeMillis(),zone:TimeZone=TimeZone.getDefault()):List<Item> {
  val day=Calendar.getInstance(zone).apply {timeInMillis=now;set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
  val start=day.timeInMillis;day.add(Calendar.DAY_OF_YEAR,1);val end=day.timeInMillis
  val q=query.trim();val digits=q.filter(Char::isDigit);val numeric=digits.isNotEmpty() && q.all{it.isDigit() || it in "+-(). "}
  return lists.flatMap {l->l.entries.map {Item(l.id,l.title,it)}}.filter {i->
   val e=i.entry
   !e.done && (outcome==null || e.outcome==outcome) && when(view){
    View.TODAY->e.dueAt in start until end
    View.OVERDUE->e.dueAt>0 && e.dueAt<now
    View.UPCOMING->e.dueAt>=end
    View.UNSCHEDULED->e.dueAt==0L
    View.ALL->true
   } && (q.isEmpty() || i.title.contains(q,true) || e.name.contains(q,true) || e.note.contains(q,true) || e.number.contains(q,true) || (numeric && e.number.filter(Char::isDigit).contains(digits)))
  }.sortedWith(compareBy<Item>{if(it.entry.dueAt==0L) Long.MAX_VALUE else it.entry.dueAt}.thenBy{it.title}.thenBy{it.entry.id})
 }
}
