package com.mandu.liquiddialer.data
import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/** Bounded normalized vector ink. Ordinary local data, never a credential store. */
object CallCanvas {
 const val MAX_NUMBERS=30
 const val MAX_STROKES=80
 const val MAX_POINTS=400
 const val MAX_TOTAL_POINTS=12000
 const val MAX_BYTES=2*1024*1024
 data class Point(val x:Int,val y:Int){init{require(x in 0..10000&&y in 0..10000)}}
 data class Stroke(val ink:Int,val width:Int,val points:List<Point>){fun validate(){require(ink in 0..3&&width in 1..3&&points.size in 1..MAX_POINTS)}}
 data class Snapshot(val raw:String,val strokes:List<Stroke>)
 fun key(number:String):String{CallLists.number(number);require(number.all{it in '0'..'9'||it in "+-(). "});return ContactControls.k(number)}
 fun validate(strokes:List<Stroke>){require(strokes.size<=MAX_STROKES&&strokes.sumOf{it.points.size}<=MAX_TOTAL_POINTS);strokes.forEach{it.validate()}}
 private fun int(o:JSONObject,k:String):Int{val n=o.get(k);require(n is Int);return n}
 fun decode(raw:String):LinkedHashMap<String,List<Stroke>>{
  if(raw.isEmpty())return linkedMapOf()
  require(raw.toByteArray(Charsets.UTF_8).size<=MAX_BYTES&&'\u0000' !in raw)
  val t=JSONTokener(raw);val o=t.nextValue()as?JSONObject?:error("Invalid canvas file");require(t.nextClean()=='\u0000'&&int(o,"version")==1)
  val a=o.getJSONArray("numbers");require(a.length()<=MAX_NUMBERS);val result=linkedMapOf<String,List<Stroke>>()
  repeat(a.length()){i->val n=a.getJSONObject(i);require(n.get("key")is String);val k=n.getString("key");require(k.matches(Regex("(\\+|local:)[0-9]{3,20}"))&&k !in result)
   val lines=n.getJSONArray("strokes");require(lines.length() in 1..MAX_STROKES)
   val strokes=(0 until lines.length()).map{j->val s=lines.getJSONObject(j);val p=s.getJSONArray("points");require(p.length() in 1..MAX_POINTS);Stroke(int(s,"ink"),int(s,"width"),(0 until p.length()).map{z->val v=p.getJSONArray(z);require(v.length()==2&&v.get(0)is Int&&v.get(1)is Int);Point(v.getInt(0),v.getInt(1))})}
   validate(strokes);result[k]=strokes
  };return result
 }
 fun encode(all:Map<String,List<Stroke>>):String{val numbers=JSONArray();all.forEach{(k,ss)->validate(ss);val lines=JSONArray();ss.forEach{s->val ps=JSONArray();s.points.forEach{ps.put(JSONArray().put(it.x).put(it.y))};lines.put(JSONObject().put("ink",s.ink).put("width",s.width).put("points",ps))};numbers.put(JSONObject().put("key",k).put("strokes",lines))};return JSONObject().put("version",1).put("numbers",numbers).toString().also{decode(it)}}
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"call_canvas.json"))
 private fun raw(ctx:Context):String{val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return "";val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(4096);while(true){val n=input.read(b);if(n<0)break;require(out.size()+n<=MAX_BYTES);out.write(b,0,n)};out.toByteArray()};return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()}
 @Synchronized fun read(ctx:Context,number:String):Snapshot{val k=key(number);val r=raw(ctx);return Snapshot(r,decode(r)[k].orEmpty())}
 @Synchronized fun save(ctx:Context,number:String,expected:String,ss:List<Stroke>):Snapshot{val k=key(number);val current=raw(ctx);val all=decode(current);require(current==expected){"Saved canvas changed. Reload before saving."};validate(ss);if(ss.isEmpty())all.remove(k)else all[k]=ss;val next=encode(all);val f=file(ctx);val out=f.startWrite();try{out.write(next.toByteArray(Charsets.UTF_8));f.finishWrite(out)}catch(t:Throwable){f.failWrite(out);throw t};return Snapshot(next,ss.toList())}
 // Decimate old samples only when full, preserving both ends of long gestures.
 fun appendSample(points:MutableList<Point>,next:Point){
  if(points.lastOrNull()==next)return
  if(points.size>=MAX_POINTS){val reduced=points.filterIndexed{i,_->i%2==0}.toMutableList();if(reduced.last()!=points.last())reduced+=points.last();points.clear();points.addAll(reduced)}
  points+=next
 }
 class History(initial:List<Stroke>){var strokes:List<Stroke> = initial.toList();private set;private var future=emptyList<Stroke>()
  init{validate(initial)}
  val canUndo get()=strokes.isNotEmpty();val canRedo get()=future.isNotEmpty()
  fun add(stroke:Stroke):Boolean{val next=strokes+stroke;if(runCatching{validate(next)}.isFailure)return false;strokes=next;future=emptyList();return true}
  fun undo(){if(strokes.isNotEmpty()){future=future+strokes.last();strokes=strokes.dropLast(1)}}
  fun redo(){if(future.isNotEmpty()){strokes=strokes+future.last();future=future.dropLast(1)}}
  fun clear(){strokes=emptyList();future=emptyList()}
 }
}
