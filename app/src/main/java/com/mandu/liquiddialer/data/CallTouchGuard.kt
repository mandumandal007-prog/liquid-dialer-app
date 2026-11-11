package com.mandu.liquiddialer.data

import android.telecom.Call
import android.telecom.VideoProfile
import com.mandu.liquiddialer.telecom.CallManager

/** View-only guard. No Telecom commands, persistence, timers or services. */
object CallTouchGuard {
 fun eligible(enabled:Boolean,active:Boolean,onlyTarget:Boolean,conference:Boolean,hasParent:Boolean,hasChildren:Boolean,video:Boolean,upgrade:Boolean):Boolean =
  enabled && active && onlyTarget && !conference && !hasParent && !hasChildren && !video && !upgrade
 fun eligibleCall(enabled:Boolean,target:Call):Boolean = try {
  val calls=CallManager.calls.value
  val d=target.details
  // Unknown details fail closed: do not obscure potentially important controls.
  d!=null && eligible(enabled,CallManager.state(target)==Call.STATE_ACTIVE,
   calls.size==1 && calls.single()===target,
   d.hasProperty(Call.Details.PROPERTY_CONFERENCE),target.parent!=null,target.children.isNotEmpty(),
   d.videoState!=VideoProfile.STATE_AUDIO_ONLY,CallManager.videoUpgradeFrom.value!=null)
 } catch(_:Exception){false}
 data class State(val locked:Boolean=false,val armedAt:Long?=null){
  fun lock()=State(true)
  fun arm(now:Long)=if(locked && now>=0)copy(armedAt=now)else this
  fun cancelUnlock()=copy(armedAt=null)
  fun unlock(now:Long):State = if(locked && armedAt!=null && now>=armedAt && now-armedAt<5000)State()else cancelUnlock()
 }
}
