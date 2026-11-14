package com.mandu.liquiddialer.data
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

/** Phone-only library; never captures the master switch, caller identities, photos or shared settings. */
object IncomingProfiles {
 const val MAX=6
 data class Profile(val id:String,val name:String,val choices:IncomingCard.Config)
 data class Plan(val before:String,val after:String,val library:String,val profileId:String)
 fun name(raw:String)=raw.trim().also{require(it.isNotBlank()&&it.length<=32&&it.none{c->c.isISOControl()}){"Use a name of 1-32 characters, without line breaks"}}
 fun validate(rows:List<Profile>):List<Profile>{require(rows.size<=MAX);require(rows.map{it.id}.distinct().size==rows.size);require(rows.map{it.name.lowercase(java.util.Locale.ROOT)}.distinct().size==rows.size){"That profile name already exists"};rows.forEach{require(it.id.matches(Regex("[A-Za-z0-9-]{1,60}")));require(name(it.name)==it.name);IncomingCard.validate(it.choices);require(!it.choices.enabled)};return rows}
 fun encode(rows:List<Profile>):String{validate(rows);return JSONObject().put("version",1).put("profiles",JSONArray().also{a->rows.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("choices",JSONObject(IncomingCard.encode(it.choices))))}}).toString()}
 fun decode(raw:String):List<Profile>{if(raw.isEmpty())return emptyList();require(raw.length<=24000){"Saved profile data is too large"};val t=JSONTokener(raw);val o=t.nextValue()as JSONObject;require(t.nextClean()=='\u0000'&&o.get("version")==1);val a=o.get("profiles")as JSONArray;require(a.length()<=MAX);return validate((0 until a.length()).map{i->val r=a.getJSONObject(i);val choices=r.get("choices")as JSONObject;val c=IncomingCard.decodeStrict(choices.toString());Profile(r.get("id")as String,r.get("name")as String,c)})}
 fun capture(rows:List<Profile>,n:String,c:IncomingCard.Config):List<Profile>{validate(rows);require(rows.size<MAX){"Six saved profiles already. Delete one first"};return validate(rows+Profile(UUID.randomUUID().toString(),name(n),IncomingCard.validate(c).copy(enabled=false)))}
 fun rename(rows:List<Profile>,id:String,n:String):List<Profile>{validate(rows);require(rows.any{it.id==id}){"Profile no longer exists"};return validate(rows.map{if(it.id==id)it.copy(name=name(n))else it})}
 fun remove(rows:List<Profile>,id:String):List<Profile>{validate(rows);require(rows.any{it.id==id}){"Profile no longer exists"};return rows.filterNot{it.id==id}}
 fun plan(current:String,library:String,id:String):Plan{val selected=decode(library).single{it.id==id};val before=if(current.isEmpty())IncomingCard.Config()else IncomingCard.decodeStrict(current);return Plan(current,IncomingCard.encode(selected.choices.copy(enabled=before.enabled)),library,id)}
 fun apply(plan:Plan,current:String,library:String):String{require(current==plan.before&&library==plan.library){"Choices or profiles changed. Open comparison again"};require(plan==plan(current,library,plan.profileId)){"Comparison changed. Open it again"};return plan.after}
 fun undo(plan:Plan,current:String):String{require(plan==plan(plan.before,plan.library,plan.profileId)){"Undo record changed"};require(current==plan.after){"Incoming choices changed after apply; Undo refused"};return plan.before}
 fun differences(a:IncomingCard.Config,b:IncomingCard.Config):List<String>{IncomingCard.validate(a);IncomingCard.validate(b);return buildList{
  fun row(label:String,x:Any,y:Any){if(x!=y)add("$label: $x → $y")}
  row("Card style",a.style,b.style);row("Background",a.background,b.background);row("Avatar shape",a.avatar,b.avatar);row("Avatar size %",a.avatarSize,b.avatarSize);row("Name size %",a.nameSize,b.nameSize);row("Extra number",a.number,b.number);row("Contact type",a.contactType,b.contactType);row("Last call",a.lastCall,b.lastCall);row("Message",a.message,b.message);row("WhatsApp",a.whatsapp,b.whatsapp);row("Answer on left",a.swap,b.swap);row("Swipe mode",a.swipe,b.swipe);row("Swipe travel dp",a.travel,b.travel);row("Confirm Cut",a.confirmCut,b.confirmCut)
 }}
}
