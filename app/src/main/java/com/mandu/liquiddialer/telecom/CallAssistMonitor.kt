package com.mandu.liquiddialer.telecom

import android.os.Handler
import android.telecom.CallAudioState

/** No polling and no audio writes. Runs in InCallService independently of an Activity. */
internal class CallAssistMonitor(private val handler:Handler,private val read:()->Input,
 private val notify:(Kind,Int)->Unit,private val cancel:(Kind?)->Unit) {
 enum class Kind {MUTE,ROUTE}
 data class Input(val activeSingleCall:Any?,val muted:Boolean,val route:Int,val muteEnabled:Boolean,val routeEnabled:Boolean)
 private var call:Any?=null
 private var previousRoute=0
 private var mutePending:Runnable?=null
 private var muteSent=false
 private fun resetMute(){mutePending?.let{handler.removeCallbacks(it)};mutePending=null;muteSent=false;cancel(Kind.MUTE)}
 fun stop(){resetMute();call=null;previousRoute=0;cancel(null)}
 fun refresh(){
  val s=read()
  if(s.activeSingleCall==null){stop();return}
  if(call!==s.activeSingleCall){stop();call=s.activeSingleCall;previousRoute=s.route}
  if(!s.muteEnabled || !s.muted)resetMute()
  else if(!muteSent && mutePending==null){
   val target=call
   val r=Runnable {
    mutePending=null
    val live=read()
    if(live.activeSingleCall===target && live.muteEnabled && live.muted){muteSent=true;notify(Kind.MUTE,live.route)}
   }
   mutePending=r;handler.postDelayed(r,30_000)
  }
  if(!s.routeEnabled)cancel(Kind.ROUTE)
  else if(previousRoute in listOf(CallAudioState.ROUTE_BLUETOOTH,CallAudioState.ROUTE_WIRED_HEADSET) &&
     s.route in listOf(CallAudioState.ROUTE_EARPIECE,CallAudioState.ROUTE_SPEAKER))notify(Kind.ROUTE,s.route)
  if(s.route in listOf(CallAudioState.ROUTE_BLUETOOTH,CallAudioState.ROUTE_WIRED_HEADSET))cancel(Kind.ROUTE)
  previousRoute=s.route
 }
}
