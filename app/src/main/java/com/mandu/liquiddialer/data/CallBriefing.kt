package com.mandu.liquiddialer.data

import android.content.Context
import kotlinx.coroutines.CancellationException

/** Independent local-source reads; a broken section is never represented as empty. No writes. */
object CallBriefing {
 data class Section<T>(val value:T?=null,val error:Boolean=false)
 data class ListEntry(val listId:String,val title:String,val entry:CallLists.Entry)
 data class Snapshot(val number:String,val notes:Section<List<Note>>,val topics:Section<List<CallChecklist.Item>>,val estimate:Section<CallEstimate.Draft>,val lists:Section<List<ListEntry>>)
 private fun key(number:String):String {CallLists.number(number);return ContactControls.k(number).also{require(it.isNotEmpty())}}
 private fun same(a:String,k:String)=runCatching{key(a)}.getOrNull()==k
 fun notes(number:String,all:List<Note>):List<Note>{val k=key(number);return all.filter{same(it.number,k)}.sortedWith(compareByDescending<Note>{it.time}.thenByDescending{it.id})}
 fun lists(number:String,all:List<CallLists.ListItem>):List<ListEntry>{val k=key(number);return all.flatMap{list->list.entries.filter{same(it.number,k)}.map{ListEntry(list.id,list.title,it)}}.sortedWith(compareBy<ListEntry>{it.entry.done}.thenByDescending{it.entry.updatedAt}.thenBy{it.title})}
 private fun <T> read(action:()->T):Section<T> = try{Section(action())}catch(e:CancellationException){throw e}catch(_:Exception){Section(error=true)}
 fun load(ctx:Context,number:String):Snapshot {
  key(number)
  return Snapshot(number,read{notes(number,Repo.notesChecked(ctx))},read{CallChecklist.read(ctx,number)},read{CallEstimate.read(ctx,number)},read{lists(number,CallLists.read(ctx))})
 }
 fun match(text:String,query:String)=query.trim().let{it.isEmpty()||text.contains(it,ignoreCase=true)}
 fun visibleNotes(s:Snapshot,q:String)=s.notes.value.orEmpty().filter{match(it.text,q)}
 fun visibleTopics(s:Snapshot,q:String,pendingOnly:Boolean)=s.topics.value.orEmpty().filter{(!pendingOnly||!it.done)&&match(it.text,q)}
 fun visibleLists(s:Snapshot,q:String)=s.lists.value.orEmpty().filter{match(it.title+"\n"+it.entry.name+"\n"+it.entry.note+"\n"+(CallLists.outcomes[it.entry.outcome]?:""),q)}
}
