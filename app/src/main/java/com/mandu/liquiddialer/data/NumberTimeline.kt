package com.mandu.liquiddialer.data

import android.content.Context
import java.util.Calendar

/** Read-only exact-identity view over bounded keyset pages, never a complete-history claim. */
object NumberTimeline {
 const val MAX_PAGES=20
 data class Batch(val rows:List<Recent>,val scanned:Int,val next:HistoryExplorer.Anchor?)
 data class Summary(val calls:Int,val connectedSeconds:Long,val incoming:Int,val outgoing:Int,val missed:Int,val rejected:Int)
 fun filter(days:Int,type:Int,now:Long):HistoryExplorer.Filter {
  require(days in listOf(0,7,30)&&type in listOf(0,1,2,3,5,6));require(now in 1 until Long.MAX_VALUE)
  val start=if(days==0)0 else Calendar.getInstance().apply{timeInMillis=now;add(Calendar.DAY_OF_YEAR,-days)}.timeInMillis.coerceAtLeast(0)
  return HistoryExplorer.Filter(start,now+1,type)
 }
 fun exact(number:String):String {CallLists.number(number);return ContactControls.k(number).also{require(it.isNotEmpty())}}
 fun select(number:String,rows:List<Recent>):List<Recent>{val key=exact(number);return rows.filter{runCatching{exact(it.number)}.getOrNull()==key}}
 fun page(ctx:Context,number:String,filter:HistoryExplorer.Filter,after:HistoryExplorer.Anchor?):Batch {
  exact(number);val p=HistoryExplorer.page(ctx,filter,after);require(p.rows.all{it.duration>=0});return Batch(select(number,p.rows),p.rows.size,p.next)
 }
 fun summary(rows:List<Recent>):Summary {
  require(rows.all{it.duration>=0});var duration=0L
  rows.filter{it.type in listOf(1,2)}.forEach{require(it.duration<=Long.MAX_VALUE-duration);duration+=it.duration}
  return Summary(rows.size,duration,rows.count{it.type==1},rows.count{it.type==2},rows.count{it.type==3},rows.count{it.type==5})
 }
}
