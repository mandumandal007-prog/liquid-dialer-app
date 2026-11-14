package com.mandu.liquiddialer.data
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

/** Explicit phone-only snapshots, not stock, orders, customer records or a live session autosave. */
object TallyBoards {
 const val MAX=6
 data class Saved(val id:String,val name:String,val rows:List<CallTally.Item>)
 data class Plan(val before:List<CallTally.Item>,val library:String,val id:String,val token:String,val after:List<CallTally.Item>)
 fun name(raw:String)=raw.trim().also{require(it.length in 1..32&&it.none{c->c.isISOControl()}){"Name: 1-32 characters, no line breaks"}}
 fun validate(all:List<Saved>):List<Saved>{require(all.size<=MAX);require(all.map{it.id}.distinct().size==all.size);require(all.map{it.name.lowercase(java.util.Locale.ROOT)}.distinct().size==all.size){"That saved board name already exists"};all.forEach{require(it.id.matches(Regex("[A-Za-z0-9-]{1,60}")));require(name(it.name)==it.name);require(it.rows.isNotEmpty());CallTally.validate(it.rows)};return all}
 fun encode(all:List<Saved>):String{validate(all);return JSONObject().put("version",1).put("boards",JSONArray().also{a->all.forEach{b->a.put(JSONObject().put("id",b.id).put("name",b.name).put("rows",JSONArray().also{r->b.rows.forEach{r.put(JSONObject().put("id",it.id).put("label",it.label).put("value",it.value))}}))}}).toString()}
 fun decode(raw:String):List<Saved>{if(raw.isEmpty())return emptyList();require(raw.length<=40000);val t=JSONTokener(raw);val o=t.nextValue()as JSONObject;require(t.nextClean()=='\u0000'&&o.get("version")is Int&&o.getInt("version")==1);val a=o.get("boards")as JSONArray;require(a.length()<=MAX);return validate((0 until a.length()).map{i->val b=a.getJSONObject(i);val r=b.get("rows")as JSONArray;require(r.length() in 1..12);Saved(b.get("id")as String,b.get("name")as String,(0 until r.length()).map{j->val x=r.getJSONObject(j);val v=x.get("value");require(v is Int||v is Long);CallTally.Item(x.get("id")as String,x.get("label")as String,(v as Number).toLong())})})}
 fun capture(all:List<Saved>,n:String,rows:List<CallTally.Item>):List<Saved>{validate(all);CallTally.validate(rows);require(rows.isNotEmpty()){"Add at least one counter first"};require(all.size<MAX){"Six saved boards already. Delete one first"};return validate(all+Saved(UUID.randomUUID().toString(),name(n),rows.map{it.copy(id=UUID.randomUUID().toString())}))}
 fun rename(all:List<Saved>,id:String,n:String):List<Saved>{validate(all);require(all.any{it.id==id});return validate(all.map{if(it.id==id)it.copy(name=name(n))else it})}
 fun remove(all:List<Saved>,id:String):List<Saved>{validate(all);require(all.any{it.id==id});return all.filterNot{it.id==id}}
 fun plan(rows:List<CallTally.Item>,library:String,id:String,token:String=UUID.randomUUID().toString()):Plan{CallTally.validate(rows);require(runCatching{UUID.fromString(token).toString()==token}.getOrDefault(false));val b=decode(library).single{it.id==id};val next=b.rows.mapIndexed{i,r->r.copy(id="$token-$i")};CallTally.validate(next);return Plan(rows.toList(),library,id,token,next)}
 fun apply(p:Plan,current:List<CallTally.Item>,library:String):List<CallTally.Item>{require(current==p.before&&library==p.library){"Session or saved library changed. Preview again"};require(p==plan(current,library,p.id,p.token)){"Preview changed. Preview again"};return p.after}
}
