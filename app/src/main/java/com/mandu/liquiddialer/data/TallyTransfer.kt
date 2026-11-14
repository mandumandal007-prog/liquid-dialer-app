package com.mandu.liquiddialer.data

/** Session-only allocation. The user, never this tool, decides whether units match. */
object TallyTransfer {
 data class Preview(val before:List<CallTally.Item>,val after:List<CallTally.Item>,val source:String,val allocations:Map<String,Long>){val moved get()=allocations.values.sum()}
 fun preview(rows:List<CallTally.Item>,source:String,raw:Map<String,String>):Preview{
  CallTally.validate(rows)
  val from=rows.singleOrNull{it.id==source}?:error("Choose a source counter first")
  require(raw.keys.all{id->id!=source&&rows.any{it.id==id}}){"Choose other existing counters as destinations"}
  val allocations=linkedMapOf<String,Long>()
  rows.filter{it.id!=source}.forEach{row->val text=raw[row.id].orEmpty();if(text.isNotEmpty()){
   val n=CallTally.whole(text);require(n>0){"${row.label}: enter a positive whole quantity, or leave blank"};allocations[row.id]=n
  }}
  require(allocations.isNotEmpty()){"Enter at least one destination quantity"}
  val moved=allocations.values.sum()
  require(moved<=from.value){"Source has ${from.value}; allocations total $moved. Whole transfer refused."}
  val after=rows.map{row->when{row.id==source->row.copy(value=row.value-moved);row.id in allocations->{val n=row.value+allocations.getValue(row.id);require(n<=CallTally.MAX_VALUE){"${row.label}: result above 1000000000. Whole transfer refused."};row.copy(value=n)};else->row}}
  CallTally.validate(after);require(CallTally.total(rows)==CallTally.total(after))
  return Preview(rows.toList(),after,source,allocations.toMap())
 }
 fun apply(rows:List<CallTally.Item>,p:Preview):List<CallTally.Item>{
  require(rows==p.before){"Board changed. Preview the transfer again."}
  val checked=preview(rows,p.source,p.allocations.mapValues{it.value.toString()});require(checked==p){"Transfer preview changed. Preview again."};return checked.after
 }
}
