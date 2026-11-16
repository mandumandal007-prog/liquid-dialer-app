package com.mandu.liquiddialer.data
import java.util.Locale
/** A view over the current session, never an edit or saved search. */
object TallyFind{
 val modes=listOf("All","Nonzero","Zero")
 fun rows(rows:List<CallTally.Item>,query:String,mode:String):List<CallTally.Item>{
  CallTally.validate(rows);require(mode in modes)
  val terms=query.take(100).trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter{it.isNotBlank()}
  return rows.filter{r->(mode=="All"||mode=="Zero"&&r.value==0L||mode=="Nonzero"&&r.value!=0L)&&terms.all{r.label.lowercase(Locale.ROOT).contains(it)}}
 }
}
