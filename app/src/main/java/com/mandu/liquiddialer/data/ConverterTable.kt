package com.mandu.liquiddialer.data
/** Same-category projection through the existing decimal converter; no persisted history. */
object ConverterTable {
 data class Table(val input:String,val from:CallConverter.UnitDef,val rows:List<CallConverter.Result>)
 fun create(raw:String,fromId:String):Table {
  val from=CallConverter.unit(fromId)
  val rows=CallConverter.of(from.kind).map{CallConverter.convert(raw,from.id,it.id)}
  require(rows.isNotEmpty())
  return Table(rows.first().input,from,rows)
 }
}
