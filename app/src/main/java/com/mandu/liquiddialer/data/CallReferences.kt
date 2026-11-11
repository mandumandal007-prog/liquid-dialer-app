package com.mandu.liquiddialer.data
import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/** Short ordinary reference facts. Plain local data, never a credential vault. */
object CallReferences {
 const val MAX_CARDS=12
 const val MAX_NUMBERS=100
 data class Card(val id:String,val title:String,val value:String){fun validate(){
  require(id.matches(Regex("[A-Za-z0-9-]{1,60}")))
  require(title.isNotBlank()&&title.length<=40&&title.none{it.isISOControl()}){"Title 1-40 letters rakho"}
  require(value.isNotBlank()&&value.length<=500&&value.none{it.isISOControl()&&it!='\n'}){"Value 1-500 letters rakho; control characters nahi"}
 }}
 data class Snapshot(val raw:String,val cards:List<Card>)
 fun key(number:String):String{CallLists.number(number);return ContactControls.k(number).also{require(it.isNotEmpty())}}
 fun decode(raw:String):LinkedHashMap<String,List<Card>>{
  if(raw.isEmpty())return linkedMapOf()
  require(raw.toByteArray(Charsets.UTF_8).size<=1024*1024&&'\u0000' !in raw)
  val t=JSONTokener(raw);val o=t.nextValue() as? JSONObject?:error("Invalid reference file")
  require(t.nextClean()=='\u0000'&&o.get("version") is Int&&o.getInt("version")==1)
  val a=o.getJSONArray("numbers");require(a.length()<=MAX_NUMBERS);val out=linkedMapOf<String,List<Card>>()
  repeat(a.length()){i->val n=a.getJSONObject(i);require(n.get("key") is String);val k=n.getString("key")
   require(k.matches(Regex("(\\+|local:)[0-9]{3,20}"))&&k !in out)
   val rows=n.getJSONArray("cards");require(rows.length() in 1..MAX_CARDS)
   val cards=(0 until rows.length()).map{j->val r=rows.getJSONObject(j);listOf("id","title","value").forEach{require(r.get(it) is String)};Card(r.getString("id"),r.getString("title"),r.getString("value")).also{it.validate()}}
   require(cards.map{it.id}.distinct().size==cards.size);out[k]=cards
  };return out
 }
 fun encode(all:Map<String,List<Card>>):String{
  val a=JSONArray();all.forEach{(k,cards)->val rows=JSONArray();cards.forEach{it.validate();rows.put(JSONObject().put("id",it.id).put("title",it.title).put("value",it.value))};a.put(JSONObject().put("key",k).put("cards",rows))}
  return JSONObject().put("version",1).put("numbers",a).toString().also{decode(it)}
 }
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"call_references.json"))
 private fun raw(ctx:Context):String{
  val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return ""
  val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;require(out.size()+n<=1024*1024);out.write(b,0,n)};out.toByteArray()}
  return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
 }
 @Synchronized fun read(ctx:Context,number:String):Snapshot{val k=key(number);val r=raw(ctx);return Snapshot(r,decode(r)[k].orEmpty())}
 @Synchronized fun save(ctx:Context,number:String,expected:String,cards:List<Card>):Snapshot{
  val k=key(number);val current=raw(ctx);val all=decode(current);require(current==expected){"Saved cards changed; Reload karo"}
  if(cards.isEmpty())all.remove(k)else all[k]=cards
  val next=encode(all);val f=file(ctx);val stream=f.startWrite();try{stream.write(next.toByteArray(Charsets.UTF_8));f.finishWrite(stream)}catch(t:Throwable){f.failWrite(stream);throw t}
  return Snapshot(next,cards.toList())
 }
 fun search(cards:List<Card>,query:String)=cards.filter{it.title.contains(query.trim(),ignoreCase=true)}
}
