package com.mandu.liquiddialer.data
import org.json.JSONObject

/** Cosmetic only. Bounded preset IDs, no action or permission settings. */
data class CallIconChoice(val style:Int=-1,val color:Int=0)
data class CallIconConfig(val base:CallIconChoice=CallIconChoice(),val overrides:Map<String,CallIconChoice> = emptyMap(),val polish:Boolean=false) {
 fun choice(id:String)=overrides[id] ?: base
 fun encode():String {val o=JSONObject();overrides.forEach{(k,v)->o.put(k,JSONObject().put("style",v.style).put("color",v.color))};return JSONObject().put("style",base.style).put("color",base.color).put("polish",polish).put("icons",o).toString()}
 companion object {
  val ids=setOf("chevron-down","video","edit","mic","dialpad","record","pause","plus","phone","volume","speaker-fill","call-end","bluetooth","merge","swap","users","x","message","sim","check","phone-out")
  fun decode(text:String):CallIconConfig=try {
   require(text.length<=6000);val j=JSONObject(text)
   fun choice(o:JSONObject):CallIconChoice {val s=o.optInt("style",-1);val c=o.optInt("color",0);require(s in -1..7 && c in 0..6);return CallIconChoice(s,c)}
   val o=j.optJSONObject("icons");val m=linkedMapOf<String,CallIconChoice>();o?.keys()?.forEach{k->require(k in ids);m[k]=choice(o.getJSONObject(k))}
   CallIconConfig(choice(j),m,j.optBoolean("polish",false))
  }catch(_:Exception){CallIconConfig()}
 }
}
