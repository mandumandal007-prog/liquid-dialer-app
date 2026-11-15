package com.mandu.liquiddialer.data
import com.mandu.liquiddialer.LdApp

/** Read-only analysis of explicitly chosen completed lap durations, never cumulative totals. */
object LapCompare {
 data class Row(val index:Int,val label:String,val durationMs:Long,val totalMs:Long)
 data class Result(val rows:List<Row>,val subtotalMs:Long,val averageMs:Long,val shortestMs:Long,val longestMs:Long,val remainderMs:Long)
 fun rows(board:DeskStopwatch.Board):List<Row>{
  board.validate();require(board.runningSince==null){"Pause stopwatch first"}
  return board.laps.mapIndexed{i,l->Row(i,l.label,l.totalMs-(board.laps.getOrNull(i-1)?.totalMs?:0),l.totalMs)}
 }
 fun analyze(board:DeskStopwatch.Board,selected:Set<Int>):Result {
  val all=rows(board);require(selected.isNotEmpty()&&selected.all{it in all.indices}){"Choose at least one measured lap"}
  val chosen=all.filter{it.index in selected};val sum=chosen.sumOf{it.durationMs}
  return Result(chosen,sum,sum/chosen.size,chosen.minOf{it.durationMs},chosen.maxOf{it.durationMs},board.accumulatedMs-(board.laps.lastOrNull()?.totalMs?:0))
 }
 fun ordered(rows:List<Row>,longestFirst:Boolean)=if(longestFirst)rows.sortedWith(compareByDescending<Row>{it.durationMs}.thenBy{it.index})else rows.sortedBy{it.index}
 fun current(session:LiveCallDesk.Session,expected:DeskStopwatch.Board,current:DeskStopwatch.Board):Boolean =
  LdApp.settings.value.lapCompare&&DeskStopwatch.allowed(session)&&expected==current&&current.runningSince==null
}
