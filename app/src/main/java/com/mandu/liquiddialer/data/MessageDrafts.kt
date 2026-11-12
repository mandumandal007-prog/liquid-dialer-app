package com.mandu.liquiddialer.data
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/** Ordinary reusable text only. Reading/selecting never sends a message or fills the clipboard. */
object MessageDrafts {
 const val MAX=20
 const val MAX_BODY=2000
 const val MAX_BYTES=128*1024
 data class Draft(val id:String,val title:String,val body:String){fun validate(){
  require(id.matches(Regex("[A-Za-z0-9-]{1,60}")))
  require(title.isNotBlank()&&title.length<=50&&title.none{it.isISOControl()}){"Title 1-50 letters rakho"}
  validateBody(body)
 }}
 data class Snapshot(val raw:String,val rows:List<Draft>)
 fun validateBody(body:String){require(body.isNotBlank()&&body.length<=MAX_BODY&&body.none{it.isISOControl()&&it!='\n'}){"Text 1-2000 letters rakho; control characters nahi"}}
 fun target(number:String)=CallLists.number(number).also{require(it.all{c->c in '0'..'9'||c in "+-(). "}&&ContactControls.k(it).isNotEmpty())}
 fun decode(raw:String):List<Draft>{
  if(raw.isEmpty())return emptyList()
  require(raw.toByteArray(Charsets.UTF_8).size<=MAX_BYTES&&'\u0000' !in raw)
  val t=JSONTokener(raw);val o=t.nextValue()as?JSONObject?:error("Invalid drafts")
  require(t.nextClean()=='\u0000'&&o.get("version")is Int&&o.getInt("version")==1)
  val a=o.getJSONArray("drafts");require(a.length()<=MAX)
  val rows=(0 until a.length()).map{i->val r=a.getJSONObject(i);listOf("id","title","body").forEach{require(r.get(it)is String)};Draft(r.getString("id"),r.getString("title"),r.getString("body")).also{it.validate()}}
  require(rows.map{it.id}.distinct().size==rows.size);return rows
 }
 fun encode(rows:List<Draft>):String{val a=JSONArray();rows.forEach{it.validate();a.put(JSONObject().put("id",it.id).put("title",it.title).put("body",it.body))};return JSONObject().put("version",1).put("drafts",a).toString().also{decode(it)}}
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"message_drafts.json"))
 private fun raw(ctx:Context):String{
  val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return ""
  val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(4096);while(true){val n=input.read(b);if(n<0)break;require(out.size()+n<=MAX_BYTES);out.write(b,0,n)};out.toByteArray()}
  return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
 }
 @Synchronized fun read(ctx:Context):Snapshot{val r=raw(ctx);return Snapshot(r,decode(r))}
 @Synchronized fun save(ctx:Context,expected:String,rows:List<Draft>):Snapshot{
  val old=raw(ctx);decode(old);require(old==expected){"Saved drafts changed. Reload before saving."};val next=encode(rows)
  val f=file(ctx);val out=f.startWrite();try{out.write(next.toByteArray(Charsets.UTF_8));f.finishWrite(out)}catch(t:Throwable){f.failWrite(out);throw t};return Snapshot(next,rows.toList())
 }
 fun matching(rows:List<Draft>,q:String)=rows.filter{it.title.contains(q.trim(),true)||it.body.contains(q.trim(),true)}
 fun sms(number:String,body:String):Intent{val n=target(number);validateBody(body);return Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",n,null)).putExtra("sms_body",body)}
 fun whatsapp(number:String,body:String):Intent{target(number);validateBody(body);val url=WhatsAppLink.url(number)?:error("WhatsApp ke liye country code ke saath valid number chahiye");return Intent(Intent.ACTION_VIEW,Uri.parse(url).buildUpon().appendQueryParameter("text",body).build())}
}
