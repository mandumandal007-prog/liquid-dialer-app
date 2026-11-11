package com.mandu.liquiddialer.data

import androidx.datastore.preferences.core.MutablePreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Deliberately excludes calling behavior, identities, permissions, feature visibility and button order. */
data class LookProfile(val id:String,val name:String,val look:Look)
data class Look(val theme:String,val accent:Long,val blur:Int,val depth:Int,val frost:Int,val motion:Boolean,val font:Float,val bold:Boolean,val fontStyle:Int,val icons:Int,val clean:Boolean,val quiet:Boolean,val refined:Boolean,val airy:Boolean,val confirm:Boolean,val dialer:Int){
 fun settings(base:LdSettings)=base.copy(theme=theme,accent=accent,blur=blur,glassDepth=depth,glassFrost=frost,motion=motion,fontScale=font,boldText=bold,fontStyle=fontStyle,iconStyle=icons,cleanGlass=clean,quietRows=quiet,refinedHeaders=refined,airyCall=airy,premiumConfirm=confirm,dialerLayout=dialer)
 fun apply(p:MutablePreferences){validate();p[K.theme]=theme;p[K.accent]=accent;p[K.blur]=blur;p[K.glassDepth]=depth;p[K.glassFrost]=frost;p[K.motion]=motion;p[K.fontScale]=font;p[K.boldText]=bold;p[K.fontStyle]=fontStyle;p[K.iconStyle]=icons;p[K.cleanGlass]=clean;p[K.quietRows]=quiet;p[K.refinedHeaders]=refined;p[K.airyCall]=airy;p[K.premiumConfirm]=confirm;p[K.dialerLayout]=dialer}
 fun validate(){require(theme in setOf("midnight","aurora","sunset","ocean","rose","daylight","sand","auto"));require(accent in 0xFF000000L..0xFFFFFFFFL);require(blur in 0..40&&depth in 0..2&&frost in 0..2&&font.isFinite()&&font in .8f..1.5f&&fontStyle in 0..3&&icons in 0..7&&dialer in 0..2)}
 fun json()=JSONObject().put("theme",theme).put("accent",accent).put("blur",blur).put("depth",depth).put("frost",frost).put("motion",motion).put("font",font).put("bold",bold).put("fontStyle",fontStyle).put("icons",icons).put("clean",clean).put("quiet",quiet).put("refined",refined).put("airy",airy).put("confirm",confirm).put("dialer",dialer)
 companion object{
  fun capture(s:LdSettings)=Look(s.theme,s.accent,s.blur,s.glassDepth,s.glassFrost,s.motion,s.fontScale,s.boldText,s.fontStyle,s.iconStyle,s.cleanGlass,s.quietRows,s.refinedHeaders,s.airyCall,s.premiumConfirm,s.dialerLayout)
  fun read(o:JSONObject)=Look(o.getString("theme"),o.getLong("accent"),o.getInt("blur"),o.getInt("depth"),o.getInt("frost"),o.getBoolean("motion"),o.getDouble("font").toFloat(),o.getBoolean("bold"),o.getInt("fontStyle"),o.getInt("icons"),o.getBoolean("clean"),o.getBoolean("quiet"),o.getBoolean("refined"),o.getBoolean("airy"),o.getBoolean("confirm"),o.getInt("dialer")).also{it.validate()}
 }
}
object LookProfiles{
 const val MAX=6
 fun decode(raw:String):List<LookProfile>{if(raw.isEmpty())return emptyList();require(raw.length<=24000);val o=JSONObject(raw);require(o.getInt("version")==1);val a=o.getJSONArray("profiles");require(a.length()<=MAX);val rows=(0 until a.length()).map{i->val r=a.getJSONObject(i);LookProfile(r.getString("id").also{require(it.matches(Regex("[A-Za-z0-9-]{1,60}")))},name(r.getString("name")),Look.read(r.getJSONObject("look")))};require(rows.map{it.id}.distinct().size==rows.size);return rows}
 fun encode(rows:List<LookProfile>):String{require(rows.size<=MAX);return JSONObject().put("version",1).put("profiles",JSONArray().also{a->rows.forEach{r->r.look.validate();a.put(JSONObject().put("id",r.id).put("name",name(r.name)).put("look",r.look.json()))}}).toString().also{decode(it)}}
 fun name(s:String)=s.trim().also{require(it.isNotBlank()&&it.length<=32&&it.none{c->c.isISOControl()})}
 fun add(rows:List<LookProfile>,name:String,look:Look):List<LookProfile>{require(rows.size<MAX);look.validate();return rows+LookProfile(UUID.randomUUID().toString(),name(name),look)}
 fun rename(rows:List<LookProfile>,id:String,n:String):List<LookProfile>{require(rows.any{it.id==id});val clean=name(n);return rows.map{if(it.id==id)it.copy(name=clean)else it}}
 fun remove(rows:List<LookProfile>,id:String):List<LookProfile>{require(rows.any{it.id==id});return rows.filterNot{it.id==id}}
 fun presets(base:LdSettings):List<LookProfile>{val b=Look.capture(base);return listOf(
  LookProfile("calm","Calm Glass",b.copy(theme="daylight",quiet=true,refined=true,depth=1,frost=1,icons=6,motion=false,airy=true,confirm=true,fontStyle=0)),
  LookProfile("royal","Royal Night",b.copy(theme="midnight",quiet=false,refined=true,depth=2,frost=1,icons=7,airy=true,confirm=true,fontStyle=0)),
  LookProfile("read","Easy Read",b.copy(theme="daylight",quiet=true,refined=true,depth=1,frost=2,icons=6,font=1.2f,bold=true,fontStyle=0,clean=true,motion=false))) }
}
