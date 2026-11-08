package com.mandu.liquiddialer.data

/** Search copies of local data. Stable source IDs are kept for exact-item navigation. */
object GlobalSearch {
 enum class Kind { CONTACT, NOTE, LIST_ENTRY, REMINDER }
 data class Hit(val kind:Kind,val id:String,val parentId:String="",val title:String,val number:String,val detail:String)
 fun find(query:String,contacts:List<Contact>,notes:List<Note>,lists:List<CallLists.ListItem>,reminders:List<CallReminders.Reminder>):List<Hit>{
  val q=query.trim();if(q.isEmpty())return emptyList()
  val digits=q.filter(Char::isDigit);val numeric=digits.isNotEmpty() && q.all{it.isDigit() || it in "+-(). "}
  fun match(vararg values:String)=values.any{it.contains(q,true)} || (numeric && values.any{it.filter(Char::isDigit).contains(digits)})
  val out=ArrayList<Hit>()
  contacts.forEach{if(match(it.name,it.altName,it.number))out+=Hit(Kind.CONTACT,it.id.toString(),title=it.name.ifBlank{it.number},number=it.number,detail=it.label)}
  notes.forEach{if(match(it.name,it.number,it.text))out+=Hit(Kind.NOTE,it.id.toString(),title=it.name.ifBlank{it.number},number=it.number,detail=it.text)}
  lists.forEach{l->l.entries.forEach{e->if(match(l.title,e.name,e.number,e.note))out+=Hit(Kind.LIST_ENTRY,e.id,l.id,e.name.ifBlank{e.number},e.number,l.title+" · "+CallLists.outcomes.getValue(e.outcome)+if(e.note.isBlank())"" else " · "+e.note)}}
  reminders.forEach{if(match(it.name,it.number))out+=Hit(Kind.REMINDER,it.token,title=it.name.ifBlank{it.number},number=it.number,detail="")}
  return out
 }
}
