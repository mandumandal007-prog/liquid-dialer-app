package com.mandu.liquiddialer.data
object ChecklistOrganizer {
 enum class Filter(val label:String){ALL("All"),PENDING("Pending"),DONE("Discussed")}
 fun visible(rows:List<CallChecklist.Item>,query:String,filter:Filter)=rows.filter{(filter==Filter.ALL||it.done==(filter==Filter.DONE))&&it.text.contains(query.trim().take(200),ignoreCase=true)}
 fun validate(before:List<CallChecklist.Item>,after:List<CallChecklist.Item>){
  require(before.size<=CallChecklist.MAX_ITEMS&&before.map{it.id}.distinct().size==before.size)
  require(after.size==before.size&&after.map{it.id}.toSet()==before.map{it.id}.toSet()&&after.map{it.id}.distinct().size==after.size){"Topics add/delete nahi kar sakte"}
  val old=before.associateBy{it.id};require(after.all{old[it.id]?.text==it.text}){"Topic text nahi badal sakte"}
 }
 fun mark(rows:List<CallChecklist.Item>,ids:Set<String>,done:Boolean)=rows.map{if(it.id in ids)it.copy(done=done)else it}
 fun move(rows:List<CallChecklist.Item>,id:String,step:Int):List<CallChecklist.Item>{require(step==1||step== -1);val i=rows.indexOfFirst{it.id==id};if(i<0||i+step !in rows.indices)return rows;return rows.toMutableList().apply{add(i+step,removeAt(i))}}
 class Draft(initial:List<CallChecklist.Item>){var rows=initial.toList();private set
  private val past=mutableListOf<List<CallChecklist.Item>>()
  init{validate(initial,initial)}
  val canUndo get()=past.isNotEmpty()
  fun change(next:List<CallChecklist.Item>):Boolean{validate(rows,next);if(next==rows)return false;past.add(rows);if(past.size>40)past.removeAt(0);rows=next.toList();return true}
  fun undo(){if(past.isNotEmpty())rows=past.removeAt(past.lastIndex)}
 }
}
