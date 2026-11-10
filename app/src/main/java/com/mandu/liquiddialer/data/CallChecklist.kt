package com.mandu.liquiddialer.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.UUID

/** Local conversation topics, never call automation or a contacts-provider mutation. */
object CallChecklist {
 const val MAX_ITEMS=30
 const val MAX_TEXT=250
 private const val MAX_NUMBERS=100
 private const val MAX_BYTES=1024*1024
 data class Item(val id:String,val text:String,val done:Boolean=false)
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"call_checklists.json"))
 private fun key(number:String):String {CallLists.number(number);return ContactControls.k(number).also{require(it.isNotEmpty())}}
 private fun text(value:String)=value.trim().also{require(it.isNotEmpty()&&it.length<=MAX_TEXT){"Topic 1-250 letters ka rakho"}}
 private fun readAll(ctx:Context):LinkedHashMap<String,List<Item>> {
  val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return linkedMapOf()
  val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;require(out.size()+n<=MAX_BYTES){"Checklist file bahut badi hai"};out.write(b,0,n)};out.toByteArray()}
  val raw=bytes.toString(Charsets.UTF_8);require('\u0000' !in raw);val tokener=JSONTokener(raw);val root=tokener.nextValue() as? JSONObject ?: error("Checklist format sahi nahi hai");require(tokener.nextClean()=='\u0000')
  require(root.get("version") is Int&&root.getInt("version")==1)
  val a=root.getJSONArray("checklists");require(a.length()<=MAX_NUMBERS)
  val result=linkedMapOf<String,List<Item>>()
  repeat(a.length()){i->val o=a.getJSONObject(i);require(o.get("key") is String);val k=o.getString("key");require(k.isNotEmpty()&&k.length<=50&&(k.startsWith("+")||k.startsWith("local:"))&&k.removePrefix("local:").removePrefix("+").let{digits->digits.length in 3..20&&digits.all{it.isDigit()}}&&k !in result)
   val items=o.getJSONArray("items");require(items.length() in 1..MAX_ITEMS);val ids=hashSetOf<String>()
   result[k]=(0 until items.length()).map{j->val x=items.getJSONObject(j);require(x.get("id") is String&&x.get("text") is String&&x.get("done") is Boolean);val id=x.getString("id");require(id.isNotBlank()&&ids.add(id));Item(id,text(x.getString("text")),x.getBoolean("done"))}
  };return result
 }
 private fun write(ctx:Context,all:Map<String,List<Item>>) {
  val a=JSONArray();all.forEach{(k,items)->val rows=JSONArray();items.forEach{rows.put(JSONObject().put("id",it.id).put("text",it.text).put("done",it.done))};a.put(JSONObject().put("key",k).put("items",rows))}
  val bytes=JSONObject().put("version",1).put("checklists",a).toString().toByteArray(Charsets.UTF_8);require(bytes.size<=MAX_BYTES){"Checklist storage ki limit hai"}
  val f=file(ctx);val stream=f.startWrite();try{stream.write(bytes);f.finishWrite(stream)}catch(t:Throwable){f.failWrite(stream);throw t}
 }
 @Synchronized fun read(ctx:Context,number:String):List<Item> = readAll(ctx)[key(number)].orEmpty()
 @Synchronized fun add(ctx:Context,number:String,value:String):List<Item> {
  val k=key(number);val all=readAll(ctx);val old=all[k].orEmpty();require(old.size<MAX_ITEMS){"Ek number par 30 topics tak rakho"};require(k in all||all.size<MAX_NUMBERS){"100 numbers ki limit hai; koi purani checklist mitao"}
  val next=old+Item(UUID.randomUUID().toString(),text(value));all[k]=next;write(ctx,all);return next
 }
 /** Re-read under the same lock: all-or-nothing append, preserving IDs, order and ticks. */
 @Synchronized fun appendTopics(ctx:Context,number:String,values:List<String>):List<Item>{
  val k=key(number);val all=readAll(ctx);val old=all[k].orEmpty();val missing=ChecklistTemplates.missing(old,values)
  require(old.size+missing.size<=MAX_ITEMS){"Jagah kam hai: ${missing.size} naye topics, ${MAX_ITEMS-old.size} jagah. Koi topic nahi juda."}
  if(missing.isEmpty())return old
  require(k in all||all.size<MAX_NUMBERS){"100 numbers ki limit hai; koi purani checklist mitao"}
  val next=old+missing.map{Item(UUID.randomUUID().toString(),it)};all[k]=next;write(ctx,all);return next
 }
 private fun change(ctx:Context,number:String,id:String,f:(Item)->Item?):List<Item> {
  val k=key(number);val all=readAll(ctx);val old=all[k].orEmpty();require(old.any{it.id==id}){"Topic ab nahi mila; dobara load karo"};val next=old.mapNotNull{if(it.id==id)f(it)else it};if(next.isEmpty())all.remove(k)else all[k]=next;write(ctx,all);return next
 }
 @Synchronized fun edit(ctx:Context,number:String,id:String,value:String)=change(ctx,number,id){it.copy(text=text(value))}
 @Synchronized fun done(ctx:Context,number:String,id:String,value:Boolean)=change(ctx,number,id){it.copy(done=value)}
 @Synchronized fun remove(ctx:Context,number:String,id:String)=change(ctx,number,id){null}
 @Synchronized fun reset(ctx:Context,number:String):List<Item> {val k=key(number);val all=readAll(ctx);val next=all[k].orEmpty().map{it.copy(done=false)};if(next.isNotEmpty()){all[k]=next;write(ctx,all)};return next}
 @Synchronized fun clear(ctx:Context,number:String):List<Item> {val k=key(number);val all=readAll(ctx);if(all.remove(k)!=null)write(ctx,all);return emptyList()}
}
