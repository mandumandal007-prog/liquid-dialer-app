package com.mandu.liquiddialer.data
import java.util.UUID

/** Manual pipe-delimited text only. No clipboard, files, contacts or saved estimate access. */
object EstimateLines {
 const val MAX_INPUT=8000
 const val MAX_INPUT_LINES=40
 data class Row(val sourceLine:Int,val item:CallEstimate.Line)
 data class Preview(val source:CallEstimate.Draft,val raw:String,val token:String,val rows:List<Row>,val result:CallEstimate.Draft,val blankLines:Int)
 fun preview(base:CallEstimate.Draft,raw:String):Preview=build(base,raw,UUID.randomUUID().toString())
 private fun build(base:CallEstimate.Draft,raw:String,token:String):Preview {
  CallEstimate.totals(base)
  require(raw.length<=MAX_INPUT){"Use at most 8000 characters. Whole batch refused."}
  require(raw.none{it.isISOControl()&&it!='\n'&&it!='\r'&&it!='\t'}){"Control characters refused. Nothing was appended."}
  require(runCatching{UUID.fromString(token).toString()==token}.getOrDefault(false)){"Invalid preview. Preview again."}
  val lines=raw.replace("\r\n","\n").replace('\r','\n').split('\n')
  require(lines.size<=MAX_INPUT_LINES){"Use at most 40 physical lines, including blanks."}
  var blanks=0
  val rows=lines.mapIndexedNotNull{i,s->if(s.isBlank()){blanks++;null}else{
   val parts=s.split('|');require(parts.size==3){"Line ${i+1}: use Name | Quantity | Rate, exactly 3 fields. Whole batch refused."}
   val fields=parts.map{it.trim()};require(fields[0].none{it.isISOControl()}){"Line ${i+1}: item name contains a control character."}
   val item=try{CallEstimate.line(fields[0],fields[1],fields[2],"$token-$i")}catch(_:IllegalArgumentException){throw IllegalArgumentException("Line ${i+1}: name 1-80 characters, quantity >0 to 1000000 (3 decimals), rate 0-1000000000 (2 decimals). Use a dot, not commas or symbols. Whole batch refused.")}
   Row(i+1,item)
  }}
  require(rows.isNotEmpty()){"Write at least one item line."}
  require(base.lines.size+rows.size<=CallEstimate.MAX_LINES){"${rows.size} new items, only ${CallEstimate.MAX_LINES-base.lines.size} spaces. Whole batch refused; nothing was truncated."}
  val next=base.copy(lines=base.lines+rows.map{it.item});CallEstimate.totals(next)
  return Preview(base.copy(lines=base.lines.toList()),raw,token,rows.toList(),next,blanks)
 }
 fun apply(current:CallEstimate.Draft,view:Preview):CallEstimate.Draft {
  require(current==view.source){"Calculator changed. Preview again."}
  val verified=build(current,view.raw,view.token);require(verified==view){"Preview changed. Preview again."}
  return verified.result
 }
}
