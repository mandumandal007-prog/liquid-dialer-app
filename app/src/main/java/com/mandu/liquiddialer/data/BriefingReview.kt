package com.mandu.liquiddialer.data
import java.util.Locale
/** Projection only. No source reads/writes, Telecom actions, scheduling or storage. */
object BriefingReview {
 data class Row(val id:String,val source:String,val title:String,val detail:String,val dueAt:Long=0)
 data class Queue(val rows:List<Row>,val errors:List<String>)
 fun from(s:CallBriefing.Snapshot):Queue {
  CallLists.number(s.number)
  val topics=if(s.topics.error)emptyList()else s.topics.value.orEmpty().filter{!it.done}.map{Row("topic:"+it.id,"Topics",it.text,"Saved pending topic")}
  val lists=if(s.lists.error)emptyList()else s.lists.value.orEmpty().filter{!it.entry.done&&ContactControls.k(it.entry.number)==ContactControls.k(s.number)}.map{
   Row("list:${it.listId.length}:${it.listId}:${it.entry.id}","Call Lists",it.title,listOf(it.entry.name,it.entry.note,CallLists.outcomes[it.entry.outcome]?:it.entry.outcome).filter{v->v.isNotBlank()}.joinToString("\n"),it.entry.dueAt)
  }
  val rows=topics+lists;require(rows.map{it.id}.distinct().size==rows.size)
  return Queue(rows, listOfNotNull(if(s.topics.error)"Topics"else null,if(s.lists.error)"Call Lists"else null))
 }
 val sources=listOf("All","Topics","Call Lists")
 fun visible(q:Queue,query:String,source:String,unreviewed:Boolean,reviewed:Set<String>):List<Row>{
  require(source in sources)
  val terms=query.take(100).lowercase(Locale.ROOT).trim().split(Regex("\\s+")).filter{it.isNotBlank()}
  return q.rows.filter{r->(source=="All"||r.source==source)&&(!unreviewed||r.id !in reviewed)&&terms.all{(r.title+" "+r.detail).lowercase(Locale.ROOT).contains(it)}}
 }
 fun toggle(q:Queue,reviewed:Set<String>,id:String):Set<String>{require(q.rows.any{it.id==id});val valid=reviewed.intersect(q.rows.map{it.id}.toSet());return if(id in valid)valid-id else valid+id}
 fun count(q:Queue,reviewed:Set<String>)=q.rows.count{it.id in reviewed}
}
