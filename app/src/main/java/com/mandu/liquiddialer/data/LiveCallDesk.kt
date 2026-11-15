package com.mandu.liquiddialer.data

import android.telecom.Call
import com.mandu.liquiddialer.telecom.CallManager

/** No persistence or Telecom command of its own. Every control rechecks the captured call. */
object LiveCallDesk {
 fun eligible(settings:LdSettings,target:Call):Boolean = try {
  settings.liveCallDesk && FeatureControls.enabled(settings,"notes") &&
   CallTouchGuard.eligibleCall(true,target) && target.details.connectTimeMillis>0 &&
   runCatching { CallLists.number(CallManager.number(target)) }.isSuccess
 } catch(_:Exception){false}
 class Session internal constructor(val target:Call,val number:String,private val connectedAt:Long) {
  fun current(settings:LdSettings):Boolean = eligible(settings,target) &&
   CallManager.number(target)==number && target.details.connectTimeMillis==connectedAt
  fun dispatch(settings:LdSettings,action:()->Unit):Boolean {
   if(!current(settings))return false
   action();return true
  }
 }
 fun open(settings:LdSettings,target:Call):Session? = if(eligible(settings,target))
  Session(target,CallManager.number(target),target.details.connectTimeMillis) else null
}
