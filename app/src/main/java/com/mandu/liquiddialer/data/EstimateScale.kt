package com.mandu.liquiddialer.data
import java.math.BigDecimal
import java.math.RoundingMode

/** Explicit arithmetic on selected draft quantities. No storage, stock or payment effects. */
object EstimateScale {
 data class Change(val id:String,val label:String,val before:String,val after:String,val rate:String)
 data class Preview(val source:CallEstimate.Draft,val selected:Set<String>,val numerator:String,val denominator:String,val changes:List<Change>,val result:CallEstimate.Draft)
 private fun ratio(raw:String):BigDecimal {
  require(Regex("[0-9]{1,7}").matches(raw)){"Use whole ratio numbers 1-1000000"}
  return BigDecimal(raw).also{require(it>=BigDecimal.ONE&&it<=BigDecimal("1000000")){"Use whole ratio numbers 1-1000000"}}
 }
 fun preview(base:CallEstimate.Draft,selected:Set<String>,numerator:String,denominator:String):Preview {
  require(base.lines.isNotEmpty()){"Add an estimate item first"};CallEstimate.totals(base)
  require(selected.isNotEmpty()&&selected.all{id->base.lines.any{it.id==id}}){"Select current estimate items"}
  val n=ratio(numerator);val d=ratio(denominator);require(n!=d){"Use a ratio other than 1:1"}
  val changes=mutableListOf<Change>()
  val lines=base.lines.map{r->if(r.id !in selected)r else {
   val q=try{(BigDecimal(r.quantity)*n).divide(d,3,RoundingMode.UNNECESSARY).stripTrailingZeros().toPlainString()}catch(_:ArithmeticException){throw IllegalArgumentException("${r.label}: result needs more than 3 decimals. No quantity was rounded or applied.")}
   val next=CallEstimate.line(r.label,q,r.rate,r.id);changes+=Change(r.id,r.label,r.quantity,next.quantity,r.rate);next
  }}
  val result=base.copy(lines=lines);CallEstimate.totals(result)
  require(result!=base){"Ratio leaves all quantities unchanged"}
  return Preview(base.copy(lines=base.lines.toList()),selected.toSet(),numerator,denominator,changes.toList(),result)
 }
 fun apply(current:CallEstimate.Draft,view:Preview):CallEstimate.Draft {
  require(current==view.source){"Calculator changed. Preview again."}
  val verified=preview(current,view.selected,view.numerator,view.denominator)
  require(verified==view){"Preview changed. Preview again."}
  return verified.result
 }
}
