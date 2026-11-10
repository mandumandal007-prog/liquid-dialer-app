package com.mandu.liquiddialer.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.UUID
import java.util.Locale

/** Reusable text only. Never retains a phone number or discussed ticks. */
object ChecklistTemplates {
 const val MAX_TEMPLATES=20
 data class Template(val id:String,val title:String,val topics:List<String>,val builtIn:Boolean=false)
 val starters=listOf(
  Template("starter_order","Order ki baat",listOf("Kaunsa item aur kitni quantity?","Rate aur total confirm karo","Delivery ya pickup ka din aur samay","Agla step kiski taraf se?"),true),
  Template("starter_followup","Follow-up",listOf("Pichhli baat ka status?","Kya abhi baaki hai?","Agla step aur zimmedari","Dobara kab baat karein?"),true),
  Template("starter_service","Service / appointment",listOf("Problem ya kaam ki detail","Estimate aur extra charges poochho","Date, time aur jagah confirm karo","Kya taiyari ya documents chahiye?"),true)
 )
 fun normalized(s:String)=s.trim().replace(Regex("\\s+")," ").lowercase(Locale.ROOT)
 fun topics(values:List<String>):List<String>{require(values.size in 1..CallChecklist.MAX_ITEMS){"1-30 topics rakho"};val cleaned=values.map{it.trim().also{t->require(t.isNotEmpty()&&t.length<=CallChecklist.MAX_TEXT){"Har topic 1-250 letters ka rakho"}}};require(cleaned.map{normalized(it)}.distinct().size==cleaned.size){"Template mein duplicate topic hatao"};return cleaned}
 fun lines(s:String)=topics(s.trim().split(Regex("\\r?\\n[ \\t]*\\r?\\n+")).filter{it.isNotBlank()})
 private fun title(s:String)=s.trim().also{require(it.length in 1..60){"Naam 1-60 letters ka rakho"}}
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"checklist_templates.json"))
 @Synchronized fun read(ctx:Context):List<Template>{
  val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return emptyList()
  val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=512*1024){"Template file bahut badi hai"};out.write(buffer,0,n)};out.toByteArray()}
  val raw=bytes.toString(Charsets.UTF_8);require('\u0000' !in raw);val tok=JSONTokener(raw);val root=tok.nextValue() as? JSONObject ?: error("Template format sahi nahi hai");require(tok.nextClean()=='\u0000');require(root.get("version") is Int&&root.getInt("version")==1)
  val a=root.getJSONArray("templates");require(a.length()<=MAX_TEMPLATES);val ids=hashSetOf<String>();val names=hashSetOf<String>()
  return (0 until a.length()).map{i->val o=a.getJSONObject(i);require(o.get("id") is String&&o.get("title") is String);val id=o.getString("id");require(id.isNotBlank()&&!id.startsWith("starter_")&&ids.add(id));val name=title(o.getString("title"));require(names.add(normalized(name)));val ts=o.getJSONArray("topics");val rows=(0 until ts.length()).map{j->require(ts.get(j) is String);ts.getString(j)};Template(id,name,topics(rows))}
 }
 private fun write(ctx:Context,rows:List<Template>){val a=JSONArray();rows.forEach{a.put(JSONObject().put("id",it.id).put("title",it.title).put("topics",JSONArray(it.topics)))};val bytes=JSONObject().put("version",1).put("templates",a).toString().toByteArray(Charsets.UTF_8);require(bytes.size<=512*1024);val f=file(ctx);val stream=f.startWrite();try{stream.write(bytes);f.finishWrite(stream)}catch(t:Throwable){f.failWrite(stream);throw t}}
 @Synchronized fun save(ctx:Context,id:String?,name:String,values:List<String>):List<Template>{val all=read(ctx);val t=title(name);val ts=topics(values);require(id==null||all.any{it.id==id}){"Template ab nahi mila; dobara load karo"};require(id!=null||all.size<MAX_TEMPLATES){"20 templates ki limit hai"};require(all.none{it.id!=id&&normalized(it.title)==normalized(t)}){"Yeh naam pehle se hai"};val next=Template(id?:UUID.randomUUID().toString(),t,ts);val rows=if(id==null)all+next else all.map{if(it.id==id)next else it};write(ctx,rows);return rows}
 @Synchronized fun remove(ctx:Context,id:String):List<Template>{val all=read(ctx);require(all.any{it.id==id}){"Template ab nahi mila"};val next=all.filterNot{it.id==id};write(ctx,next);return next}
 fun missing(existing:List<CallChecklist.Item>,values:List<String>):List<String>{val seen=existing.map{normalized(it.text)}.toMutableSet();return topics(values).filter{seen.add(normalized(it))}}
}
