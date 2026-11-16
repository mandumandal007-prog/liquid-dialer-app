package com.mandu.liquiddialer.data
import java.util.Locale
/** View ordering only. Kotlin stable sorting keeps board order for equal keys. */
object TallySort{
 val modes=listOf("Board order","Name A-Z","Quantity high-low","Quantity low-high")
 fun rows(rows:List<CallTally.Item>,mode:String):List<CallTally.Item>{
  CallTally.validate(rows);require(mode in modes)
  return when(mode){
   "Name A-Z"->rows.sortedBy{it.label.lowercase(Locale.ROOT)}
   "Quantity high-low"->rows.sortedByDescending{it.value}
   "Quantity low-high"->rows.sortedBy{it.value}
   else->rows.toList()
  }
 }
}
