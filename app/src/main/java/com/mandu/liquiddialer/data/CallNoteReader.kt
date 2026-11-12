package com.mandu.liquiddialer.data

/** Only exact full-number identity may enter the call-time reader. */
object CallNoteReader {
 const val PAGE_SIZE=12
 fun eligible(number:String):Boolean=runCatching{CallLists.number(number);ContactControls.k(number).isNotEmpty()}.getOrDefault(false)
 fun notes(all:List<Note>,number:String,query:String=""):List<Note>{
  require(eligible(number));val key=ContactControls.k(number);val q=query.trim().take(200)
  return all.filter{ContactControls.k(it.number)==key&&(q.isEmpty()||it.text.contains(q,true))}.sortedWith(compareByDescending<Note>{it.time}.thenByDescending{it.id})
 }
 fun page(notes:List<Note>,requested:Int):List<Note> {val p=requested.coerceIn(0,lastPage(notes.size));return notes.drop(p*PAGE_SIZE).take(PAGE_SIZE)}
 fun lastPage(size:Int)=((size-1).coerceAtLeast(0)/PAGE_SIZE)
 fun preview(text:String)=text.take(220).let{if(text.length>220)it+"…"else it}
}
