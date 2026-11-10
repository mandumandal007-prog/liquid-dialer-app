package com.mandu.liquiddialer.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Explicit manual arithmetic, not an invoice, payment, tax advice, or call automation. */
object CallEstimate {
 const val MAX_LINES=20
 const val MAX_NUMBERS=100
 data class Line(val id:String,val label:String,val quantity:String,val rate:String)
 data class Draft(val revision:Long=0,val lines:List<Line> = emptyList(),val discount:String="0",val tax:String="0")
 data class Totals(val lineAmounts:List<BigDecimal>,val subtotal:BigDecimal,val discount:BigDecimal,val taxable:BigDecimal,val tax:BigDecimal,val total:BigDecimal)
 private fun key(number:String):String {CallLists.number(number);return ContactControls.k(number).also{require(it.isNotEmpty())}}
 private fun amount(raw:String,places:Int,max:String,positive:Boolean=false):BigDecimal {
  require(raw.length<=24&&Regex("[0-9]{1,17}(\\.[0-9]{1,$places})?").matches(raw)){"Sirf digits aur decimal: $places decimal places tak"}
  return BigDecimal(raw).also{require(it<=BigDecimal(max)&&(!positive||it>BigDecimal.ZERO)){"Quantity/rate ki limit dekho; quantity 0 se badi rakho"}}
 }
 fun line(label:String,quantity:String,rate:String,id:String=UUID.randomUUID().toString()):Line {
  val name=label.trim();require(name.length in 1..80&&'\u0000' !in name){"Item ka naam 1-80 letters rakho"};require(id.isNotBlank()&&id.length<=80)
  amount(quantity,3,"1000000",true);amount(rate,2,"1000000000")
  return Line(id,name,quantity,rate)
 }
 fun totals(draft:Draft):Totals {
  require(draft.lines.size<=MAX_LINES);val ids=hashSetOf<String>()
  val values=draft.lines.map{r->line(r.label,r.quantity,r.rate,r.id);require(ids.add(r.id));(BigDecimal(r.quantity)*BigDecimal(r.rate)).setScale(2,RoundingMode.HALF_UP)}
  val subtotal=values.fold(BigDecimal("0.00")){a,b->a+b};val discount=amount(draft.discount,2,"20000000000000000").setScale(2)
  require(discount<=subtotal){"Discount subtotal se zyada nahi ho sakta"};val base=subtotal-discount;val percent=amount(draft.tax,2,"100")
  val tax=(base*percent).divide(BigDecimal(100),2,RoundingMode.HALF_UP)
  return Totals(values,subtotal,discount,base,tax,base+tax)
 }
 fun money(value:BigDecimal)=value.setScale(2,RoundingMode.HALF_UP).toPlainString()
 private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"call_estimates.json"))
 private data class Store(val sequence:Long=0,val drafts:LinkedHashMap<String,Draft> = linkedMapOf())
 private fun readAll(ctx:Context):Store{
  val f=file(ctx);if(!f.baseFile.exists()&&!File(f.baseFile.path+".bak").exists())return Store()
  val bytes=f.openRead().use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;require(out.size()+n<=1024*1024);out.write(b,0,n)};out.toByteArray()}
  val raw=Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();require('\u0000' !in raw);val tokener=JSONTokener(raw);val root=tokener.nextValue() as? JSONObject?:error("Invalid estimate file");require(tokener.nextClean()=='\u0000');require(root.get("version") is Int&&root.getInt("version")==1)
  val seq=root.getLong("sequence");require(seq>=0&&root.get("sequence") is Number&&root.get("sequence").toString()==seq.toString());val a=root.getJSONArray("estimates");require(a.length()<=MAX_NUMBERS);val all=linkedMapOf<String,Draft>()
  repeat(a.length()){i->val o=a.getJSONObject(i);require(o.get("key") is String);val k=o.getString("key");require(k !in all&&k.length<=50&&(k.startsWith("+")||k.startsWith("local:"))&&k.removePrefix("local:").removePrefix("+").let{it.length in 3..20&&it.all{c->c.isDigit()}})
   require(o.get("revision") is Number);val revision=o.getLong("revision");require(revision>0&&revision<=seq&&o.get("revision").toString()==revision.toString());require(o.get("discount") is String&&o.get("tax") is String)
   val rows=o.getJSONArray("lines");require(rows.length() in 1..MAX_LINES);val lines=(0 until rows.length()).map{j->val r=rows.getJSONObject(j);listOf("id","label","quantity","rate").forEach{require(r.get(it) is String)};line(r.getString("label"),r.getString("quantity"),r.getString("rate"),r.getString("id"))}
   val d=Draft(revision,lines,o.getString("discount"),o.getString("tax"));totals(d);all[k]=d
  };return Store(seq,all)
 }
 private fun write(ctx:Context,all:Map<String,Draft>,sequence:Long){
  val a=JSONArray();all.forEach{(k,d)->val rows=JSONArray();d.lines.forEach{rows.put(JSONObject().put("id",it.id).put("label",it.label).put("quantity",it.quantity).put("rate",it.rate))};a.put(JSONObject().put("key",k).put("revision",d.revision).put("discount",d.discount).put("tax",d.tax).put("lines",rows))}
  val bytes=JSONObject().put("version",1).put("sequence",sequence).put("estimates",a).toString().toByteArray(Charsets.UTF_8);require(bytes.size<=1024*1024)
  val f=file(ctx);val stream=f.startWrite();try{stream.write(bytes);f.finishWrite(stream)}catch(t:Throwable){f.failWrite(stream);throw t}
 }
 @Synchronized fun read(ctx:Context,number:String)=readAll(ctx).drafts[key(number)]?:Draft()
 @Synchronized fun save(ctx:Context,number:String,expected:Long,draft:Draft):Draft {
  val k=key(number);val store=readAll(ctx);val all=store.drafts;val old=all[k]?:Draft();require(old.revision==expected){"Draft doosri jagah badla hai; dobara load karo"};require(store.sequence<Long.MAX_VALUE)
  totals(draft);require(draft.lines.isNotEmpty()){ "Kam se kam ek item jodo" };require(k in all||all.size<MAX_NUMBERS){"100 numbers ki limit hai; koi purana estimate mitao"}
  val next=draft.copy(revision=store.sequence+1);all[k]=next;write(ctx,all,next.revision);return next
 }
 @Synchronized fun clear(ctx:Context,number:String,expected:Long):Draft {val k=key(number);val store=readAll(ctx);val all=store.drafts;require((all[k]?.revision?:0)==expected){"Draft badla hai; dobara load karo"};if(all.remove(k)!=null){require(store.sequence<Long.MAX_VALUE);write(ctx,all,store.sequence+1)};return Draft()}
}
