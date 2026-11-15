package com.mandu.liquiddialer.data
import android.content.Context
import com.mandu.liquiddialer.LdApp

/** Monotonic manual intervals. No audio, Telecom command, persisted timer or background job. */
object DeskStopwatch {
 const val MAX_MS=604800000L
 data class Lap(val totalMs:Long,val label:String)
 data class Board(val accumulatedMs:Long=0,val runningSince:Long?=null,val laps:List<Lap> = emptyList(),val saved:Boolean=false){
  fun validate(){
   require(accumulatedMs in 0..MAX_MS&&laps.size<=12)
   require(runningSince==null||runningSince>=0)
   require(!saved||runningSince==null)
   var previous=0L
   laps.forEach{require(it.totalMs>previous&&it.totalMs<=accumulatedMs);require(it.label.isNotBlank()&&it.label.length<=80&&it.label.none{c->c.isISOControl()});previous=it.totalMs}
  }
  fun elapsed(now:Long):Long {validate();require(now>=0);val start=runningSince?:return accumulatedMs;require(now>=start){"Monotonic clock moved backwards"};val delta=now-start;require(delta<=MAX_MS-accumulatedMs){"Seven-day timer limit"};return accumulatedMs+delta}
  fun start(now:Long):Board {validate();require(!saved&&runningSince==null&&now>=0&&accumulatedMs<MAX_MS);return copy(runningSince=now)}
  fun pause(now:Long):Board {require(!saved&&runningSince!=null);return copy(accumulatedMs=elapsed(now),runningSince=null)}
  fun lap(now:Long,label:String):Board {
   require(!saved&&runningSince!=null&&laps.size<12)
   val total=elapsed(now);require(total>(laps.lastOrNull()?.totalMs?:0L)){"Wait before marking another lap"}
   val text=label.trim();require(text.length<=80&&text.none{it.isISOControl()})
   return copy(accumulatedMs=total,runningSince=now,laps=laps+Lap(total,text.ifEmpty{"Interval ${laps.size+1}"}))
  }
  fun summary():String {
   validate();require(runningSince==null&&accumulatedMs>0)
   var previous=0L
   val rows=laps.mapIndexed{i,l->val delta=l.totalMs-previous;previous=l.totalMs;"${i+1}. ${l.label} | lap ${time(delta)} | total ${time(l.totalMs)}"}.toMutableList()
   if(accumulatedMs>previous)rows+="Unmarked remainder | ${time(accumulatedMs-previous)}"
   return "Manual desk stopwatch (not call duration, billing or recording):\nTimed total: ${time(accumulatedMs)}\nManual pauses excluded. Precision shown: 0.1 second.\n"+rows.joinToString("\n")
  }
 }
 fun time(ms:Long):String {require(ms in 0..MAX_MS);if(ms in 1..99)return "<0.1s";return "%d:%02d:%02d.%d".format(java.util.Locale.ROOT,ms/3600000,(ms/60000)%60,(ms/1000)%60,(ms/100)%10)}
 fun allowed(session:LiveCallDesk.Session)=LdApp.settings.value.deskStopwatch&&session.current(LdApp.settings.value)
 fun change(session:LiveCallDesk.Session,current:Board,now:Long,action:String,label:String=""):Board {
  require(allowed(session)){"Call or setting changed"}
  return when(action){"start"->current.start(now);"pause"->current.pause(now);"lap"->current.lap(now,label);else->throw IllegalArgumentException("Unknown action")}
 }
 class Commit {
  private var completed=false
  @Synchronized fun save(ctx:Context,session:LiveCallDesk.Session,expected:Board,current:Board):Boolean {
   if(completed||!allowed(session)||expected!=current||current.saved)return false
   val text=current.summary();Repo.addNote(ctx,session.number,"",text);completed=true;return true
  }
 }
}
