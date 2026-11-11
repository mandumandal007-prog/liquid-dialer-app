package com.mandu.liquiddialer.telecom
import android.os.Handler
import android.os.SystemClock
import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow

/** One scheduled deadline, no polling. Consecutive eligible active stretch, never billed time. */
object CallPace {
 val intervals=listOf(1,3,5,10,15)
 data class Status(val target:Any,val deadline:Long,val minutes:Int,val reminded:Boolean)
 val status=MutableStateFlow<Status?>(null)
 fun eligible(calls:List<Call>):Call?=runCatching{calls.singleOrNull()?.takeIf{CallManager.state(it)==Call.STATE_ACTIVE&&it.parent==null&&!CallManager.isConference(it)&&it.children.isEmpty()}}.getOrNull()
}
internal class CallPaceMonitor(private val handler:Handler,private val read:()->Input,
 private val notify:(Int)->Unit,private val cancel:()->Unit,private val clock:()->Long={SystemClock.uptimeMillis()},
 private val publish:(CallPace.Status?)->Unit={CallPace.status.value=it}){
 data class Input(val target:Any?,val enabled:Boolean,val minutes:Int,val repeat:Boolean)
 private var current:Input?=null
 private var pending:Runnable?=null
 private var reminded=false
 fun stop(){pending?.let{handler.removeCallbacks(it)};pending=null;current=null;reminded=false;publish(null);cancel()}
 fun refresh(){val s=read();if(s.target==null||!s.enabled||s.minutes !in CallPace.intervals){stop();return}
  val old=current
  if(old==null||old.target!==s.target||old.minutes!=s.minutes||old.repeat!=s.repeat){stop();current=s;schedule(s)}
 }
 private fun schedule(expected:Input){
  val deadline=clock()+expected.minutes*60_000L
  publish(CallPace.Status(expected.target!!,deadline,expected.minutes,reminded))
  val r=Runnable{
   pending=null
   val live=read()
   if(live.target!==expected.target||!live.enabled||live.minutes!=expected.minutes||live.repeat!=expected.repeat){stop();refresh();return@Runnable}
   reminded=true;notify(expected.minutes)
   if(expected.repeat)schedule(expected)else publish(CallPace.Status(expected.target,0,expected.minutes,true))
  }
  pending=r;handler.postDelayed(r,expected.minutes*60_000L)
 }
}
