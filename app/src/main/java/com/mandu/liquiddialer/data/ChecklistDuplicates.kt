package com.mandu.liquiddialer.data

/** Exact case/whitespace matching only, never a semantic similarity guess. */
object ChecklistDuplicates {
 data class Group(val key:String,val items:List<CallChecklist.Item>,val positions:List<Int>){val kept get()=items.first().copy(done=items.all{it.done})}
 data class Preview(val source:List<CallChecklist.Item>,val selected:Set<String>,val groups:List<Group>,val result:List<CallChecklist.Item>){val removed get()=source.size-result.size}
 private fun validate(rows:List<CallChecklist.Item>){
  require(rows.size<=CallChecklist.MAX_ITEMS&&rows.map{it.id}.distinct().size==rows.size){"Invalid checklist identities"}
  require(rows.all{it.id.isNotBlank()&&it.text.trim().isNotEmpty()&&it.text.length<=CallChecklist.MAX_TEXT&&it.text.none{c->c.isISOControl()&&c!='\n'&&c!='\r'&&c!='\t'}}){"Invalid checklist topic"}
 }
 fun groups(rows:List<CallChecklist.Item>):List<Group>{
  validate(rows)
  return rows.withIndex().groupBy{ChecklistTemplates.normalized(it.value.text)}.filterValues{it.size>1}.map{(key,values)->Group(key,values.map{it.value},values.map{it.index+1})}
 }
 fun preview(rows:List<CallChecklist.Item>,selected:Set<String>):Preview {
  val all=groups(rows);require(selected.isNotEmpty()&&selected.all{k->all.any{it.key==k}}){"Select current duplicate groups first"}
  val chosen=all.filter{it.key in selected};val removed=chosen.flatMap{it.items.drop(1)}.map{it.id}.toSet();val kept=chosen.associate{it.items.first().id to it.kept}
  val next=rows.filterNot{it.id in removed}.map{kept[it.id]?:it};validate(next)
  return Preview(rows.toList(),selected.toSet(),chosen,next)
 }
 fun apply(current:List<CallChecklist.Item>,view:Preview):List<CallChecklist.Item>{
  require(current==view.source){"Checklist changed. Reload and preview again; nothing was removed."}
  val verified=preview(current,view.selected);require(verified==view){"Preview changed. Preview again."};return verified.result
 }
}
