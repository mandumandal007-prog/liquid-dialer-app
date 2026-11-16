package com.mandu.liquiddialer.data
/** Original typed inputs only, never parsed from rounded/presented result strings. */
object DatesRecall{
 data class Input(val mode:CallDates.Mode,val first:String,val last:String,val days:String)
 data class Entry(val input:Input,val result:CallDates.Result)
 fun create(mode:CallDates.Mode,first:String,last:String,days:String):Entry{
  val result=CallDates.calculate(mode,first,last,days)
  return Entry(Input(mode,first,if(mode==CallDates.Mode.SHIFT)""else last,if(mode==CallDates.Mode.SHIFT)days else"7"),result)
 }
 fun input(e:Entry):Input{
  val v=e.input;require(create(v.mode,v.first,v.last,v.days)==e){"History input mismatch"};return v
 }
 class History{
  private val saved=mutableListOf<Entry>();val rows get()=saved.toList()
  fun add(e:Entry){input(e);saved.add(0,e);while(saved.size>10)saved.removeAt(saved.lastIndex)}
  fun clear(){saved.clear()}
 }
}
