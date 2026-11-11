package com.mandu.liquiddialer.data
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Plain local menu choices, not passwords or automatic dial strings. */
object IvrMenus {
 const val MAX=12
 data class Menu(val id:String,val name:String,val digits:String){fun validate(){require(id.matches(Regex("[A-Za-z0-9-]{1,60}")));require(name.isNotBlank()&&name.length<=40&&name.none{it.isISOControl()});require(digits.length in 1..24&&digits.all{it in "0123456789*#"})}}
 fun decode(raw:String):List<Menu>{if(raw.isEmpty())return emptyList();require(raw.length<=12000&&'\u0000' !in raw);val t=JSONTokener(raw);val j=t.nextValue()as?JSONObject?:error("Invalid menu data");require(t.nextClean()=='\u0000'&&j.get("version")is Int&&j.getInt("version")==1);val a=j.getJSONArray("menus");require(a.length()<=MAX);val out=(0 until a.length()).map{val o=a.getJSONObject(it);require(o.get("id")is String&&o.get("name")is String&&o.get("digits")is String);Menu(o.getString("id"),o.getString("name"),o.getString("digits")).also{m->m.validate()}};require(out.map{it.id}.distinct().size==out.size);return out}
 fun encode(xs:List<Menu>):String{require(xs.size<=MAX);val a=JSONArray();xs.forEach{it.validate();a.put(JSONObject().put("id",it.id).put("name",it.name).put("digits",it.digits))};return JSONObject().put("version",1).put("menus",a).toString().also{decode(it)}}
 private fun prefs(ctx:Context)=ctx.getSharedPreferences("ivr_menus",Context.MODE_PRIVATE)
 @Synchronized fun raw(ctx:Context)=prefs(ctx).getString("data","")?:""
 @Synchronized fun save(ctx:Context,expected:String,xs:List<Menu>):String{val next=encode(xs);require(raw(ctx)==expected){"Saved menus changed; reload"};check(prefs(ctx).edit().putString("data",next).commit()){"Save failed"};return next}
 /** New selection starts at zero. Opening, selecting, reset and back never invoke send. */
 class Session(val menu:Menu){var index=0;private set
  init{menu.validate()}
  fun next():Char?=menu.digits.getOrNull(index)
  fun sendOne(send:(Char)->Boolean):Boolean{val c=next()?:return false;if(!send(c))return false;index++;return true}
  fun reset(){index=0}
 }
}
