package com.mandu.liquiddialer.data
/** Restore only validated original input, never the displayed rounded conversion result. */
object ConverterRecall {
 data class Input(val value:String,val kind:CallConverter.Kind,val from:String,val to:String)
 fun input(row:CallConverter.Result):Input {
  val checked=CallConverter.convert(row.input,row.from.id,row.to.id)
  require(row.from==checked.from&&row.to==checked.to){"History unit definition changed"}
  return Input(checked.input,checked.from.kind,checked.from.id,checked.to.id)
 }
}
