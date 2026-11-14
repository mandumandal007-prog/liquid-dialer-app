package com.mandu.liquiddialer.data
import org.json.JSONObject

/** Incoming app-card overrides only. No Telecom, permissions, contacts or ring-volume effects. */
object IncomingCard {
 data class Config(val enabled:Boolean=false,val style:String="glass",val background:String="inherit",val avatar:String="circle",val avatarSize:Int=100,val nameSize:Int=100,val number:Boolean=true,val contactType:Boolean=true,val lastCall:Boolean=true,val message:Boolean=true,val whatsapp:Boolean=true,val swap:Boolean=false,val swipe:Boolean=true,val travel:Int=110,val confirmCut:Boolean=false)
 val styles=setOf("glass","filigree","cyber","jadegold","pearl","pastel","aurorapanel")
 fun validate(c:Config):Config{require(c.style in styles&&c.background in setOf("inherit","blobs","solid","photo")&&c.avatar in setOf("circle","squircle"));require(c.avatarSize in setOf(75,100,125)&&c.nameSize in setOf(85,100,115)&&c.travel in setOf(70,110,150));return c}
 fun encode(c:Config):String{validate(c);return JSONObject().put("version",1).put("enabled",c.enabled).put("style",c.style).put("background",c.background).put("avatar",c.avatar).put("avatarSize",c.avatarSize).put("nameSize",c.nameSize).put("number",c.number).put("contactType",c.contactType).put("lastCall",c.lastCall).put("message",c.message).put("whatsapp",c.whatsapp).put("swap",c.swap).put("swipe",c.swipe).put("travel",c.travel).put("confirmCut",c.confirmCut).toString()}
 fun decode(raw:String):Config=runCatching{
  require(raw.length in 1..3000);val t=org.json.JSONTokener(raw);val o=t.nextValue()as JSONObject;require(t.nextClean()=='\u0000');require(o.get("version")==1)
  fun bool(k:String)=(o.get(k)as Boolean)
  fun str(k:String)=(o.get(k)as String)
  fun num(k:String)=(o.get(k)as Int)
  validate(Config(bool("enabled"),str("style"),str("background"),str("avatar"),num("avatarSize"),num("nameSize"),bool("number"),bool("contactType"),bool("lastCall"),bool("message"),bool("whatsapp"),bool("swap"),bool("swipe"),num("travel"),bool("confirmCut")))
 }.getOrDefault(Config())
 fun effective(s:LdSettings,ringing:Boolean,c:Config=decode(s.incomingCard)):LdSettings{if(!ringing||!c.enabled)return s;return s.copy(callCardStyle=c.style,callAvatarShape=c.avatar,callShowNumber=c.number,showContactType=c.contactType,showLastCall=c.lastCall,waPill=c.whatsapp,swipeAnswer=c.swipe)}
}
