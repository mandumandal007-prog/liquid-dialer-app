package com.mandu.liquiddialer.telecom
import android.os.Handler
import android.os.Looper
import android.telecom.Call

/** One explicit tap, one tone on the exact selected call. Never falls through to primary(). */
object IvrTone {
 private var busy=false
 fun allowed(target:Call):Boolean=runCatching{CallManager.calls.value.let{it.size==1&&it.single()===target}&&CallManager.primary()===target&&CallManager.state(target)==Call.STATE_ACTIVE&&target.parent==null&&!CallManager.isConference(target)&&target.children.isEmpty()}.getOrDefault(false)
 fun send(target:Call,digit:Char):Boolean{
  if(!com.mandu.liquiddialer.LdApp.settings.value.ivrHelper||digit !in "0123456789*#"||busy||!allowed(target))return false
  busy=true
  return try{target.playDtmfTone(digit);Handler(Looper.getMainLooper()).postDelayed({try{target.stopDtmfTone()}catch(_:Exception){}finally{busy=false}},150);true}catch(_:Exception){busy=false;false}
 }
}
