package com.mandu.liquiddialer.data
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import androidx.datastore.preferences.core.MutablePreferences

/** Monday=0. Overnight windows belong to their START day. No alarms or background jobs. */
object QuietWeek{
 const val MAX=8
 val days=listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
 data class Window(val id:String,val name:String,val days:Set<Int>,val start:Int,val end:Int,val enabled:Boolean=true){
  fun validate(){require(id.matches(Regex("[A-Za-z0-9-]{1,60}")));require(name.trim().isNotBlank()&&name.length<=40&&name.none{it.isISOControl()});require(days.isNotEmpty()&&days.all{it in 0..6});require(start in 0..1439&&end in 0..1439&&start!=end)}
  fun matches(day:Int,minute:Int):Boolean{if(!enabled||day !in 0..6||minute !in 0..1439)return false;return if(start<end)day in days&&minute>=start&&minute<end else(day in days&&minute>=start)||((day+6)%7 in days&&minute<end)}
 }
 fun decode(raw:String):List<Window>{if(raw.isEmpty())return emptyList();require(raw.length<=20000&&'\u0000' !in raw);val t=JSONTokener(raw);val root=t.nextValue()as?JSONObject?:error("Invalid schedule");require(t.nextClean()=='\u0000');require(root.get("version")==1);val a=root.getJSONArray("windows");require(a.length()<=MAX);val out=(0 until a.length()).map{i->val o=a.getJSONObject(i);require(o.get("enabled")is Boolean&&o.get("id")is String&&o.get("name")is String);val ds=o.getJSONArray("days");require(ds.length()in 1..7);val d=(0 until ds.length()).map{j->require(ds.get(j)is Int);ds.getInt(j)};require(d.distinct().size==d.size);require(o.get("start")is Int&&o.get("end")is Int);Window(o.getString("id"),o.getString("name"),d.toSet(),o.getInt("start"),o.getInt("end"),o.getBoolean("enabled")).also{it.validate()}};require(out.map{it.id}.distinct().size==out.size);return out}
 fun encode(xs:List<Window>):String{require(xs.size<=MAX);val a=JSONArray();xs.forEach{w->w.validate();a.put(JSONObject().put("id",w.id).put("name",w.name).put("days",JSONArray(w.days.sorted())).put("start",w.start).put("end",w.end).put("enabled",w.enabled))};return JSONObject().put("version",1).put("windows",a).toString().also{decode(it)}}
 fun active(raw:String,day:Int,minute:Int)=runCatching{decode(raw).any{it.matches(day,minute)}}.getOrDefault(false)
 fun time(text:String):Int{require(text.matches(Regex("[0-2][0-9]:[0-5][0-9]")));val h=text.take(2).toInt();require(h<24);return h*60+text.takeLast(2).toInt()}
 fun fmt(m:Int)="%02d:%02d".format(java.util.Locale.ROOT,m/60,m%60)
 fun segments(xs:List<Window>,day:Int):List<String>{require(day in 0..6);return xs.filter{it.enabled}.flatMap{w->if(w.start<w.end){if(day in w.days)listOf("${fmt(w.start)}-${fmt(w.end)} · ${w.name}")else emptyList()}else buildList{if((day+6)%7 in w.days)add("00:00-${fmt(w.end)} · ${w.name} (pichle din se)");if(day in w.days)add("${fmt(w.start)}-24:00 · ${w.name}")}}}
 fun disable(p:MutablePreferences,expectedRaw:String,expectedMode:Boolean){require((p[K.quietWeekRules]?:"")==expectedRaw&&(p[K.quietWeekMode]?:false)==expectedMode){"Saved schedule changed"};p[K.quietWeekMode]=false}
 fun save(p:MutablePreferences,expectedRaw:String,expectedMode:Boolean,windows:List<Window>,mode:Boolean){val raw=encode(windows);require(!mode||windows.any{it.enabled});require((p[K.quietWeekRules]?:"")==expectedRaw&&(p[K.quietWeekMode]?:false)==expectedMode){"Saved schedule changed"};p[K.quietWeekRules]=raw;p[K.quietWeekMode]=mode}
}
