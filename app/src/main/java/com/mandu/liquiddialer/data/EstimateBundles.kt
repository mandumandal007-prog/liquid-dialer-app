package com.mandu.liquiddialer.data
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

/** Explicit phone-only reusable rows, never a customer, price feed, invoice or saved estimate. */
object EstimateBundles {
 const val MAX=6
 data class Bundle(val id:String,val name:String,val lines:List<CallEstimate.Line>)
 data class Plan(val before:CallEstimate.Draft,val library:String,val id:String,val token:String,val after:CallEstimate.Draft)
 fun name(raw:String)=raw.trim().also{require(it.length in 1..32&&it.none{c->c.isISOControl()}){"Use a name of 1-32 characters, no line breaks"}}
 fun validate(rows:List<Bundle>):List<Bundle>{require(rows.size<=MAX);require(rows.map{it.id}.distinct().size==rows.size);require(rows.map{it.name.lowercase(java.util.Locale.ROOT)}.distinct().size==rows.size){"That bundle name already exists"};rows.forEach{r->require(r.id.matches(Regex("[A-Za-z0-9-]{1,60}")));require(name(r.name)==r.name);require(r.lines.size in 1..20);r.lines.forEach{require(it.label.none{c->c.isISOControl()})};CallEstimate.totals(CallEstimate.Draft(lines=r.lines))};return rows}
 fun encode(rows:List<Bundle>):String{validate(rows);return JSONObject().put("version",1).put("bundles",JSONArray().also{a->rows.forEach{r->a.put(JSONObject().put("id",r.id).put("name",r.name).put("lines",JSONArray().also{l->r.lines.forEach{l.put(JSONObject().put("id",it.id).put("label",it.label).put("quantity",it.quantity).put("rate",it.rate))}}))}}).toString()}
 fun decode(raw:String):List<Bundle>{if(raw.isEmpty())return emptyList();require(raw.length<=80000);val t=JSONTokener(raw);val o=t.nextValue()as JSONObject;require(t.nextClean()=='\u0000'&&o.get("version")is Int&&o.getInt("version")==1);val a=o.get("bundles")as JSONArray;require(a.length()<=MAX);return validate((0 until a.length()).map{i->val r=a.getJSONObject(i);val ls=r.get("lines")as JSONArray;require(ls.length() in 1..20);Bundle(r.get("id")as String,r.get("name")as String,(0 until ls.length()).map{j->val x=ls.getJSONObject(j);CallEstimate.line(x.get("label")as String,x.get("quantity")as String,x.get("rate")as String,x.get("id")as String)})})}
 fun capture(rows:List<Bundle>,n:String,base:CallEstimate.Draft,selected:Set<String>):List<Bundle>{validate(rows);CallEstimate.totals(base);require(rows.size<MAX){"Six bundles already. Delete one first"};require(selected.isNotEmpty()&&selected.all{id->base.lines.any{it.id==id}}){"Choose existing items"};return validate(rows+Bundle(UUID.randomUUID().toString(),name(n),base.lines.filter{it.id in selected}.map{it.copy(id=UUID.randomUUID().toString())}))}
 fun rename(rows:List<Bundle>,id:String,n:String):List<Bundle>{validate(rows);require(rows.any{it.id==id});return validate(rows.map{if(it.id==id)it.copy(name=name(n))else it})}
 fun remove(rows:List<Bundle>,id:String):List<Bundle>{validate(rows);require(rows.any{it.id==id});return rows.filterNot{it.id==id}}
 fun plan(base:CallEstimate.Draft,library:String,id:String,token:String=UUID.randomUUID().toString()):Plan{CallEstimate.totals(base);require(runCatching{UUID.fromString(token).toString()==token}.getOrDefault(false));val b=decode(library).single{it.id==id};require(base.lines.size+b.lines.size<=20){"Whole bundle refused: only ${20-base.lines.size} item spaces"};val next=base.copy(lines=base.lines+b.lines.mapIndexed{i,r->r.copy(id="$token-$i")});CallEstimate.totals(next);return Plan(base.copy(lines=base.lines.toList()),library,id,token,next)}
 fun apply(view:Plan,current:CallEstimate.Draft,library:String):CallEstimate.Draft{require(current==view.before&&library==view.library){"Calculator or library changed. Preview again"};require(view==plan(current,library,view.id,view.token)){"Preview changed. Preview again"};return view.after}
}
