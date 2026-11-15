package com.mandu.liquiddialer.data

import android.content.Context
import com.mandu.liquiddialer.LdApp

/** Manual labels only. No audio, persistence, clock ticker or Telecom command here. */
object CallCheckpoints {
 const val MAX=12
 data class Mark(val seconds:Long,val label:String)
 data class Board(val marks:List<Mark> = emptyList(),val saved:Boolean=false){
  fun add(seconds:Long,label:String):Board {
   require(!saved && marks.size<MAX);require(seconds in 0..604800)
   val text=label.trim();require(text.length<=80&&text.none{it.isISOControl()})
   require(marks.isEmpty()||seconds>=marks.last().seconds){"Clock moved back; no mark added"}
   return copy(marks=marks+Mark(seconds,text.ifEmpty{"Checkpoint"}))
  }
  fun rename(index:Int,label:String):Board {
   require(!saved&&index in marks.indices)
   val text=label.trim();require(text.length<=80&&text.none{it.isISOControl()})
   return copy(marks=marks.mapIndexed{i,m->if(i==index)m.copy(label=text.ifEmpty{"Checkpoint"})else m})
  }
  fun remove(index:Int):Board {require(!saved&&index in marks.indices);return copy(marks=marks.filterIndexed{i,_->i!=index})}
  fun summary():String {
   require(marks.isNotEmpty()&&marks.size<=MAX)
   marks.forEach{require(it.seconds in 0..604800&&it.label.isNotBlank()&&it.label.length<=80&&it.label.none{c->c.isISOControl()})}
   require(marks.zipWithNext().all{it.first.seconds<=it.second.seconds})
   return "Manual call checkpoints (elapsed from connection, not a recording):\n"+marks.joinToString("\n"){"${time(it.seconds)} | ${it.label}"}
  }
 }
 fun rename(session:LiveCallDesk.Session,expected:Board,current:Board,index:Int,label:String):Board?=runCatching{
  val s=LdApp.settings.value
  require(s.checkpointLabels&&s.callCheckpoints&&session.current(s)&&expected==current)
  current.rename(index,label)
 }.getOrNull()
 fun time(seconds:Long):String{require(seconds in 0..604800);return "%d:%02d:%02d".format(java.util.Locale.ROOT,seconds/3600,(seconds%3600)/60,seconds%60)}
 fun elapsed(session:LiveCallDesk.Session,now:Long):Long?=runCatching{
  require(session.current(LdApp.settings.value)&&LdApp.settings.value.callCheckpoints)
  val start=session.target.details.connectTimeMillis;require(now>=start)
  ((now-start)/1000).also{require(it in 0..604800)}
 }.getOrNull()
 /** Exact preview board, exact call captured, one successful save for this board. */
 class Commit {
  private var completed=false
  @Synchronized fun save(ctx:Context,session:LiveCallDesk.Session,expected:Board,current:Board):Boolean {
   if(completed||expected!=current||current.saved||!LdApp.settings.value.callCheckpoints||!session.current(LdApp.settings.value))return false
   val text=current.summary()
   Repo.addNote(ctx,session.number,"",text)
   completed=true;return true
  }
 }
}
