package com.mandu.liquiddialer.data
import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.Locale
import java.util.UUID

/** Reusable labels and step only. Never serializes quantities, phone identities or history. */
object TallyTemplates {
 const val MAX_TEMPLATES=20
 const val MAX_BYTES=128*1024
 data class Template(val id:String,val name:String,val labels:List<String>,val step:Long)
 data class Snapshot(val raw:String,val templates:List<Template>)
 fun title(raw:String)=CallTally.label(raw)
 private fun norm(s:String)=s.lowercase(Locale.ROOT)
 fun validate(t:Template){require(t.id.matches(Regex("[A-Za-z0-9_-]{1,80}")));require(title(t.name)==t.name);require(t.labels.size in 1..CallTally.MAX_ROWS);CallTally.validate(t.labels.mapIndexed{i,s->CallTally.Item("row_$i",s,0)});require(t.step in 1..1_000_000)}
 fun decode(raw:String):List<Template>{
  if(raw.isEmpty())return emptyList()
  require(raw.toByteArray(Charsets.UTF_8).size<=MAX_BYTES&&'\u0000' !in raw)
  val tok=JSONTokener(raw);val root=tok.nextValue() as? JSONObject?:error("Invalid template file");require(tok.nextClean()=='\u0000');require(root.get("version") is Int&&root.getInt("version")==1)
  val a=root.getJSONArray("templates");require(a.length()<=MAX_TEMPLATES);val ids=hashSetOf<String>();val names=hashSetOf<String>()
  return (0 until a.length()).map{i->val o=a.getJSONObject(i);require(o.get("id") is String&&o.get("name") is String);val ls=o.getJSONArray("labels");val labels=(0 until ls.length()).map{j->require(ls.get(j) is String);ls.getString(j)};val n=o.get("step");require(n is Int||n is Long);Template(o.getString("id"),o.getString("name"),labels,(n as Number).toLong()).also{validate(it);require(ids.add(it.id)&&names.add(norm(it.name)))}}
 }
 fun encode(rows:List<Template>):String{val a=JSONArray();rows.forEach{t->validate(t);a.put(JSONObject().put("id",t.id).put("name",t.name).put("labels",JSONArray(t.labels)).put("step",t.step))};return JSONObject().put("version",1).put("templates",a).toString().also{require(decode(it)==rows)}}
 fun create(all:List<Template>,name:String,rows:List<CallTally.Item>,step:String,id:String=UUID.randomUUID().toString()):List<Template>{CallTally.validate(rows);require(rows.isNotEmpty()){"Pehle counter add karo"};require(all.size<MAX_TEMPLATES){"20 templates ki limit"};val next=Template(id,title(name),rows.map{it.label},CallTally.step(step));require(all.none{norm(it.name)==norm(next.name)}){"Yeh template naam pehle se hai"};return (all+next).also{encode(it)}}
 fun remove(all:List<Template>,id:String):List<Template>{require(all.any{it.id==id}){"Template nahi mila; reload karo"};return all.filterNot{it.id==id}.also{encode(it)}}
 fun board(t:Template):List<CallTally.Item>{validate(t);return t.labels.mapIndexed{i,s->CallTally.Item("loaded_$i",s,0)}.also{CallTally.validate(it)}}
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"tally_templates.json"))
 private fun raw(ctx:Context):String{val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return "";val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(4096);while(true){val n=input.read(b);if(n<0)break;require(out.size()+n<=MAX_BYTES){"Template file bahut badi hai"};out.write(b,0,n)};out.toByteArray()};return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()}
 @Synchronized fun read(ctx:Context):Snapshot{val r=raw(ctx);return Snapshot(r,decode(r))}
 @Synchronized fun write(ctx:Context,expected:String,rows:List<Template>):Snapshot{val current=raw(ctx);decode(current);require(current==expected){"Templates changed. Reload before saving."};val next=encode(rows);val f=file(ctx);val out=f.startWrite();try{out.write(next.toByteArray(Charsets.UTF_8));f.finishWrite(out)}catch(t:Throwable){f.failWrite(out);throw t};return Snapshot(next,rows.map{it.copy(labels=it.labels.toList())})}
}
