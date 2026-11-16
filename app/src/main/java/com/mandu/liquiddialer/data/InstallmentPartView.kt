package com.mandu.liquiddialer.data
/** Display selection only. Never changes preview values or source calculator items. */
object InstallmentPartView{
 fun index(raw:String,total:Int):Int{
  require(total in 1..36){"Preview must have 1-36 parts"}
  require(Regex("[0-9]{1,2}").matches(raw)){"Enter a whole part number 1-$total"}
  return raw.toInt().also{require(it in 1..total){"Part must be 1-$total"}}
 }
 fun rows(parts:List<InstallmentPreview.Part>,selected:Int?):List<InstallmentPreview.Part> = if(selected==null)parts.toList()else parts.filter{it.index==selected}
}
